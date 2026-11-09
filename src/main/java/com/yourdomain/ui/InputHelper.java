package com.yourdomain.ui;

import java.util.Scanner;

/**
 * Small helper for reading console input safely, so typing letters or symbols
 * where a number is expected never crashes the application.
 */
final class InputHelper {

    private InputHelper() {
    }

    /** Keeps asking until a whole number is typed. */
    static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    /** Keeps asking until a number greater than zero is typed. Returns null if the person enters 0 to cancel. */
    static Double readPositiveDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            try {
                double value = Double.parseDouble(input);
                if (value > 0 && Double.isFinite(value)) {
                    return value;
                }
                System.out.println("The value must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    /** Keeps asking until some text is typed. Returns null if the person enters 0 to cancel. */
    static String readText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This cannot be empty.");
        }
    }
}