package com.yourdomain.ui;

import com.yourdomain.dao.ActivityLogDao;
import com.yourdomain.dao.UserDao;
import com.yourdomain.model.ActivityLog;
import com.yourdomain.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Handles the first screen: register, login and routing to the role menu.
 * Contains no SQL; all data access goes through the shared UserDao.
 */
public class Authentication {

    private final Scanner scanner;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;

    public Authentication(Scanner scanner, UserDao userDao, ActivityLogDao activityLogDao) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== FISHING BOAT TRIP SYSTEM =====");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please enter 1, 2, or 3.");
            }
        }
    }

    private void register() {
        System.out.println();
        System.out.println("--- Register Account ---");

        String username = prompt("Username: ");
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("That username is already taken.");
            return;
        }

        String password = prompt("Create password (min 4 characters): ");
        if (password.length() < 4) {
            System.out.println("Password must be at least 4 characters.");
            return;
        }
        if (!password.equals(prompt("Confirm password: "))) {
            System.out.println("Passwords do not match. Registration cancelled.");
            return;
        }

        String role;
        String status;
        if (userDao.countUsers() == 0) {
            role = User.ROLE_OWNER_ADMIN;
            status = User.STATUS_APPROVED;
            System.out.println("First account detected: you are registered as " + role + ".");
        } else {
            role = chooseRole();
            if (role == null) {
                return;
            }
            // If no administrator exists yet, nobody could approve this account, so approve it directly.
            status = User.ROLE_OWNER_ADMIN.equalsIgnoreCase(role) ? User.STATUS_APPROVED : User.STATUS_PENDING;
        }

        User newUser = User.newUser(username, password, role, status);
        if (userDao.register(newUser)) {
            System.out.println("Account created! Role: " + role + ", Status: " + status);
            if (User.STATUS_PENDING.equals(status)) {
                System.out.println("Please wait for the administrator to approve your account.");
            }
        } else {
            System.out.println("That username is already taken.");
        }
    }

    /**
     * Shows the roles a new user may pick. Only one Boat Owner / Administrator is allowed,
     * so that option is hidden once an administrator account exists.
     */
    private String chooseRole() {
        List<String> roles = new ArrayList<>();
        if (!userDao.adminExists()) {
            roles.add(User.ROLE_OWNER_ADMIN);
        }
        roles.add(User.ROLE_CAPTAIN);
        roles.add(User.ROLE_CLERK);

        System.out.println("Select role:");
        for (int i = 0; i < roles.size(); i++) {
            System.out.println((i + 1) + ". " + roles.get(i));
        }
        System.out.print("Choose role: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= roles.size()) {
                return roles.get(choice - 1);
            }
        } catch (NumberFormatException ignored) {
            // falls through to the invalid message
        }
        System.out.println("Invalid role. Registration cancelled.");
        return null;
    }

    private void login() {
        System.out.println();
        System.out.println("--- Login ---");
        String username = prompt("Username: ");
        String password = prompt("Password: ");

        Optional<User> result = userDao.login(username, password);
        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }

        User user = result.get();
        if (User.STATUS_PENDING.equals(user.status())) {
            System.out.println("Your account is still PENDING approval by the administrator.");
            return;
        }
        activityLogDao.logAction(user.id(), ActivityLog.USER_LOGIN,
                "User " + user.username() + " (ID: " + user.id() + ") logged in.");
        routeToMenu(user);
        logout(user);
    }

    /** Called when a role menu ends because the user logged out; writes the audit log entry. */
    private void logout(User user) {
        activityLogDao.logAction(user.id(), ActivityLog.USER_LOGOUT,
                "User " + user.username() + " (ID: " + user.id() + ") logged out.");
    }

    /** Sends the logged-in user to the menu that matches their role. */
    private void routeToMenu(User user) {
        // Roles may be stored in upper case after an admin update, so compare ignoring case.
        String role = user.roles();
        if (User.ROLE_OWNER_ADMIN.equalsIgnoreCase(role)) {
            new AdminMenu(scanner, userDao, activityLogDao, user).show();
        } else if (User.ROLE_CAPTAIN.equalsIgnoreCase(role)) {
            new CaptainMenu(scanner, userDao, user).show();
        } else if (User.ROLE_CLERK.equalsIgnoreCase(role)) {
            new ClerkMenu(scanner, userDao, user).show();
        } else {
            System.out.println("Unknown role: " + role);
        }
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }
}