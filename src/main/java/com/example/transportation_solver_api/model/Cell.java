package com.example.transportation_solver_api.model;

/**
 * Helper class to represent a cell in the transportation table.
 * Used primarily for sorting cells by cost in the Least Cost Method
 * and for representing coordinates in the MODI loop finding.
 */
public class Cell {
    public int row;
    public int col;
    public int cost; // The unit cost of this cell (used during initial solution)

    public Cell(int row, int col, int cost) {
        this.row = row;
        this.col = col;
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
