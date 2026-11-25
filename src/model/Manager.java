package model;

import gui.ManagerDashboard;

/**
 * Manager user (owner: S1). No feature creates a Manager: the account is
 * pre-loaded in users.txt (assumption).
 */
public class Manager extends User {

    public Manager(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.MANAGER, phone, email);
    }

    @Override
    public void openDashboard() {
        new ManagerDashboard(this).setVisible(true);
    }
}
