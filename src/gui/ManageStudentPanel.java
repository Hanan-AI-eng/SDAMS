package gui;

import model.StudentProfile;
import model.User;
import service.StudentService;
import service.StudentService.StudentEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manage Students: login details (users.txt) plus profile (student_profiles.txt). */
public class ManageStudentPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private static final String MISSING = "(missing)";

    private final StudentService service;
    private final Map<String, StudentEntry> shown = new HashMap<>();

    private final JTextField searchField = new JTextField(22);

    private final DefaultTableModel model = UiStyle.readOnlyModel(
            "ID", "Name", "Username", "Phone", "Email", "Discipline", "Belt", "Joined", "Emergency");
    private final JTable table = UiStyle.table(model);
    private final JLabel countLabel = new JLabel();

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel passwordHint = new JLabel(" ");
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JComboBox<String> disciplineField =
            new JComboBox<>(StudentProfile.DISCIPLINES.toArray(new String[0]));
    private final JComboBox<String> beltField =
            new JComboBox<>(StudentProfile.BELT_RANKS.toArray(new String[0]));
    private final JTextField joinedField = new JTextField();
    private final JTextField emergencyField = new JTextField();

    public ManageStudentPanel(String adminId) {
        this.service = new StudentService(adminId);
        setLayout(new BorderLayout(12, 12));
        setBackground(UiStyle.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildFilterBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.EAST);
        clearForm();
        refresh();
    }

    private JPanel buildFilterBar() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(UiStyle.heading("Manage Students"));
        bar.add(new JLabel("    Search (ID, name, username, discipline, belt):"));
        bar.add(searchField);
        return bar;
    }

    private JPanel buildTable() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelection();
            }
        });
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        countLabel.setForeground(UiStyle.MUTED);
        p.add(countLabel, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildForm() {
        FormPanel form = new FormPanel("Student details");
        idField.setEditable(false);
        form.addRow("Student ID", idField);
        form.addRow("Full name", nameField);
        form.addRow("Username", usernameField);
        form.addRow("Password", passwordField);
        passwordHint.setForeground(UiStyle.MUTED);
        passwordHint.setFont(UiStyle.BODY_FONT.deriveFont(11f));
        form.addWide(passwordHint);
        form.addRow("Phone", phoneField);
        form.addRow("Email", emailField);
        form.addRow("Discipline", disciplineField);
        form.addRow("Belt rank", beltField);
        form.addRow("Date joined", joinedField);
        JLabel dateHint = new JLabel("Format yyyy-MM-dd, not in the future.");
        dateHint.setForeground(UiStyle.MUTED);
        dateHint.setFont(UiStyle.BODY_FONT.deriveFont(11f));
        form.addWide(dateHint);
        form.addRow("Emergency contact", emergencyField);

        JButton add = UiStyle.primaryButton("Add");
        JButton update = UiStyle.primaryButton("Update");
        JButton delete = UiStyle.dangerButton("Delete");
        JButton clear = UiStyle.secondaryButton("Clear");
        add.addActionListener(e -> addStudent());
        update.addActionListener(e -> updateStudent());
        delete.addActionListener(e -> deleteStudent());
        clear.addActionListener(e -> {
            table.clearSelection();
            clearForm();
        });
        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        buttons.setOpaque(false);
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);
        form.addWide(buttons);
        form.finish();
        form.setPreferredSize(new Dimension(360, 0));
        return form;
    }

    @Override
    public void refresh() {
        String keepId = idField.getText();
        List<StudentEntry> students = service.search(searchField.getText());
        shown.clear();
        model.setRowCount(0);
        int missing = 0;
        for (StudentEntry e : students) {
            User u = e.getAccount();
            StudentProfile p = e.getProfile();
            shown.put(u.getId(), e);
            if (p == null) {
                missing++;
            }
            model.addRow(new Object[]{u.getId(), u.getName(), u.getUsername(), u.getPhone(), u.getEmail(),
                    p == null ? MISSING : p.getDiscipline(),
                    p == null ? MISSING : p.getBeltRank(),
                    p == null ? MISSING : p.getDateJoined().toString(),
                    p == null ? MISSING : p.getEmergencyContact()});
        }
        String text = students.size() + " students shown";
        if (missing > 0) {
            text += "   |   " + missing + " without a profile: select and Update to repair";
        }
        countLabel.setText(text);
        reselect(keepId);
    }

    private void reselect(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        for (int i = 0; i < table.getRowCount(); i++) {
            if (id.equals(table.getValueAt(i, 0))) {
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private void fillFormFromSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        String id = (String) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
        StudentEntry e = shown.get(id);
        if (e == null) {
            return;
        }
        User u = e.getAccount();
        idField.setText(u.getId());
        nameField.setText(u.getName());
        usernameField.setText(u.getUsername());
        passwordField.setText("");
        passwordHint.setText("Leave password blank to keep the current one.");
        phoneField.setText(u.getPhone());
        emailField.setText(u.getEmail());
        StudentProfile p = e.getProfile();
        if (p != null) {
            disciplineField.setSelectedItem(p.getDiscipline());
            beltField.setSelectedItem(p.getBeltRank());
            joinedField.setText(p.getDateJoined().toString());
            emergencyField.setText(p.getEmergencyContact());
        } else {
            disciplineField.setSelectedIndex(0);
            beltField.setSelectedIndex(0);
            joinedField.setText(LocalDate.now().toString());
            emergencyField.setText("");
        }
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        passwordHint.setText("At least 6 characters. ID is generated on Add.");
        phoneField.setText("");
        emailField.setText("");
        disciplineField.setSelectedIndex(0);
        beltField.setSelectedIndex(0);
        joinedField.setText(LocalDate.now().toString());
        emergencyField.setText("");
    }

    private void addStudent() {
        if (!idField.getText().isEmpty()) {
            showError("This form is showing an existing student.\nClick Clear first to add a new one.");
            return;
        }
        try {
            StudentEntry created = service.addStudent(nameField.getText(), usernameField.getText(),
                    new String(passwordField.getPassword()), phoneField.getText(), emailField.getText(),
                    (String) disciplineField.getSelectedItem(), (String) beltField.getSelectedItem(),
                    joinedField.getText(), emergencyField.getText());
            String id = created.getAccount().getId();
            clearForm();
            refresh();
            reselect(id);
            showInfo("Student " + created.getAccount().getName() + " added with ID " + id + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateStudent() {
        String id = idField.getText();
        if (id.isEmpty()) {
            showError("Please select a student in the table first.");
            return;
        }
        try {
            StudentEntry updated = service.updateStudent(id, nameField.getText(), usernameField.getText(),
                    new String(passwordField.getPassword()), phoneField.getText(), emailField.getText(),
                    (String) disciplineField.getSelectedItem(), (String) beltField.getSelectedItem(),
                    joinedField.getText(), emergencyField.getText());
            refresh();
            showInfo("Details for " + updated.getAccount().getName() + " (" + id + ") saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteStudent() {
        String id = idField.getText();
        if (id.isEmpty()) {
            showError("Please select a student in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete student " + nameField.getText() + " (" + id + ")?\n"
                        + "Their login and profile will both be removed. This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.deleteStudent(id);
            table.clearSelection();
            clearForm();
            refresh();
            showInfo("Student " + id + " deleted.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Cannot continue", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }
}
