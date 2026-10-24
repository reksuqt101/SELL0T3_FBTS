package com.yourdomain.ui;

import com.yourdomain.model.User;

/**
 * Small helper shared by the role menus to show a profile.
 */
final class ProfileView {

    private ProfileView() {
    }

    static void print(User user) {
        System.out.println("ID:       " + user.id());
        System.out.println("Username: " + user.username());
        System.out.println("Role:     " + user.roles());
        System.out.println("Status:   " + user.status());
    }
}
