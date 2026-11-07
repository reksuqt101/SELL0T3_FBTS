package com.yourdomain.model;

/**
 * Immutable data model for a row in the boats table.
 */
public record Boat(int boatId, String boatName, String registrationNo, String boatType,
                   double capacity, String status) {

    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_ON_TRIP = "On Trip";

    /** Creates a new, not-yet-saved boat (id 0) that is Available. */
    public static Boat newBoat(String boatName, String registrationNo, String boatType, double capacity) {
        return new Boat(0, boatName, registrationNo, boatType, capacity, STATUS_AVAILABLE);
    }
}