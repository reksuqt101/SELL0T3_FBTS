package com.yourdomain.ui;

import com.yourdomain.model.Boat;
import com.yourdomain.model.Trip;

import java.util.List;

/**
 * Aligned console tables for boats and trips, shared by the role menus.
 */
final class TableView {

    private static final String BOAT_ROW = "%-5s | %-18s | %-14s | %-12s | %-9s | %-10s%n";
    private static final String TRIP_ROW = "%-5s | %-7s | %-9s | %-18s | %-19s | %-11s%n";

    private TableView() {
    }

    static void printBoats(List<Boat> boats) {
        String line = "-".repeat(5 + 3 + 18 + 3 + 14 + 3 + 12 + 3 + 9 + 3 + 10);
        System.out.println(line);
        System.out.printf(BOAT_ROW, "ID", "Name", "Registration", "Type", "Capacity", "Status");
        System.out.println(line);
        for (Boat b : boats) {
            System.out.printf(BOAT_ROW, b.boatId(), fit(b.boatName(), 18), fit(b.registrationNo(), 14),
                    fit(b.boatType(), 12), b.capacity(), b.status());
        }
        System.out.println(line);
        System.out.println("Total: " + boats.size());
    }

    static void printTrips(List<Trip> trips) {
        String line = "-".repeat(5 + 3 + 7 + 3 + 9 + 3 + 18 + 3 + 19 + 3 + 11);
        System.out.println(line);
        System.out.printf(TRIP_ROW, "ID", "Boat ID", "Captain", "Destination", "Departed", "Status");
        System.out.println(line);
        for (Trip t : trips) {
            System.out.printf(TRIP_ROW, t.tripId(), t.boatId(), t.captainId(), fit(t.destination(), 18),
                    fit(t.departureDate(), 19), t.status());
        }
        System.out.println(line);
        System.out.println("Total: " + trips.size());
    }

    private static String fit(String text, int width) {
        if (text == null) {
            return "";
        }
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }
}