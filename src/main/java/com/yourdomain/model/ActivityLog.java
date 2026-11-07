package com.yourdomain.model;

/**
 * Immutable data model for a row in the activity_logs table.
 */
public record ActivityLog(int logId, int userId, String actionType, String description, String timestamp) {

    public static final String USER_APPROVED = "USER_APPROVED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String LOGS_CLEARED = "LOGS_CLEARED";
}