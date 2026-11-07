package com.yourdomain.dao;

import com.yourdomain.model.ActivityLog;
import com.yourdomain.model.Boat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the boats table. Every query uses a PreparedStatement.
 */
public class BoatDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    /** Saves a new boat. Returns false if the registration number is already used. */
    public boolean addBoat(int userId, Boat boat) {
        String sql = "INSERT INTO boats (boat_name, registration_no, boat_type, capacity, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boat.boatName());
            ps.setString(2, boat.registrationNo());
            ps.setString(3, boat.boatType());
            ps.setDouble(4, boat.capacity());
            ps.setString(5, boat.status());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return false;
            }
            throw new IllegalStateException("Could not add the boat: " + e.getMessage(), e);
        }
        activityLogDao.logAction(userId, ActivityLog.BOAT_ADDED,
                "Added boat " + boat.boatName() + " (Reg. No. " + boat.registrationNo() + ")");
        return true;
    }

    /** Every boat, ordered by id. */
    public List<Boat> getAllBoats() {
        return query("SELECT boat_id, boat_name, registration_no, boat_type, capacity, status "
                + "FROM boats ORDER BY boat_id");
    }

    /** Only boats that can be sent on a new trip. */
    public List<Boat> getAvailableBoats() {
        return query("SELECT boat_id, boat_name, registration_no, boat_type, capacity, status "
                + "FROM boats WHERE UPPER(status) = 'AVAILABLE' ORDER BY boat_id");
    }

    private List<Boat> query(String sql) {
        List<Boat> boats = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boats.add(new Boat(rs.getInt("boat_id"), rs.getString("boat_name"),
                        rs.getString("registration_no"), rs.getString("boat_type"),
                        rs.getDouble("capacity"), rs.getString("status")));
            }
            return boats;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the boats: " + e.getMessage(), e);
        }
    }
}