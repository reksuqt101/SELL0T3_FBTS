package com.yourdomain.dao;

import com.yourdomain.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the activity_logs table (the audit trail). Every query uses a PreparedStatement.
 * Reads join the users table so each entry carries the username, not just the user ID.
 */
public class ActivityLogDao {

    private static final String SELECT_WITH_USERNAME =
            "SELECT l.log_id, l.user_id, COALESCE(u.username, '(deleted user)') AS username, "
                    + "l.action_type, l.description, l.timestamp "
                    + "FROM activity_logs l LEFT JOIN users u ON u.id = l.user_id ";

    /**
     * Records one action. A failure to write the log is reported but never thrown,
     * so a logging problem cannot undo or crash the action that was just completed.
     */
    public void logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Warning: could not write the audit log: " + e.getMessage());
        }
    }

    /** Every log entry, newest first. */
    public List<ActivityLog> getAllLogs() {
        return query(SELECT_WITH_USERNAME + "ORDER BY l.timestamp DESC, l.log_id DESC", null);
    }

    /** Log entries made by one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        return query(SELECT_WITH_USERNAME + "WHERE l.user_id = ? ORDER BY l.timestamp DESC, l.log_id DESC", userId);
    }

    /** Turns a user ID into a username for log descriptions. Falls back to "User #id" if not found. */
    public String usernameOf(int userId) {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "User #" + userId;
            }
        } catch (SQLException e) {
            return "User #" + userId;
        }
    }

    /** Deletes every log entry. Returns how many were removed. */
    public int clearLogs() {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not clear the logs: " + e.getMessage(), e);
        }
    }

    private List<ActivityLog> query(String sql, Integer userId) {
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(rs.getInt("log_id"), rs.getInt("user_id"),
                            rs.getString("username"), rs.getString("action_type"),
                            rs.getString("description"), rs.getString("timestamp")));
                }
            }
            return logs;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the logs: " + e.getMessage(), e);
        }
    }
}