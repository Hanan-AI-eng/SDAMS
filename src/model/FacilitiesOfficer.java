package model;

import gui.PlaceholderDashboard;

/** User subclass (owner: S1). S5 (Facilities Officer) adds role-specific fields and makes openDashboard() open the real screen. */
public class FacilitiesOfficer extends User {

    public FacilitiesOfficer(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.FACILITIES_OFFICER, phone, email);
    }

    @Override
    public void openDashboard() {
        PlaceholderDashboard.show(this, "S5 (Facilities Officer)");
    }
}
