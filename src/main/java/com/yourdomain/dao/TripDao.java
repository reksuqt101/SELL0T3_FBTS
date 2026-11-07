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
import java.util.Locale;

/**
 * All SQL for the trips table. Every query uses a PreparedStatement.
 */
public class TripDao {

    private static final String COLUMNS =
            "trip_id, boat_id, captain_id, destination, departure_date, return_date, fuel_cost, status";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    /**
     * Records a new trip as ONGOING and marks the boat as On Trip.
     * Returns the new trip id, or -1 if the boat does not exist or is not Available.
     */
    public int recordTrip(int captainId, int boatId, String destination, double fuelCost) {
        String boatName = null;
        int tripId = -1;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT boat_name FROM boats WHERE boat_id = ? AND UPPER(status) = 'AVAILABLE'")) {
                    ps.setInt(1, boatId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            boatName = rs.getString(1);
                        }
                    }
                }
                if (boatName == null) {
                    conn.rollback();
                    return -1;
                }

                String insert = "INSERT INTO trips (boat_id, captain_id, destination, departure_date, "
                        + "fuel_cost, status) VALUES (?, ?, ?, datetime('now', 'localtime'), ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, boatId);
                    ps.setInt(2, captainId);
                    ps.setString(3, destination);
                    ps.setDouble(4, fuelCost);
                    ps.setString(5, Trip.STATUS_ONGOING);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            tripId = keys.getInt(1);
                        }
                    }
                }
                setBoatStatus(conn, boatId, Boat.STATUS_ON_TRIP);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record the trip: " + e.getMessage(), e);
        }
        activityLogDao.logAction(captainId, ActivityLog.TRIP_RECORDED,
                "Recorded Trip #" + tripId + " - Boat: " + boatName + ", Destination: " + destination);
        return tripId;
    }

    /**
     * Changes an ONGOING trip to COMPLETED or CANCELLED and frees the boat.
     * Returns false if the trip does not exist, is not ONGOING, or the new status is not allowed.
     */
    public boolean updateTripStatus(int userId, int tripId, String newStatus) {
        String target = newStatus == null ? "" : newStatus.trim().toUpperCase(Locale.ROOT);
        if (!target.equals(Trip.STATUS_COMPLETED) && !target.equals(Trip.STATUS_CANCELLED)) {
            return false;
        }
        String oldStatus = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int boatId = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT boat_id, status FROM trips WHERE trip_id = ?")) {
                    ps.setInt(1, tripId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            boatId = rs.getInt("boat_id");
                            oldStatus = rs.getString("status").toUpperCase(Locale.ROOT);
                        }
                    }
                }
                if (oldStatus == null || !oldStatus.equals(Trip.STATUS_ONGOING)) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE trips SET status = ?, return_date = datetime('now', 'localtime') "
                                + "WHERE trip_id = ?")) {
                    ps.setString(1, target);
                    ps.setInt(2, tripId);
                    ps.executeUpdate();
                }
                setBoatStatus(conn, boatId, Boat.STATUS_AVAILABLE);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the trip: " + e.getMessage(), e);
        }
        activityLogDao.logAction(userId, ActivityLog.TRIP_STATUS_UPDATED,
                "Updated Trip #" + tripId + " status from " + oldStatus + " to " + target);
        return true;
    }

    /** Every trip, newest first. */
    public List<Trip> getAllTrips() {
        return query("SELECT " + COLUMNS + " FROM trips ORDER BY trip_id DESC", null, null);
    }

    /** Trips recorded by one captain, newest first. */
    public List<Trip> getTripsByCaptain(int captainId) {
        return query("SELECT " + COLUMNS + " FROM trips WHERE captain_id = ? ORDER BY trip_id DESC",
                captainId, null);
    }

    /** One captain's trips that are still ONGOING. */
    public List<Trip> getOngoingTripsByCaptain(int captainId) {
        return query("SELECT " + COLUMNS + " FROM trips WHERE captain_id = ? AND UPPER(status) = ? "
                + "ORDER BY trip_id DESC", captainId, Trip.STATUS_ONGOING);
    }

    private void setBoatStatus(Connection conn, int boatId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE boats SET status = ? WHERE boat_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, boatId);
            ps.executeUpdate();
        }
    }

    private List<Trip> query(String sql, Integer captainId, String status) {
        List<Trip> trips = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            if (captainId != null) {
                ps.setInt(index++, captainId);
            }
            if (status != null) {
                ps.setString(index, status);
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
            throw new IllegalStateException("Could not load the trips: " + e.getMessage(), e);
        }
    }
}