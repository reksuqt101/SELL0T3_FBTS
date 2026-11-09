package com.yourdomain.model;

/**
 * Immutable data model for a row in the boats table.
 */
public record Boat(int boatId, String boatName, String registrationNo, String boatType,
                   double capacity, String status) {

    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_ON_TRIP = "On Trip";
}