package gui;

import model.Administrator;

/** Administrator dashboard: Manage Staff, Manage Students, Payment Records, Logout. */
public class AdminDashboard extends BaseDashboardFrame {

    public AdminDashboard(Administrator admin) {
        super("Administrator", admin);
        addSection("Manage Staff", new ManageStaffPanel(admin.getId()));
        addSection("Manage Students", new ManageStudentPanel(admin.getId()));
        addSection("Payment Records", new PaymentRecordsPanel(admin.getId()));
    }
}
