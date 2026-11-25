package model;

import gui.PlaceholderDashboard;

/** User subclass (owner: S1). S4 (Instructor) adds role-specific fields and makes openDashboard() open the real screen. */
public class Instructor extends User {

    public Instructor(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.INSTRUCTOR, phone, email);
    }

    @Override
    public void openDashboard() {
        PlaceholderDashboard.show(this, "S4 (Instructor)");
    }
}
