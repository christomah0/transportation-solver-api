package com.example.transportation_solver_api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for the transportation problem request.
 * This class defines the structure of the JSON input for the API.
 */
@Setter
@Getter
public class TransportationRequest {
    // Getters and Setters
    private int[][] costs;
    private int[] supply;
    private int[] demand;

    // Default constructor for JSON deserialization
    public TransportationRequest() {
    }

    public TransportationRequest(int[][] costs, int[] supply, int[] demand) {
        this.costs = costs;
        this.supply = supply;
        this.demand = demand;
    }

}

