package model;

import gui.PlaceholderDashboard;

/** STAND-IN subclass from S1. S5 (Facilities Officer) replaces openDashboard() with the real screen. */
public class FacilitiesOfficer extends User {

    public FacilitiesOfficer(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.FACILITIES_OFFICER, phone, email);
    }

    @Override
    public void openDashboard() {
        PlaceholderDashboard.show(this, "S5 (Facilities Officer)");
    }
}
