package com.example.transportation_solver_api.service;

import com.example.transportation_solver_api.model.Cell;
import com.example.transportation_solver_api.model.TransportationResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Service class that encapsulates the core logic for solving the Transportation Problem
 * using the MODI (Modified Distribution) method.
 * This class is designed to be injected into a Spring Controller.
 */
@Service
public class TransportationProblemService {

    private int[][] costs;
    private int[] supply;
    private int[] demand;
    private int[][] allocations;
    private boolean[][] isBasic;
    private int numSources;
    private int numDestinations;

    private double[] u; // Row potentials
    private double[] v; // Column potentials

    // StringBuilder to capture log messages for the response
    private StringBuilder logMessages;

    /**
     * Solves the transportation problem given costs, supply, and demand.
     * This method orchestrates the finding of an IBFS and then applying the MODI method.
     *
     * @param costs The 2D array representing unit transportation costs.
     * @param supply The array representing supply at each source.
     * @param demand The array representing demand at each destination.
     * @return A TransportationResponse object containing the final allocations, optimal cost,
     * u/v values, and any relevant messages.
     */
    public TransportationResponse solveTransportationProblem(int[][] costs, int[] supply, int[] demand) {
        this.costs = costs;
        this.supply = supply;
        this.demand = demand;
        this.numSources = supply.length;
        this.numDestinations = demand.length;
        this.allocations = new int[numSources][numDestinations];
        this.isBasic = new boolean[numSources][numDestinations];
        this.u = new double[numSources];
        this.v = new double[numDestinations];
        this.logMessages = new StringBuilder();

        // Initialize all allocations to zero and mark all cells as non-basic initially
        for (int i = 0; i < numSources; i++) {
            for (int j = 0; j < numDestinations; j++) {
                allocations[i][j] = 0;
                isBasic[i][j] = false;
            }
        }

        // Check if the problem is balanced (total supply equals total demand)
        int totalSupply = IntStream.of(supply).sum();
        int totalDemand = IntStream.of(demand).sum();
        if (totalSupply != totalDemand) {
            logMessages.append("Warning: Transportation problem is unbalanced. Total Supply: ").append(totalSupply)
                    .append(", Total Demand: ").append(totalDemand).append(". This implementation assumes a balanced problem for MODI.\n");
            // In a production scenario, you would add a dummy source/destination here.
            // For this example, we proceed assuming it's handled externally or by design.
        }

        // 1. Find an Initial Basic Feasible Solution (IBFS)
        findInitialBasicFeasibleSolutionLeastCost();

        // 2. Apply MODI method to find the optimal solution
        applyMODIMethod();

        // 3. Prepare the response
        return new TransportationResponse(
                allocations,
                getTotalCost(),
                u,
                v,
                logMessages.toString()
        );
    }

    /**
     * Finds an Initial Basic Feasible Solution (IBFS) using the Least Cost Method.
     * This method prioritizes allocating units to cells with the lowest transportation costs.
     * It also includes a basic handling for degeneracy to ensure m+n-1 basic cells.
     */
    private void findInitialBasicFeasibleSolutionLeastCost() {
        logMessages.append("Finding Initial Basic Feasible Solution (Least Cost Method)...\n");

        // Create copies of supply and demand to track remaining quantities
        int[] currentSupply = Arrays.copyOf(supply, numSources);
        int[] currentDemand = Arrays.copyOf(demand, numDestinations);

        // Create a list of all cells (i, j, cost)
        List<Cell> allCells = new ArrayList<>();
        for (int i = 0; i < numSources; i++) {
            for (int j = 0; j < numDestinations; j++) {
                allCells.add(new Cell(i, j, costs[i][j]));
            }
        }

        // Sort cells by cost in ascending order
        allCells.sort(Comparator.comparingInt(c -> c.cost));

        // Iterate through sorted cells and make allocations
        for (Cell cell : allCells) {
            int i = cell.row;
            int j = cell.col;

            // If both supply and demand for the current cell are positive, make an allocation
            if (currentSupply[i] > 0 && currentDemand[j] > 0) {
                int quantity = Math.min(currentSupply[i], currentDemand[j]);
                allocations[i][j] = quantity;
                isBasic[i][j] = true; // Mark as a basic variable

                currentSupply[i] -= quantity;
                currentDemand[j] -= quantity;
            }
        }

        // Degeneracy Handling: Ensure there are exactly (numSources + numDestinations - 1) basic cells.
        // If fewer, add dummy basic cells with 0 allocation.
        int basicCount = 0;
        for (int r = 0; r < numSources; r++) {
            for (int c = 0; c < numDestinations; c++) {
                if (isBasic[r][c]) {
                    basicCount++;
                }
            }
        }

        if (basicCount < numSources + numDestinations - 1) {
            logMessages.append("Degeneracy detected in initial solution. Adding dummy basic cells with 0 allocation.\n");
            for (int r = 0; r < numSources; r++) {
                for (int c = 0; c < numDestinations; c++) {
                    if (!isBasic[r][c] && basicCount < numSources + numDestinations - 1) {
                        isBasic[r][c] = true; // Mark as basic
                        allocations[r][c] = 0; // Explicitly set allocation to 0
                        basicCount++;
                    }
                }
            }
        }
        logMessages.append("Initial Basic Feasible Solution found.\n");
        logMessages.append("Initial Total Cost: ").append(getTotalCost()).append("\n");
    }

