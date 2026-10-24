package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections to the SQLite database file in the project root.
 * Foreign key enforcement is switched on for every connection.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:fbts.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // SQLite ignores foreign keys unless this is enabled per connection.
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
