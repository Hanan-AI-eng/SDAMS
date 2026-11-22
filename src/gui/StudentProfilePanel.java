package gui;

import model.StudentProfile;
import model.User;
import service.StudentPortalService;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.util.Optional;

/** View and update own details, and change password (owner: S3). */
public class StudentProfilePanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final StudentPortalService service;

    private final JTextField idField = readOnly();
    private final JTextField usernameField = readOnly();
    private final JTextField nameField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField disciplineField = readOnly();
    private final JTextField beltField = readOnly();
    private final JTextField joinedField = readOnly();
    private final JTextField emergencyField = new JTextField();

    private final JPasswordField currentPw = new JPasswordField();
    private final JPasswordField newPw = new JPasswordField();
    private final JPasswordField confirmPw = new JPasswordField();

    public StudentProfilePanel(StudentPortalService service) {
        this.service = service;
        StudentUi.page(this, "My Profile",
                "Grey fields are managed by the academy. Ask the Administrator to change them.");

        FormPanel details = new FormPanel("My details");
        details.addRow("Student ID", idField);
        details.addRow("Username", usernameField);
        details.addRow("Full name", nameField);
        details.addRow("Phone", phoneField);
        details.addRow("Email", emailField);
        details.addRow("Discipline", disciplineField);
        details.addRow("Belt rank", beltField);
        details.addRow("Date joined", joinedField);
        details.addRow("Emergency contact", emergencyField);
        JButton save = UiStyle.primaryButton("Save changes");
        JButton reset = UiStyle.secondaryButton("Undo changes");
        save.addActionListener(e -> saveDetails());
        reset.addActionListener(e -> refresh());
        JPanel b1 = new JPanel(new GridLayout(1, 2, 6, 0));
        b1.setOpaque(false);
        b1.add(save);
        b1.add(reset);
        details.addWide(b1);
        details.finish();

        FormPanel password = new FormPanel("Change password");
        password.addRow("Current password", currentPw);
        password.addRow("New password", newPw);
        password.addRow("Confirm new password", confirmPw);
        password.addWide(StudentUi.muted("At least 6 characters, no spaces."));
        JButton change = UiStyle.primaryButton("Change password");
        change.addActionListener(e -> changePassword());
        password.addWide(change);
        password.finish();

        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
        grid.setOpaque(false);
        grid.add(details);
        grid.add(password);
        add(grid, java.awt.BorderLayout.CENTER);
        refresh();
    }

    private static JTextField readOnly() {
        JTextField f = new JTextField();
        f.setEditable(false);
        f.setBackground(new java.awt.Color(0xEEEEEE));
        return f;
    }

    @Override
    public void refresh() {
        try {
            User u = service.getAccount();
            idField.setText(u.getId());
            usernameField.setText(u.getUsername());
            nameField.setText(u.getName());
            phoneField.setText(u.getPhone());
            emailField.setText(u.getEmail());
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
        Optional<StudentProfile> p = service.getProfile();
        disciplineField.setText(p.map(StudentProfile::getDiscipline).orElse("(not set)"));
        beltField.setText(p.map(StudentProfile::getBeltRank).orElse("(not set)"));
        joinedField.setText(p.map(x -> x.getDateJoined().toString()).orElse("(not set)"));
        emergencyField.setText(p.map(StudentProfile::getEmergencyContact).orElse(""));
        emergencyField.setEnabled(p.isPresent());
    }

    private void saveDetails() {
        try {
            service.updateProfile(nameField.getText(), phoneField.getText(), emailField.getText(),
                    emergencyField.getText());
            StudentUi.info(this, "Your details were saved.");
            refresh();
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
    }

    private void changePassword() {
        try {
            service.changePassword(new String(currentPw.getPassword()), new String(newPw.getPassword()),
                    new String(confirmPw.getPassword()));
            currentPw.setText("");
            newPw.setText("");
            confirmPw.setText("");
            StudentUi.info(this, "Your password was changed.");
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
    }
}