    /**
     * Applies the MODI (Modified Distribution) method iteratively to find the optimal solution.
     */
    private void applyMODIMethod() {
        logMessages.append("\nStarting MODI Method iterations...\n");
        int iteration = 0;
        while (true) {
            iteration++;
            logMessages.append("\n--- Iteration ").append(iteration).append(" ---\n");

            // Step 1: Calculate u_i and v_j values for all basic cells
            calculateUV();

            // Step 2: Calculate improvement indices (C_ij - (u_i + v_j)) for non-basic cells
            int enteringRow = -1;
            int enteringCol = -1;
            double maxNegativeImprovement = 0; // Stores the most negative improvement index

            logMessages.append("Improvement Indices (C_ij - (u_i + v_j)):\n");
            for (int i = 0; i < numSources; i++) {
                for (int j = 0; j < numDestinations; j++) {
                    if (!isBasic[i][j]) { // Only calculate for non-basic cells
                        double improvementIndex = costs[i][j] - (u[i] + v[j]);
                        logMessages.append(String.format("  (%d,%d): %.2f", i, j, improvementIndex));
                        if (improvementIndex < maxNegativeImprovement) {
                            maxNegativeImprovement = improvementIndex;
                            enteringRow = i;
                            enteringCol = j;
                        }
                    } else {
                        logMessages.append(String.format("  (%d,%d): BASIC", i, j));
                    }
                }
                logMessages.append("\n");
            }

            // Step 3: Check for optimality
            if (enteringRow == -1) {
                logMessages.append("\nAll improvement indices are non-negative. Optimal solution found!\n");
                break; // Optimal solution reached
            }

            logMessages.append(String.format("\nEntering cell: (%d, %d) with improvement index %.2f\n", enteringRow, enteringCol, maxNegativeImprovement));

            // Step 4: Find the closed loop for the entering cell
            List<Cell> loop = findClosedLoop(enteringRow, enteringCol);
            if (loop == null) {
                logMessages.append("Error: Could not find a closed loop for entering cell (").append(enteringRow)
                        .append(", ").append(enteringCol).append("). This indicates an issue.\n");
                break; // Exit to prevent infinite loop
            }
            logMessages.append("Closed loop found (alternating + and - signs): ");
            for (Cell cell : loop) {
                logMessages.append("(").append(cell.row).append(",").append(cell.col).append(") ");
            }
            logMessages.append("\n");

            // Step 5: Determine the minimum allocation to shift along the loop
            int minAllocationInLoop = Integer.MAX_VALUE;
            for (int k = 1; k < loop.size(); k += 2) { // Cells at odd indices get '-' sign
                minAllocationInLoop = Math.min(minAllocationInLoop, allocations[loop.get(k).row][loop.get(k).col]);
            }
            logMessages.append("Minimum allocation to shift (theta): ").append(minAllocationInLoop).append("\n");

            // Step 6: Adjust allocations along the loop and identify the leaving cell
            int leavingRow = -1;
            int leavingCol = -1;
            for (int k = 0; k < loop.size(); k++) {
                Cell cell = loop.get(k);
                if (k % 2 == 0) { // Cells with '+' sign (add theta)
                    allocations[cell.row][cell.col] += minAllocationInLoop;
                } else { // Cells with '-' sign (subtract theta)
                    allocations[cell.row][cell.col] -= minAllocationInLoop;
                    if (allocations[cell.row][cell.col] == 0) {
                        if (leavingRow == -1) {
                            leavingRow = cell.row;
                            leavingCol = cell.col;
                        }
                    }
                }
            }

            // Step 7: Update basic variables (entering cell becomes basic, leaving cell becomes non-basic)
            isBasic[enteringRow][enteringCol] = true;
            if (leavingRow != -1) {
                isBasic[leavingRow][leavingCol] = false;
                logMessages.append(String.format("Leaving cell: (%d, %d)\n", leavingRow, leavingCol));
            } else {
                logMessages.append("Degeneracy: No specific cell identified as leaving (min allocation was 0 or multiple cells became 0).\n");
            }

            logMessages.append("Solution after Iteration ").append(iteration).append(":\n");
            logMessages.append("Current Allocations:\n");
            for (int i = 0; i < numSources; i++) {
                for (int j = 0; j < numDestinations; j++) {
                    logMessages.append(String.format("%5d ", allocations[i][j]));
                }
                logMessages.append("\n");
            }
            logMessages.append(String.format("Current Total Cost: %.2f\n", getTotalCost()));
        }
    }

