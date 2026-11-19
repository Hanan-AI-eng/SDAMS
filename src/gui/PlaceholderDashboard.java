package gui;

import model.User;

import javax.swing.JOptionPane;

/** Temporary screen for roles whose dashboards teammates are still building. */
public final class PlaceholderDashboard {

    private PlaceholderDashboard() {
    }

    public static void show(User user, String owner) {
        JOptionPane.showMessageDialog(null,
                "Welcome, " + user.getName() + " (" + user.getRole() + ").\n"
                        + "This dashboard is being built by " + owner + ".",
                "SDAMS", JOptionPane.INFORMATION_MESSAGE);
        new LoginFrame().setVisible(true);
    }
}
