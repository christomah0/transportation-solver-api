package com.example.transportation_solver_api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for the transportation problem response.
 * This class defines the structure of the JSON output from the API.
 */
@Setter
@Getter
public class TransportationResponse {
    // Getters and Setters
    private int[][] finalAllocations;
    private double optimalCost;
    private double[] uValues; // Row potentials
    private double[] vValues; // Column potentials
    private String message; // General message (e.g., success, error, warnings)

    // Default constructor for JSON serialization
    public TransportationResponse() {
    }

    public TransportationResponse(int[][] finalAllocations, double optimalCost, double[] uValues, double[] vValues, String message) {
        this.finalAllocations = finalAllocations;
        this.optimalCost = optimalCost;
        this.uValues = uValues;
        this.vValues = vValues;
        this.message = message;
    }

}

