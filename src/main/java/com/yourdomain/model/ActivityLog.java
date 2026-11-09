package com.yourdomain.model;

/**
 * Immutable data model for a row in the activity_logs table.
 * The username comes from the users table, so the audit trail shows a name instead of a bare ID.
 */
public record ActivityLog(int logId, int userId, String username, String actionType,
                          String description, String timestamp) {

    public static final String USER_LOGIN = "USER_LOGIN";
    public static final String USER_LOGOUT = "USER_LOGOUT";
    public static final String USER_APPROVED = "USER_APPROVED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String BOAT_ADDED = "BOAT_ADDED";
    public static final String TRIP_RECORDED = "TRIP_RECORDED";
    public static final String TRIP_STATUS_UPDATED = "TRIP_STATUS_UPDATED";
    public static final String LOGS_CLEARED = "LOGS_CLEARED";
}