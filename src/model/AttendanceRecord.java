package model;

public class AttendanceRecord {

    private String attendanceId;
    private String studentId;
    private String classId;
    private String date;
    private String status;

    public AttendanceRecord(String attendanceId, String studentId,
                            String classId, String date, String status) {

        if (isBlank(attendanceId) || isBlank(studentId)
                || isBlank(classId) || isBlank(date)) {
            throw new IllegalArgumentException(
                    "Attendance fields cannot be empty.");
        }

        this.attendanceId = attendanceId.trim();
        this.studentId = studentId.trim();
        this.classId = classId.trim();
        this.date = date.trim();

        setStatus(status);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String getAttendanceId() {
        return attendanceId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getClassId() {
        return classId;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status != null && (
                status.equalsIgnoreCase("PRESENT")
                || status.equalsIgnoreCase("ABSENT")
                || status.equalsIgnoreCase("LATE"))) {

            this.status = status.toUpperCase();

        } else {
            throw new IllegalArgumentException(
                    "Status must be PRESENT, ABSENT or LATE.");
        }
    }

    public String toFileString() {
        return attendanceId + "|" + studentId + "|" + classId
                + "|" + date + "|" + status;
    }
}