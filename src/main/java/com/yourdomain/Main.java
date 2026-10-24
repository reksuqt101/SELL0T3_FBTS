package com.yourdomain;

import com.yourdomain.dao.DatabaseInitializer;
import com.yourdomain.dao.UserDao;
import com.yourdomain.ui.Authentication;

import java.util.Scanner;

/**
 * Startup entry point only: initialize database, create shared objects,
 * then hand control to Authentication.
 */
public class Main {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        try (Scanner scanner = new Scanner(System.in)) {
            UserDao userDao = new UserDao();
            new Authentication(scanner, userDao).start();
        }
    }
}
