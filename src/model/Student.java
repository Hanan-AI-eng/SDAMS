package model;

import gui.StudentDashboard;

/** Student user (subclass from S1). openDashboard() opens the S3 Student dashboard. */
public class Student extends User {

    public Student(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.STUDENT, phone, email);
    }

    @Override
    public void openDashboard() {
        new StudentDashboard(this).setVisible(true);
    }
}
