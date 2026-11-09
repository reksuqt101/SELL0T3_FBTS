package com.yourdomain.ui;

import com.yourdomain.dao.TripDao;
import com.yourdomain.dao.UserDao;
import com.yourdomain.model.Trip;
import com.yourdomain.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Menu for the Port Clerk (Encoder): view the recorded trips.
 */
public class ClerkMenu {

    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;
    private final TripDao tripDao = new TripDao();

    public ClerkMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        System.out.println();
        System.out.println("Welcome, " + user.username() + "! (" + user.roles() + ")");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("===== PORT CLERK MENU =====");
            System.out.println("1. View my profile");
            System.out.println("2. View all trips");
            System.out.println("3. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> ProfileView.print(user);
                case "2" -> viewAllTrips();
                case "3" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Please enter 1, 2 or 3.");
            }
        }
    }

    private void viewAllTrips() {
        List<Trip> trips = tripDao.getAllTrips();
        System.out.println();
        System.out.println("--- All Trips ---");
        if (trips.isEmpty()) {
            System.out.println("No trips have been recorded yet.");
            return;
        }
        TableView.printTrips(trips);
    }
}