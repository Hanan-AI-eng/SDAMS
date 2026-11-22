package gui;

import model.User;
import service.StudentPortalService;

/**
 * Student dashboard (owner: S3, Student). One menu button per student feature.
 * All panels share one StudentPortalService, which holds every business rule.
 */
public class StudentDashboard extends BaseDashboardFrame {

    public StudentDashboard(User student) {
        super("Student", student);
        StudentPortalService service = new StudentPortalService(student.getId());
        addSection("Home", new StudentHomePanel(service));
        addSection("My Profile", new StudentProfilePanel(service));
        addSection("Book a Class", new BookClassPanel(service));
        addSection("My Bookings", new MyBookingsPanel(service));
        addSection("Payments", new StudentPaymentsPanel(service));
        addSection("Attendance", new StudentAttendancePanel(service));
        addSection("Rate a Class", new RateClassPanel(service));
    }
}
