package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.Optional;
import java.util.Scanner;

/**
 * Handles the first screen: register, login and routing to the role menu.
 * Contains no SQL; all data access goes through the shared UserDao.
 */
public class Authentication {

    private final Scanner scanner;
    private final UserDao userDao;

    public Authentication(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
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
            status = User.STATUS_ACTIVE;
            System.out.println("First account detected: you are registered as " + role + ".");
        } else {
            role = chooseRole();
            if (role == null) {
                return;
            }
            status = User.STATUS_PENDING;
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

    private String chooseRole() {
        System.out.println("Select role:");
        System.out.println("1. " + User.ROLE_OWNER_ADMIN);
        System.out.println("2. " + User.ROLE_CAPTAIN);
        System.out.println("3. " + User.ROLE_CLERK);
        System.out.print("Choose role: ");
        return switch (scanner.nextLine().trim()) {
            case "1" -> User.ROLE_OWNER_ADMIN;
            case "2" -> User.ROLE_CAPTAIN;
            case "3" -> User.ROLE_CLERK;
            default -> {
                System.out.println("Invalid role. Registration cancelled.");
                yield null;
            }
        };
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
        routeToMenu(user);
    }

    /** Sends the logged-in user to the menu that matches their role. */
    private void routeToMenu(User user) {
        switch (user.roles()) {
            case User.ROLE_OWNER_ADMIN -> new AdminMenu(scanner, userDao, user).show();
            case User.ROLE_CAPTAIN -> new CaptainMenu(scanner, userDao, user).show();
            case User.ROLE_CLERK -> new ClerkMenu(scanner, userDao, user).show();
            default -> System.out.println("Unknown role: " + user.roles());
        }
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }
}
