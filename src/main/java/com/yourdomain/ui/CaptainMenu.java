package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.Scanner;

/**
 * Menu for the Boat Captain.
 */
public class CaptainMenu {

    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public CaptainMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        System.out.println();
        System.out.println("Welcome, Captain " + user.username() + "!");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("===== BOAT CAPTAIN MENU =====");
            System.out.println("1. View my profile");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> ProfileView.print(user);
                case "2" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Please enter 1 or 2.");
            }
        }
    }
}
