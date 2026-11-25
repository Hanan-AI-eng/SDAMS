package gui;

import model.Manager;

/** Manager dashboard: Manage Administrators, Staff Report, Revenue Report, My Account, Logout. */
public class ManagerDashboard extends BaseDashboardFrame {

    public ManagerDashboard(Manager manager) {
        super("Manager", manager);
        addSection("Manage Administrators", new ManageAdminPanel(manager.getId()));
        addSection("Staff Report", new StaffReportPanel());
        addSection("Revenue Report", new RevenueReportPanel());
        addSection("My Account", new ChangePasswordPanel(manager.getId()));
    }
}