    /**
     * Calculates the u_i (row potentials) and v_j (column potentials) for all basic cells.
     * This is done by setting u[0] to 0 and then solving C_ij = u_i + v_j for all basic cells.
     * Uses a queue-based approach to propagate values.
     */
    private void calculateUV() {
        Arrays.fill(u, Double.NaN);
        Arrays.fill(v, Double.NaN);

        u[0] = 0; // Arbitrarily set u[0] to 0

        Queue<int[]> queue = new LinkedList<>();
        for (int j = 0; j < numDestinations; j++) {
            if (isBasic[0][j]) {
                queue.offer(new int[]{0, j});
            }
        }

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int r = cell[0];
            int c = cell[1];

            if (!Double.isNaN(u[r]) && Double.isNaN(v[c])) {
                v[c] = costs[r][c] - u[r];
                for (int i = 0; i < numSources; i++) {
                    if (isBasic[i][c] && Double.isNaN(u[i])) {
                        queue.offer(new int[]{i, c});
                    }
                }
            } else if (!Double.isNaN(v[c]) && Double.isNaN(u[r])) {
                u[r] = costs[r][c] - v[c];
                for (int j = 0; j < numDestinations; j++) {
                    if (isBasic[r][j] && Double.isNaN(v[j])) {
                        queue.offer(new int[]{r, j});
                    }
                }
            }
        }

        logMessages.append("u values: ");
        for (double val : u) logMessages.append(String.format("%.2f ", val));
        logMessages.append("\n");
        logMessages.append("v values: ");
        for (double val : v) logMessages.append(String.format("%.2f ", val));
        logMessages.append("\n");
    }

    /**
     * Finds a closed loop for the given entering cell (startR, startC) using Depth-First Search (DFS).
     * The loop must consist of alternating horizontal and vertical segments, passing only through
     * basic cells (or the entering cell itself).
     *
     * @param startR The row of the entering cell.
     * @param startC The column of the entering cell.
     * @return A list of cells forming the closed loop, or null if no loop is found.
     */
    private List<Cell> findClosedLoop(int startR, int startC) {
        List<Cell> path = new ArrayList<>();
        boolean[][] visitedInPath = new boolean[numSources][numDestinations];

        path.add(new Cell(startR, startC, 0));
        visitedInPath[startR][startC] = true;

        if (dfsFindLoopRecursive(startR, startC, startR, startC, path, visitedInPath, -1, -1)) {
            path.removeLast(); // Remove the duplicate start cell
            return path;
        }
        return null;
    }

    /**
     * Recursive helper for finding a closed loop using DFS.
     */
    private boolean dfsFindLoopRecursive(int currentR, int currentC, int startR, int startC,
                                         List<Cell> path, boolean[][] visitedInPath,
                                         int prevR, int prevC) {

        boolean canMoveHorizontally = (prevR == -1 && prevC == -1) || (prevC == currentC);
        boolean canMoveVertically = (prevR == -1 && prevC == -1) || (prevR == currentR);

        // Try moving horizontally
        if (canMoveHorizontally) {
            for (int c = 0; c < numDestinations; c++) {
                if (c == currentC) continue;

                if ((isBasic[currentR][c] || (currentR == startR && c == startC)) && !visitedInPath[currentR][c]) {
                    if (currentR == startR && c == startC && path.size() >= 3) {
                        path.add(new Cell(startR, startC, 0));
                        return true;
                    }

                    path.add(new Cell(currentR, c, 0));
                    visitedInPath[currentR][c] = true;

                    if (dfsFindLoopRecursive(currentR, c, startR, startC, path, visitedInPath, currentR, currentC)) {
                        return true;
                    }

                    path.removeLast();
                    visitedInPath[currentR][c] = false;
                }
            }
        }

        // Try moving vertically
        if (canMoveVertically) {
            for (int r = 0; r < numSources; r++) {
                if (r == currentR) continue;

                if ((isBasic[r][currentC] || (r == startR && currentC == startC)) && !visitedInPath[r][currentC]) {
                    if (r == startR && currentC == startC && path.size() >= 3) {
                        path.add(new Cell(startR, startC, 0));
                        return true;
                    }

                    path.add(new Cell(r, currentC, 0));
                    visitedInPath[r][currentC] = true;

                    if (dfsFindLoopRecursive(r, currentC, startR, startC, path, visitedInPath, currentR, currentC)) {
                        return true;
                    }

                    path.removeLast();
                    visitedInPath[r][currentC] = false;
                }
            }
        }
        return false;
    }

    /**
     * Calculates the total transportation cost based on the current allocations and costs.
     * @return The total cost.
     */
    private double getTotalCost() {
        double totalCost = 0;
        for (int i = 0; i < numSources; i++) {
            for (int j = 0; j < numDestinations; j++) {
                totalCost += allocations[i][j] * costs[i][j];
            }
        }
        return totalCost;
    }
}

