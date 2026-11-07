package com.yourdomain.model;

/**
 * Immutable data model for a row in the trips table.
 */
public record Trip(int tripId, int boatId, int captainId, String destination,
                   String departureDate, String returnDate, double fuelCost, String status) {

    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
}