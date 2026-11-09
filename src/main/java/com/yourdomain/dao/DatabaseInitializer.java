package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes sure every required table exists before the menu starts.
 * Tables follow the approved ERD (parents are created before children).
 */
public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        String[] statements = {
                usersTableSql("users"),
                """
                CREATE TABLE IF NOT EXISTS boats (
                    boat_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    boat_name       TEXT NOT NULL,
                    registration_no TEXT NOT NULL UNIQUE,
                    boat_type       TEXT,
                    capacity        REAL,
                    status          TEXT NOT NULL DEFAULT 'Available'
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS trips (
                    trip_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    boat_id        INTEGER NOT NULL,
                    captain_id     INTEGER NOT NULL,
                    destination    TEXT,
                    departure_date TIMESTAMP NOT NULL,
                    return_date    TIMESTAMP,
                    fuel_cost      REAL,
                    status         TEXT NOT NULL DEFAULT 'ONGOING',
                    FOREIGN KEY (boat_id)    REFERENCES boats(boat_id),
                    FOREIGN KEY (captain_id) REFERENCES users(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS catch_records (
                    catch_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    trip_id      INTEGER NOT NULL,
                    fish_species TEXT NOT NULL,
                    weight_kg    REAL NOT NULL,
                    price_per_kg REAL NOT NULL,
                    FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
                )
                """,
                ACTIVITY_LOGS_SQL
        };

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
            upgradeUsersTableIfNeeded(conn);
            upgradeActivityLogsIfNeeded(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the database: " + e.getMessage(), e);
        }
    }

    /** Audit trail table. Timestamps are stored in local time. */
    private static final String ACTIVITY_LOGS_SQL = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id     INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp   TIMESTAMP NOT NULL DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;

    /**
     * Brings an older activity_logs table up to date and keeps its rows. Two older versions exist:
     * the first had different columns (action, table_affected, record_id, log_time), and the
     * second lacked ON DELETE CASCADE on the user_id foreign key.
     */
    private static void upgradeActivityLogsIfNeeded(Connection conn) throws SQLException {
        boolean hasNewLayout = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(activity_logs)")) {
            while (rs.next()) {
                if ("action_type".equalsIgnoreCase(rs.getString("name"))) {
                    hasNewLayout = true;
                }
            }
        }
        boolean hasCascade = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'activity_logs'")) {
            if (rs.next() && rs.getString(1) != null) {
                hasCascade = rs.getString(1).toUpperCase().contains("ON DELETE CASCADE");
            }
        }
        if (hasNewLayout && hasCascade) {
            return;
        }

        String copySql = hasNewLayout
                ? "INSERT INTO activity_logs (log_id, user_id, action_type, description, timestamp) "
                + "SELECT log_id, user_id, action_type, description, timestamp FROM activity_logs_old"
                : "INSERT INTO activity_logs (log_id, user_id, action_type, description, timestamp) "
                + "SELECT log_id, user_id, action, "
                + "COALESCE(table_affected || ' #' || record_id, table_affected, action), log_time "
                + "FROM activity_logs_old";

        try (Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                stmt.execute("ALTER TABLE activity_logs RENAME TO activity_logs_old");
                stmt.execute(ACTIVITY_LOGS_SQL);
                stmt.execute(copySql);
                stmt.execute("DROP TABLE activity_logs_old");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** CREATE TABLE statement for the users table, with a case-insensitive role check. */
    private static String usersTableSql(String tableName) {
        return """
                CREATE TABLE IF NOT EXISTS %s (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles    TEXT NOT NULL CHECK (UPPER(roles) IN
                             ('BOAT OWNER / ADMINISTRATOR', 'BOAT CAPTAIN', 'PORT CLERK (ENCODER)')),
                    status   TEXT NOT NULL DEFAULT 'PENDING'
                )
                """.formatted(tableName);
    }

    /**
     * Older databases were created with a case-sensitive role check, which rejects
     * upper-case roles. If the users table still has that check, rebuild it with the
     * new check and keep all existing accounts.
     */
    private static void upgradeUsersTableIfNeeded(Connection conn) throws SQLException {
        String existingSql = null;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'users'")) {
            if (rs.next()) {
                existingSql = rs.getString(1);
            }
        }
        if (existingSql == null || existingSql.toUpperCase().contains("UPPER(ROLES)")) {
            return; // already up to date
        }

        try (Statement stmt = conn.createStatement()) {
            // Other tables point at users(id), so foreign keys must be off while it is rebuilt.
            stmt.execute("PRAGMA foreign_keys = OFF");
            conn.setAutoCommit(false);
            try {
                stmt.execute(usersTableSql("users_new"));
                stmt.execute("INSERT INTO users_new (id, username, password, roles, status) "
                        + "SELECT id, username, password, roles, status FROM users");
                stmt.execute("DROP TABLE users");
                stmt.execute("ALTER TABLE users_new RENAME TO users");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
    }
}