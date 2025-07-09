package com.example.transportation_solver_api.controller;

import com.example.transportation_solver_api.model.TransportationRequest;
import com.example.transportation_solver_api.model.TransportationResponse;
import com.example.transportation_solver_api.service.TransportationProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for the Transportation Problem Solver API.
 * Exposes an endpoint to receive transportation problem data and return the optimal solution.
 */
@RestController
@RequestMapping("/api")
public class TransportationProblemController {

    private final TransportationProblemService transportationProblemService;

    /**
     * Constructor for dependency injection of TransportationProblemService.
     * @param transportationProblemService The service that contains the core transportation problem logic.
     */
    public TransportationProblemController(TransportationProblemService transportationProblemService) {
        this.transportationProblemService = transportationProblemService;
    }

    /**
     * Solves the transportation problem.
     *
     * @param request The TransportationRequest containing costs, supply, and demand.
     * @return A ResponseEntity containing the TransportationResponse with the optimal allocations and cost.
     */
    @PostMapping("/solve")
    public ResponseEntity<TransportationResponse> solveTransportationProblem(@RequestBody TransportationRequest request) {
        // Basic validation
        if (request.getCosts() == null || request.getSupply() == null || request.getDemand() == null) {
            return ResponseEntity.badRequest().body(new TransportationResponse(null, 0, null, null, "Error: Missing input data (costs, supply, or demand)."));
        }
        if (request.getCosts().length == 0 || request.getCosts()[0].length == 0) {
            return ResponseEntity.badRequest().body(new TransportationResponse(null, 0, null, null, "Error: Costs matrix cannot be empty."));
        }

        try {
            TransportationResponse response = transportationProblemService.solveTransportationProblem(
                    request.getCosts(),
                    request.getSupply(),
                    request.getDemand()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Catch any unexpected errors during computation
            return ResponseEntity.internalServerError().body(new TransportationResponse(null, 0, null, null, "An internal error occurred: " + e.getMessage()));
        }
    }
}

