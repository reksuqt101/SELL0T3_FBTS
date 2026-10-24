package com.yourdomain.dao;

import java.sql.Connection;
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
                """
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles    TEXT NOT NULL CHECK (roles IN
                             ('Boat Owner / Administrator', 'Boat Captain', 'Port Clerk (Encoder)')),
                    status   TEXT NOT NULL DEFAULT 'PENDING'
                )
                """,
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
                    status         TEXT NOT NULL DEFAULT 'Ongoing',
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
                """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    log_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id        INTEGER NOT NULL,
                    action         TEXT NOT NULL,
                    table_affected TEXT,
                    record_id      INTEGER,
                    log_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """
        };

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the database: " + e.getMessage(), e);
        }
    }
}
