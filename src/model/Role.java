package model;

/** The six user roles (owner: S1). The enum name is what goes into users.txt. */
public enum Role {
    MANAGER("M", "Manager"),
    ADMINISTRATOR("A", "Administrator"),
    INSTRUCTOR("I", "Instructor"),
    STUDENT("S", "Student"),
    FACILITIES_OFFICER("F", "Facilities Officer"),
    ATTENDANCE_OFFICER("T", "Attendance Officer");

    private final String idPrefix;
    private final String displayName;

    Role(String idPrefix, String displayName) {
        this.idPrefix = idPrefix;
        this.displayName = displayName;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;   // shown in combo boxes and tables
    }
}
