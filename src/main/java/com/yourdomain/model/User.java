package com.yourdomain.model;

/**
 * Immutable data model for a row in the users table.
 */
public record User(int id, String username, String password, String roles, String status) {

    public static final String ROLE_OWNER_ADMIN = "Boat Owner / Administrator";
    public static final String ROLE_CAPTAIN = "Boat Captain";
    public static final String ROLE_CLERK = "Port Clerk (Encoder)";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** Creates a new, not-yet-saved user (id 0) with the given role and status. */
    public static User newUser(String username, String password, String roles, String status) {
        return new User(0, username, password, roles, status);
    }
}
