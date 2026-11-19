package model;

import gui.PlaceholderDashboard;

/** STAND-IN subclass from S1. S1 (Manager) replaces openDashboard() with the real screen. */
public class Manager extends User {

    public Manager(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.MANAGER, phone, email);
    }

    @Override
    public void openDashboard() {
        PlaceholderDashboard.show(this, "S1 (Manager)");
    }
}
