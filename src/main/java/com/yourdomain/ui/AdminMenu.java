package com.yourdomain.ui;

import com.yourdomain.dao.ActivityLogDao;
import com.yourdomain.dao.BoatDao;
import com.yourdomain.dao.UserDao;
import com.yourdomain.model.ActivityLog;
import com.yourdomain.model.Boat;
import com.yourdomain.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menu for the Boat Owner / Administrator.
 */
public class AdminMenu {

    // Column formats: ID, Username, Role, Status.
    // The role column is wider than 12 because "Boat Owner / Administrator" is 26 characters.
    private static final String ROW_FORMAT = "| %-5s | %-15s | %-28s | %-10s |%n";
    private static final String BORDER = "+-------+-----------------+------------------------------+------------+";

    // Audit log columns: ID, User ID, Action, Description, Timestamp.
    private static final String LOG_ROW_FORMAT = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
    private static final String LOG_BORDER = "-".repeat(5 + 3 + 8 + 3 + 18 + 3 + 35 + 3 + 20);

    private static final List<String> VALID_ROLES =
            List.of(User.ROLE_OWNER_ADMIN, User.ROLE_CAPTAIN, User.ROLE_CLERK);
    private static final List<String> VALID_STATUSES =
            List.of(User.STATUS_APPROVED, User.STATUS_PENDING);

    private final Scanner scanner;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final User user;
    private final BoatDao boatDao = new BoatDao();

    public AdminMenu(Scanner scanner, UserDao userDao, ActivityLogDao activityLogDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
        this.user = user;
    }

