package com.yourdomain.ui;

import com.yourdomain.dao.BoatDao;
import com.yourdomain.dao.TripDao;
import com.yourdomain.dao.UserDao;
import com.yourdomain.model.Boat;
import com.yourdomain.model.Trip;
import com.yourdomain.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Menu for the Boat Captain: start trips, view own trips and close them.
 */
public class CaptainMenu {

    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;
    private final BoatDao boatDao = new BoatDao();
    private final TripDao tripDao = new TripDao();

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
            System.out.println("2. Record a trip");
            System.out.println("3. View my trips");
            System.out.println("4. Update trip status");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> ProfileView.print(user);
                case "2" -> recordTrip();
                case "3" -> viewMyTrips();
                case "4" -> updateTripStatus();
                case "5" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Please enter 1 to 5.");
            }
        }
    }

    private void recordTrip() {
        List<Boat> boats = boatDao.getAvailableBoats();
        System.out.println();
        System.out.println("--- Record a Trip ---");
        if (boats.isEmpty()) {
            System.out.println("No boats are available. Ask the administrator to register a boat.");
            return;
        }
        TableView.printBoats(boats);

        int boatId;
        while (true) {
            boatId = InputHelper.readInt(scanner, "Enter the Boat ID to use (0 to cancel): ");
            if (boatId == 0) {
                System.out.println("Trip cancelled.");
                return;
            }
            int chosen = boatId;
            if (boats.stream().anyMatch(b -> b.boatId() == chosen)) {
                break;
            }
            System.out.println("Boat ID " + boatId + " is not in the available list. Try again.");
        }

        String destination = InputHelper.readText(scanner, "Destination (0 to cancel): ");
        if (destination == null) {
            System.out.println("Trip cancelled.");
            return;
        }
        Double fuelCost = InputHelper.readPositiveDouble(scanner, "Estimated fuel cost in P (0 to cancel): ");
        if (fuelCost == null) {
            System.out.println("Trip cancelled.");
            return;
        }

        int tripId = tripDao.recordTrip(user.id(), boatId, destination, fuelCost);
        if (tripId < 0) {
            System.out.println("That boat is no longer available. Please try again.");
        } else {
            System.out.println("Trip #" + tripId + " recorded. Status: " + Trip.STATUS_ONGOING);
        }
    }

    private void viewMyTrips() {
        List<Trip> trips = tripDao.getTripsByCaptain(user.id());
        System.out.println();
        System.out.println("--- My Trips ---");
        if (trips.isEmpty()) {
            System.out.println("You have no trips yet.");
            return;
        }
        TableView.printTrips(trips);
    }

    private void updateTripStatus() {
        List<Trip> ongoing = tripDao.getTripsByCaptain(user.id()).stream()
                .filter(t -> Trip.STATUS_ONGOING.equalsIgnoreCase(t.status()))
                .toList();
        System.out.println();
        System.out.println("--- Update Trip Status ---");
        if (ongoing.isEmpty()) {
            System.out.println("You have no ongoing trips.");
            return;
        }
        TableView.printTrips(ongoing);

        int tripId;
        while (true) {
            tripId = InputHelper.readInt(scanner, "Enter the Trip ID to update (0 to cancel): ");
            if (tripId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            int chosen = tripId;
            if (ongoing.stream().anyMatch(t -> t.tripId() == chosen)) {
                break;
            }
            System.out.println("Trip ID " + tripId + " is not one of your ongoing trips. Try again.");
        }

        String newStatus = null;
        while (newStatus == null) {
            System.out.println("1. " + Trip.STATUS_COMPLETED);
            System.out.println("2. " + Trip.STATUS_CANCELLED);
            switch (InputHelper.readInt(scanner, "Choose the new status (0 to cancel): ")) {
                case 0 -> {
                    System.out.println("Update cancelled.");
                    return;
                }
                case 1 -> newStatus = Trip.STATUS_COMPLETED;
                case 2 -> newStatus = Trip.STATUS_CANCELLED;
                default -> System.out.println("Invalid choice. Please enter 1, 2 or 0.");
            }
        }

        if (tripDao.updateTripStatus(user.id(), tripId, newStatus)) {
            System.out.println("Trip #" + tripId + " is now " + newStatus + ".");
        } else {
            System.out.println("The trip could not be updated. It may no longer be ongoing.");
        }
    }
}