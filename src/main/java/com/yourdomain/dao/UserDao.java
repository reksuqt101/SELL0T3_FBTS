package com.yourdomain.dao;

import com.yourdomain.model.ActivityLog;
import com.yourdomain.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * All SQL for the users table. Every query uses a PreparedStatement.
 * Passwords are stored as "salt:hash" (PBKDF2), never as plain text.
 */
public class UserDao {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    /** Saves a new user. Returns false if the username is already taken. */
    public boolean register(User user) {
        String sql = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, hashPassword(user.password()));
            ps.setString(3, user.roles());
            ps.setString(4, user.status());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return false;
            }
            throw new IllegalStateException("Registration failed: " + e.getMessage(), e);
        }
    }

    /** Number of registered users (0 means the next account is the very first one). */
    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Count failed: " + e.getMessage(), e);
        }
    }

    /** True if a Boat Owner / Administrator account already exists (only one is allowed). */
    public boolean adminExists() {
        String sql = "SELECT 1 FROM users WHERE UPPER(roles) = UPPER(?) LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, User.ROLE_OWNER_ADMIN);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Admin lookup failed: " + e.getMessage(), e);
        }
    }

    /** Every registered account, regardless of role or status. */
    public List<User> getAllUsers() {
        String sql = "SELECT id, username, password, roles, status FROM users ORDER BY id";
        return queryUsers(sql, null);
    }

    /** All accounts (any role) whose status is PENDING. */
    public List<User> getPendingUsers() {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE status = ? ORDER BY id";
        return queryUsers(sql, User.STATUS_PENDING);
    }

    /**
     * Sets the target account's status to APPROVED and logs USER_APPROVED:
     * "Admin [1] approved User [5]". Returns true if a row was updated.
     */
    public boolean approveUser(int adminId, int targetUserId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, targetUserId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Approval failed: " + e.getMessage(), e);
        }
        if (updated) {
            activityLogDao.logAction(adminId, ActivityLog.USER_APPROVED,
                    "Admin [" + adminId + "] approved User [" + targetUserId + "]");
        }
        return updated;
    }

    /**
     * Changes a user's role and status (saved in upper case) and logs USER_UPDATED:
     * "Admin [1] changed User [5] role to BOAT CAPTAIN, status to APPROVED".
     * Returns true if a row was updated.
     */
    public boolean updateUserRoleAndStatus(int adminId, int targetUserId, String newRole, String newStatus) {
        String role = newRole.toUpperCase(Locale.ROOT);
        String status = newStatus.toUpperCase(Locale.ROOT);
        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";
        boolean updated;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, status);
            ps.setInt(3, targetUserId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Update failed: " + e.getMessage(), e);
        }
        if (updated) {
            activityLogDao.logAction(adminId, ActivityLog.USER_UPDATED,
                    "Admin [" + adminId + "] changed User [" + targetUserId + "] role to " + role
                            + ", status to " + status);
        }
        return updated;
    }

    /** Runs a user SELECT with an optional single String parameter and maps the rows. */
    private List<User> queryUsers(String sql, String param) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(rs.getInt("id"), rs.getString("username"),
                            rs.getString("password"), rs.getString("roles"), rs.getString("status")));
                }
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load users: " + e.getMessage(), e);
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lookup failed: " + e.getMessage(), e);
        }
    }

    /** Returns the user if the username and password match, otherwise empty. */
    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && verifyPassword(password, rs.getString("password"))) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            rs.getString("status")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Login failed: " + e.getMessage(), e);
        }
    }

    // ---- password helpers ----

    private static String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(String password, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        return MessageDigest.isEqual(expected, pbkdf2(password, salt));
    }

    private static byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}