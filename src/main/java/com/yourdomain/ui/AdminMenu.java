package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

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

    private static final List<String> VALID_ROLES =
            List.of(User.ROLE_OWNER_ADMIN, User.ROLE_CAPTAIN, User.ROLE_CLERK);
    private static final List<String> VALID_STATUSES =
            List.of(User.STATUS_APPROVED, User.STATUS_PENDING);

    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public AdminMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
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
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> ProfileView.print(user);
                case "2" -> viewAllUsers();
                case "3" -> viewPendingUsers();
                case "4" -> approveUserAccount();
                case "5" -> updateUserRoleAndStatus();
                case "6" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Please enter 1 to 6.");
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

            if (userDao.approveUser(id)) {
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

        if (userDao.updateUserRoleAndStatus(target.id(), newRole, newStatus)) {
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