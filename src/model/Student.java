package model;

import gui.PlaceholderDashboard;

/** STAND-IN subclass from S1. S3 (Student) replaces openDashboard() with the real screen. */
public class Student extends User {

    public Student(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.STUDENT, phone, email);
    }

    @Override
    public void openDashboard() {
        PlaceholderDashboard.show(this, "S3 (Student)");
    }
}
