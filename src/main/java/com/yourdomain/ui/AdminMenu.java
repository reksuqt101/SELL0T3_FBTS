package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Menu for the Boat Owner / Administrator.
 */
public class AdminMenu {

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
            System.out.println("2. Approve pending accounts");
            System.out.println("3. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> ProfileView.print(user);
                case "2" -> approvePending();
                case "3" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Please enter 1, 2, or 3.");
            }
        }
    }

    private void approvePending() {
        List<User> pending = userDao.findPending();
        if (pending.isEmpty()) {
            System.out.println("No pending accounts.");
            return;
        }
        System.out.println("--- Pending accounts ---");
        for (User u : pending) {
            System.out.println(u.id() + ". " + u.username() + " (" + u.roles() + ")");
        }
        System.out.print("Enter the ID to approve (or press Enter to go back): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return;
        }
        try {
            int id = Integer.parseInt(input);
            boolean listed = pending.stream().anyMatch(u -> u.id() == id);
            if (listed && userDao.approve(id)) {
                System.out.println("Account approved. Status is now ACTIVE.");
            } else {
                System.out.println("No pending account with that ID.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }
}
