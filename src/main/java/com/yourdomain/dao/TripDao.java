package com.yourdomain.dao;

import com.yourdomain.model.ActivityLog;
import com.yourdomain.model.Boat;
import com.yourdomain.model.Trip;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the trips table. Every query uses a PreparedStatement.
 * Trip actions are written to the audit log after the database change is saved.
 */
public class TripDao {

    private static final String COLUMNS =
            "trip_id, boat_id, captain_id, destination, departure_date, return_date, fuel_cost, status";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    /**
     * Records a new trip: marks the boat as On Trip and saves the trip as ONGOING.
     * Returns the new trip ID, or -1 if the boat is not available.
     */
    public int recordTrip(int captainId, int boatId, String destination, double fuelCost) {
        int tripId;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE boats SET status = ? WHERE boat_id = ? AND status = ?")) {
                    ps.setString(1, Boat.STATUS_ON_TRIP);
                    ps.setInt(2, boatId);
                    ps.setString(3, Boat.STATUS_AVAILABLE);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return -1;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO trips (boat_id, captain_id, destination, departure_date, fuel_cost, status) "
                                + "VALUES (?, ?, ?, datetime('now', 'localtime'), ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, boatId);
                    ps.setInt(2, captainId);
                    ps.setString(3, destination);
                    ps.setDouble(4, fuelCost);
                    ps.setString(5, Trip.STATUS_ONGOING);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        tripId = keys.getInt(1);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record the trip: " + e.getMessage(), e);
        }
        // Logged after the trip is saved, e.g. "Recorded Trip #3 to Palawan with Boat #1 - Fuel Cost: P1,500.00"
        activityLogDao.logAction(captainId, ActivityLog.TRIP_RECORDED,
                String.format("Recorded Trip #%d to %s with Boat #%d - Fuel Cost: P%,.2f",
                        tripId, destination, boatId, fuelCost));
        return tripId;
    }

    /**
     * Moves an ONGOING trip to COMPLETED or CANCELLED, sets its return date and frees the boat.
     * Returns false if the trip does not exist or is no longer ONGOING.
     */
    public boolean updateTripStatus(int actingUserId, int tripId, String newStatus) {
        String oldStatus;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int boatId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT boat_id, status FROM trips WHERE trip_id = ?")) {
                    ps.setInt(1, tripId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        boatId = rs.getInt("boat_id");
                        oldStatus = rs.getString("status");
                    }
                }
                if (!Trip.STATUS_ONGOING.equalsIgnoreCase(oldStatus)) {
                    conn.rollback();
                    return false;
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE trips SET status = ?, return_date = datetime('now', 'localtime') WHERE trip_id = ?")) {
                    ps.setString(1, newStatus.toUpperCase());
                    ps.setInt(2, tripId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE boats SET status = ? WHERE boat_id = ?")) {
                    ps.setString(1, Boat.STATUS_AVAILABLE);
                    ps.setInt(2, boatId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the trip: " + e.getMessage(), e);
        }
        activityLogDao.logAction(actingUserId, ActivityLog.TRIP_STATUS_UPDATED,
                "Updated Trip #" + tripId + " status from " + oldStatus.toUpperCase()
                        + " to " + newStatus.toUpperCase());
        return true;
    }

    public List<Trip> getAllTrips() {
        return query("SELECT " + COLUMNS + " FROM trips ORDER BY trip_id DESC", null);
    }

    public List<Trip> getTripsByCaptain(int captainId) {
        return query("SELECT " + COLUMNS + " FROM trips WHERE captain_id = ? ORDER BY trip_id DESC", captainId);
    }

    private List<Trip> query(String sql, Integer param) {
        List<Trip> trips = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(new Trip(rs.getInt("trip_id"), rs.getInt("boat_id"), rs.getInt("captain_id"),
                            rs.getString("destination"), rs.getString("departure_date"),
                            rs.getString("return_date"), rs.getDouble("fuel_cost"), rs.getString("status")));
                }
            }
            return trips;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load trips: " + e.getMessage(), e);
        }
    }
}