package model;

import java.io.IOException;
import service.AttendanceService;
import java.util.ArrayList;

public class AttendanceReport {

    private final AttendanceService service;

    public AttendanceReport() {
        service = new AttendanceService();
    }

    public String generateStudentReport(String studentId)
            throws IOException {

        ArrayList<AttendanceRecord> records =
                service.searchByStudentId(studentId);

        return createReport("Student ID", studentId, records);
    }

    public String generateClassReport(String classId)
            throws IOException {

        ArrayList<AttendanceRecord> records =
                service.searchByClassId(classId);

        return createReport("Class ID", classId, records);
    }

    private String createReport(
            String label,
            String value,
            ArrayList<AttendanceRecord> records) {

        int present = 0;
        int absent = 0;
        int late = 0;

        for (AttendanceRecord record : records) {
            switch (record.getStatus()) {
                case "PRESENT":
                    present++;
                    break;

                case "ABSENT":
                    absent++;
                    break;

                case "LATE":
                    late++;
                    break;
            }
        }

        return "===== Attendance Report =====\n"
                + label + ": " + value + "\n"
                + "Present: " + present + "\n"
                + "Absent: " + absent + "\n"
                + "Late: " + late + "\n"
                + "Total Records: " + records.size();
    }
}