    public void show() {
        System.out.println();
        System.out.println("Welcome, " + user.username() + "! You are logged in as the " + user.roles() + ".");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("===== ADMINISTRATOR MENU =====");
            System.out.println("1. View my profile");
            System.out.println("2. View all system users");
            System.out.println("3. View pending registrations");
            System.out.println("4. Approve user account");
            System.out.println("5. Update user role and status");
            System.out.println("6. Manage boats");
            System.out.println("7. View system audit logs");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> ProfileView.print(user);
                case "2" -> viewAllUsers();
                case "3" -> viewPendingUsers();
                case "4" -> approveUserAccount();
                case "5" -> updateUserRoleAndStatus();
                case "6" -> manageBoats();
                case "7" -> auditLogMenu();
                case "8" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Please enter 1 to 8.");
            }
        }
    }

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        System.out.println();
        System.out.println("--- All System Users ---");
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        printUserTable(users);
    }

    private void viewPendingUsers() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println();
        System.out.println("--- Pending Registrations ---");
        if (pending.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }
        printUserTable(pending);
    }

    private void approveUserAccount() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println();
        System.out.println("--- Approve User Account ---");
        if (pending.isEmpty()) {
            System.out.println("No pending registrations to approve.");
            return;
        }
        printUserTable(pending);

        while (true) {
            int id = readIntInput("Enter the Target User ID to approve (0 to cancel): ");
            if (id == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            boolean isPending = pending.stream().anyMatch(u -> u.id() == id);
            if (!isPending) {
                System.out.println("User ID " + id + " is not in the pending list. Try again.");
                continue;
            }

            // UserDao.approveUser records USER_APPROVED in the audit log.
            if (userDao.approveUser(user.id(), id)) {
                System.out.println("User ID " + id + " has been approved.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    /** Lets the admin change another user's role and status. Enter 0 at any step to cancel. */
    private void updateUserRoleAndStatus() {
        List<User> users = userDao.getAllUsers();
        System.out.println();
        System.out.println("--- Update User Role and Status ---");
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        printUserTable(users);

        User target = null;
        while (target == null) {
            int targetUserId = readIntInput("Enter the Target User ID to update (0 to cancel): ");
            if (targetUserId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (targetUserId == user.id()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }
            Optional<User> found = users.stream().filter(u -> u.id() == targetUserId).findFirst();
            if (found.isEmpty()) {
                System.out.println("User ID " + targetUserId + " does not exist. Try again.");
                continue;
            }
            target = found.get();
        }

        String newRole = null;
        while (newRole == null) {
            newRole = readValidRole();
            if (newRole == null) {
                System.out.println("Update cancelled.");
                return;
            }
            // Only one Boat Owner / Administrator is allowed in the system.
            if (User.ROLE_OWNER_ADMIN.equals(newRole) && !User.ROLE_OWNER_ADMIN.equalsIgnoreCase(target.roles())) {
                System.out.println("Only one Boat Owner / Administrator account is allowed. Choose another role.");
                newRole = null;
            }
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // UserDao.updateUserRoleAndStatus records USER_UPDATED in the audit log.
        if (userDao.updateUserRoleAndStatus(user.id(), target.id(), newRole, newStatus)) {
            System.out.println("User ID " + target.id() + " updated: role = " + newRole.toUpperCase()
                    + ", status = " + newStatus.toUpperCase() + ".");
        } else {
            System.out.println("Update failed. No changes were made.");
        }
    }

    // ---- input validation wrappers ----

    /** Keeps asking until the person types a whole number, so letters or symbols never crash the app. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    /**
     * Keeps asking until a valid role is entered (by number or by name).
     * Returns the role as written in User, or null if the person enters 0 to cancel.
     */
    private String readValidRole() {
        while (true) {
            System.out.println("Select new role:");
            for (int i = 0; i < VALID_ROLES.size(); i++) {
                System.out.println((i + 1) + ". " + VALID_ROLES.get(i));
            }
            System.out.print("Enter role (name or number, 0 to cancel): ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }
            for (int i = 0; i < VALID_ROLES.size(); i++) {
                if (input.equalsIgnoreCase(VALID_ROLES.get(i)) || input.equals(String.valueOf(i + 1))) {
                    return VALID_ROLES.get(i);
                }
            }
            System.out.println("Invalid role. Choose one of the roles listed.");
        }
    }

    /**
     * Keeps asking until a valid status (APPROVED or PENDING) is entered.
     * Returns it in upper case, or null if the person enters 0 to cancel.
     */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED / PENDING, 0 to cancel): ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }
            for (String status : VALID_STATUSES) {
                if (input.equalsIgnoreCase(status)) {
                    return status;
                }
            }
            System.out.println("Invalid status. Please enter APPROVED or PENDING.");
        }
    }

    // ---- boats ----

    /** Submenu to register boats and list them. Enter 0 to go back. */
    private void manageBoats() {
        boolean inBoatMenu = true;
        while (inBoatMenu) {
            System.out.println();
            System.out.println("===== MANAGE BOATS =====");
            System.out.println("1. View all boats");
            System.out.println("2. Register a new boat");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    List<Boat> boats = boatDao.getAllBoats();
                    if (boats.isEmpty()) {
                        System.out.println("No boats registered yet.");
                    } else {
                        printBoatTable(boats);
                    }
                }
                case "2" -> registerBoat();
                case "0" -> inBoatMenu = false;
                default -> System.out.println("Invalid option. Please enter 1, 2 or 0.");
            }
        }
    }

    private void registerBoat() {
        System.out.println();
        System.out.println("--- Register a New Boat ---");
        String name = readText("Boat name (0 to cancel): ");
        if (name == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        String registration = readText("Registration number (0 to cancel): ");
        if (registration == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        String type = readText("Boat type, e.g. Bangka (0 to cancel): ");
        if (type == null) {
            System.out.println("Registration cancelled.");
            return;
        }
        Double capacity = readPositiveDouble("Capacity in tons (0 to cancel): ");
        if (capacity == null) {
            System.out.println("Registration cancelled.");
            return;
        }

        Boat boat = new Boat(0, name, registration, type, capacity, Boat.STATUS_AVAILABLE);
        if (boatDao.addBoat(user.id(), boat)) {
            System.out.println("Boat " + name + " registered and set to " + Boat.STATUS_AVAILABLE + ".");
        } else {
            System.out.println("That registration number is already used.");
        }
    }

    // ---- boat helpers ----

    private static final String BOAT_ROW = "| %-5s | %-20s | %-16s | %-12s | %-10s | %-10s |%n";
    private static final String BOAT_BORDER =
            "+-------+----------------------+------------------+--------------+------------+------------+";

    private void printBoatTable(List<Boat> boats) {
        System.out.println(BOAT_BORDER);
        System.out.printf(BOAT_ROW, "ID", "Name", "Registration No.", "Type", "Capacity", "Status");
        System.out.println(BOAT_BORDER);
        for (Boat b : boats) {
            System.out.printf(BOAT_ROW, b.boatId(), fit(b.boatName(), 20), fit(b.registrationNo(), 16),
                    fit(b.boatType(), 12), String.format("%.1f", b.capacity()), fit(b.status(), 10));
        }
        System.out.println(BOAT_BORDER);
        System.out.println("Total: " + boats.size());
    }

    /** Keeps asking until non-empty text is entered. Returns null if the person enters 0. */
    private String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            if (input.isEmpty()) {
                System.out.println("This field cannot be empty.");
                continue;
            }
            return input;
        }
    }

    /** Keeps asking until a number greater than 0 is entered. Returns null if the person enters 0. */
    private Double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            try {
                double value = Double.parseDouble(input);
                if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
                    System.out.println("Please enter a number greater than 0.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // ---- audit logs ----

    /** Submenu for the audit trail: view all, filter by user, or clear. Enter 0 to go back. */
    private void auditLogMenu() {
        boolean inAuditMenu = true;
        while (inAuditMenu) {
            System.out.println();
            System.out.println("===== SYSTEM AUDIT LOGS =====");
            System.out.println("1. View all system logs");
            System.out.println("2. Filter logs by user ID");
            System.out.println("3. Clear all logs");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printLogTable(activityLogDao.getAllLogs());
                case "2" -> filterLogsByUser();
                case "3" -> clearLogs();
                case "0" -> inAuditMenu = false;
                default -> System.out.println("Invalid option. Please enter 1, 2, 3 or 0.");
            }
        }
    }

    private void filterLogsByUser() {
        int userId = readIntInput("Enter the User ID to filter by (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        List<ActivityLog> logs = activityLogDao.getLogsByUserId(userId);
        if (logs.isEmpty()) {
            System.out.println("No logs found for User ID " + userId + ".");
            return;
        }
        printLogTable(logs);
    }

    /** Deletes all logs, but only after the admin types YES. The clearing itself is logged. */
    private void clearLogs() {
        System.out.println("WARNING: This permanently deletes every audit log entry.");
        System.out.print("Type YES to confirm (anything else cancels): ");
        if (!scanner.nextLine().trim().equals("YES")) {
            System.out.println("Clear cancelled. No logs were deleted.");
            return;
        }
        int removed = activityLogDao.clearLogs();
        System.out.println(removed + " log entries deleted.");
        activityLogDao.logAction(user.id(), ActivityLog.LOGS_CLEARED,
                "Admin [" + user.id() + "] cleared " + removed + " log entries");
    }

    /** Splits text into lines of at most the given width, breaking at spaces where possible. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String rest = text == null ? "" : text.trim();
        while (rest.length() > width) {
            int cut = rest.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width; // no space found, break the word
            }
            lines.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        lines.add(rest);
        return lines;
    }

    /** Prints log entries as an aligned table. Long descriptions continue on extra lines. */
    private void printLogTable(List<ActivityLog> logs) {
        System.out.println();
        if (logs.isEmpty()) {
            System.out.println("No logs recorded yet.");
            return;
        }
        System.out.println(LOG_BORDER);
        System.out.printf(LOG_ROW_FORMAT, "ID", "User ID", "Action", "Description", "Timestamp");
        System.out.println(LOG_BORDER);
        for (ActivityLog log : logs) {
            List<String> descriptionLines = wrap(log.description(), 35);
            System.out.printf(LOG_ROW_FORMAT, log.logId(), log.userId(),
                    fit(log.actionType(), 18), descriptionLines.get(0), fit(log.timestamp(), 20));
            // A long description continues on extra lines so no information is cut off.
            for (int i = 1; i < descriptionLines.size(); i++) {
                System.out.printf(LOG_ROW_FORMAT, "", "", "", descriptionLines.get(i), "");
            }
        }
        System.out.println(LOG_BORDER);
        System.out.println("Total: " + logs.size());
    }

    /** Cuts text to the column width, ending with "..." when it is shortened. */
    private static String fit(String text, int width) {
        if (text == null) {
            return "";
        }
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }

    /** Prints any list of users as an aligned table with borders. */
    private void printUserTable(List<User> users) {
        System.out.println(BORDER);
        System.out.printf(ROW_FORMAT, "ID", "Username", "Role", "Status");
        System.out.println(BORDER);
        for (User u : users) {
            System.out.printf(ROW_FORMAT, u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println(BORDER);
        System.out.println("Total: " + users.size());
    }
}