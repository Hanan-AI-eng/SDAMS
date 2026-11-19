package model;

import gui.PlaceholderDashboard;

/** STAND-IN subclass from S1. S6 (Attendance Officer) replaces openDashboard() with the real screen. */
public class AttendanceOfficer extends User {

    public AttendanceOfficer(String id, String name, String username, String password, String phone, String email) {
        super(id, name, username, password, Role.ATTENDANCE_OFFICER, phone, email);
    }

    @Override
    public void openDashboard() {
        PlaceholderDashboard.show(this, "S6 (Attendance Officer)");
    }
}
