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
 */
public class ActivityLogDao {

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
        String sql = "SELECT log_id, user_id, action_type, description, timestamp "
                + "FROM activity_logs ORDER BY timestamp DESC, log_id DESC";
        return query(sql, null);
    }

    /** Log entries made by one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = "SELECT log_id, user_id, action_type, description, timestamp "
                + "FROM activity_logs WHERE user_id = ? ORDER BY timestamp DESC, log_id DESC";
        return query(sql, userId);
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
                            rs.getString("action_type"), rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
            return logs;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the logs: " + e.getMessage(), e);
        }
    }
}