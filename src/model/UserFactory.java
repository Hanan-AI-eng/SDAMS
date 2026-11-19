package model;

import util.FileHandler;

/**
 * STAND-IN for S1's UserFactory: builds the correct User subclass.
 * Factory design pattern: callers never write "new Instructor(...)" themselves.
 */
public final class UserFactory {

    private UserFactory() {
    }

    public static User create(Role role, String id, String name, String username,
                              String password, String phone, String email) {
        switch (role) {
            case MANAGER:            return new Manager(id, name, username, password, phone, email);
            case ADMINISTRATOR:      return new Administrator(id, name, username, password, phone, email);
            case INSTRUCTOR:         return new Instructor(id, name, username, password, phone, email);
            case STUDENT:            return new Student(id, name, username, password, phone, email);
            case FACILITIES_OFFICER: return new FacilitiesOfficer(id, name, username, password, phone, email);
            case ATTENDANCE_OFFICER: return new AttendanceOfficer(id, name, username, password, phone, email);
            default: throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    /** Rebuilds a user from one line of users.txt. */
    public static User fromFileLine(String line) {
        String[] f = FileHandler.split(line);
        if (f.length < 7) {
            throw new IllegalArgumentException("users.txt line needs 7 fields");
        }
        Role role = Role.valueOf(f[4].trim());
        return create(role, f[0].trim(), f[1], f[2], f[3], f[5], f[6]);
    }
}
