package model;

import gui.AdminDashboard;

/** Administrator user. Overrides openDashboard() to open the Administrator screens. */
public class Administrator extends User {

    public Administrator(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.ADMINISTRATOR, phone, email);
    }

    @Override
    public void openDashboard() {
        new AdminDashboard(this).setVisible(true);
    }
}
