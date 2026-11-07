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
 * Menu for the Boat Captain: record trips and update their status.
 */
public class CaptainMenu {

    private static final String TRIP_ROW = "| %-5s | %-6s | %-20s | %-19s | %-19s | %-10s | %-10s |%n";
    private static final String TRIP_BORDER =
            "+-------+--------+----------------------+---------------------+---------------------+------------+------------+";

    private static final String BOAT_ROW = "| %-5s | %-20s | %-16s | %-12s | %-10s |%n";
    private static final String BOAT_BORDER =
            "+-------+----------------------+------------------+--------------+------------+";

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
        System.out.println("Welcome, " + user.username() + "! You are logged in as the " + user.roles() + ".");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("===== BOAT CAPTAIN MENU =====");
            System.out.println("1. View my profile");
            System.out.println("2. Record a new trip");
            System.out.println("3. Update trip status (Completed / Cancelled)");
            System.out.println("4. View my trips");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> ProfileView.print(user);
                case "2" -> recordTrip();
                case "3" -> updateTripStatus();
                case "4" -> viewMyTrips();
                case "5" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid option. Please enter 1 to 5.");
            }
        }
    }

    /** Records a new ONGOING trip. TripDao.recordTrip writes the TRIP_RECORDED audit log. */
    private void recordTrip() {
        System.out.println();
        System.out.println("--- Record a New Trip ---");
        List<Boat> available = boatDao.getAvailableBoats();
        if (available.isEmpty()) {
            System.out.println("No boats are available. Ask the administrator to register a boat.");
            return;
        }
        printBoatTable(available);

        int boatId;
        while (true) {
            boatId = readIntInput("Enter the Boat ID for this trip (0 to cancel): ");
            if (boatId == 0) {
                System.out.println("Trip cancelled.");
                return;
            }
            final int chosen = boatId;
            if (available.stream().anyMatch(b -> b.boatId() == chosen)) {
                break;
            }
            System.out.println("Boat ID " + boatId + " is not in the available list. Try again.");
        }

        String destination = readText("Destination (0 to cancel): ");
        if (destination == null) {
            System.out.println("Trip cancelled.");
            return;
        }
        double fuelCost = readNonNegativeDouble("Fuel cost (0 for none): ");

        int tripId = tripDao.recordTrip(user.id(), boatId, destination, fuelCost);
        if (tripId > 0) {
            System.out.println("Trip #" + tripId + " recorded and set to " + Trip.STATUS_ONGOING + ".");
        } else {
            System.out.println("Could not record the trip. The boat may no longer be available.");
        }
    }

    /** Changes an ONGOING trip to COMPLETED or CANCELLED. TripDao writes the TRIP_STATUS_UPDATED audit log. */
    private void updateTripStatus() {
        System.out.println();
        System.out.println("--- Update Trip Status ---");
        List<Trip> ongoing = tripDao.getOngoingTripsByCaptain(user.id());
        if (ongoing.isEmpty()) {
            System.out.println("You have no ONGOING trips to update.");
            return;
        }
        printTripTable(ongoing);

        int tripId;
        while (true) {
            tripId = readIntInput("Enter the Trip ID to update (0 to cancel): ");
            if (tripId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            final int chosen = tripId;
            if (ongoing.stream().anyMatch(t -> t.tripId() == chosen)) {
                break;
            }
            System.out.println("Trip ID " + tripId + " is not in your ONGOING list. Try again.");
        }

        String newStatus = null;
        while (newStatus == null) {
            System.out.println("1. COMPLETED");
            System.out.println("2. CANCELLED");
            System.out.print("Choose the new status (0 to cancel): ");
            switch (scanner.nextLine().trim()) {
                case "0" -> {
                    System.out.println("Update cancelled.");
                    return;
                }
                case "1" -> newStatus = Trip.STATUS_COMPLETED;
                case "2" -> newStatus = Trip.STATUS_CANCELLED;
                default -> System.out.println("Invalid option. Please enter 1, 2 or 0.");
            }
        }

        if (tripDao.updateTripStatus(user.id(), tripId, newStatus)) {
            System.out.println("Trip #" + tripId + " is now " + newStatus + ".");
        } else {
            System.out.println("Update failed. Only ONGOING trips can be updated.");
        }
    }

    private void viewMyTrips() {
        System.out.println();
        System.out.println("--- My Trips ---");
        List<Trip> trips = tripDao.getTripsByCaptain(user.id());
        if (trips.isEmpty()) {
            System.out.println("You have not recorded any trips yet.");
            return;
        }
        printTripTable(trips);
    }

    // ---- input validation ----

    /** Keeps asking until the person types a whole number, so letters or symbols never crash the app. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    /** Keeps asking until non-empty text is entered. Returns null if the person enters 0. */
    private String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    /** Keeps asking until a number that is 0 or more is entered. */
    private double readNonNegativeDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (!Double.isNaN(value) && !Double.isInfinite(value) && value >= 0) {
                    return value;
                }
                System.out.println("Please enter 0 or a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // ---- tables ----

    private void printBoatTable(List<Boat> boats) {
        System.out.println(BOAT_BORDER);
        System.out.printf(BOAT_ROW, "ID", "Name", "Registration No.", "Type", "Capacity");
        System.out.println(BOAT_BORDER);
        for (Boat b : boats) {
            System.out.printf(BOAT_ROW, b.boatId(), fit(b.boatName(), 20), fit(b.registrationNo(), 16),
                    fit(b.boatType(), 12), String.format("%.1f", b.capacity()));
        }
        System.out.println(BOAT_BORDER);
    }

    private void printTripTable(List<Trip> trips) {
        System.out.println(TRIP_BORDER);
        System.out.printf(TRIP_ROW, "ID", "Boat", "Destination", "Departure", "Return", "Fuel Cost", "Status");
        System.out.println(TRIP_BORDER);
        for (Trip t : trips) {
            System.out.printf(TRIP_ROW, t.tripId(), t.boatId(), fit(t.destination(), 20),
                    fit(t.departureDate(), 19), fit(t.returnDate(), 19),
                    String.format("%.2f", t.fuelCost()), fit(t.status(), 10));
        }
        System.out.println(TRIP_BORDER);
        System.out.println("Total: " + trips.size());
    }

    private static String fit(String text, int width) {
        if (text == null) {
            return "";
        }
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }
}