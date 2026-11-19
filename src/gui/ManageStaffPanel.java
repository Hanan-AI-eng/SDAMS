package gui;

import model.Role;
import model.User;
import service.StaffService;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manage Staff: Instructors, Facilities Officers and Attendance Officers. */
public class ManageStaffPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private static final String ALL_ROLES = "All staff roles";

    private final StaffService service;
    private final Map<String, User> shown = new HashMap<>();

    // filter bar
    private final JComboBox<Object> roleFilter = new JComboBox<>();
    private final JTextField searchField = new JTextField(20);

    // table
    private final DefaultTableModel model =
            UiStyle.readOnlyModel("ID", "Name", "Username", "Role", "Phone", "Email");
    private final JTable table = UiStyle.table(model);
    private final JLabel countLabel = new JLabel();

    // form
    private final JComboBox<Role> roleField =
            new JComboBox<>(StaffService.STAFF_ROLES.toArray(new Role[0]));
    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JLabel passwordHint = new JLabel(" ");

    public ManageStaffPanel(String adminId) {
        this.service = new StaffService(adminId);
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
        DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
        m.addElement(ALL_ROLES);
        for (Role r : StaffService.STAFF_ROLES) {
            m.addElement(r);
        }
        roleFilter.setModel(m);
        roleFilter.addActionListener(e -> refresh());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(UiStyle.heading("Manage Staff"));
        bar.add(new JLabel("    Role:"));
        bar.add(roleFilter);
        bar.add(new JLabel("  Search (ID, name, username):"));
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
        FormPanel form = new FormPanel("Staff details");
        idField.setEditable(false);
        form.addRow("Role", roleField);
        form.addRow("Staff ID", idField);
        form.addRow("Full name", nameField);
        form.addRow("Username", usernameField);
        form.addRow("Password", passwordField);
        passwordHint.setForeground(UiStyle.MUTED);
        passwordHint.setFont(UiStyle.BODY_FONT.deriveFont(11f));
        form.addWide(passwordHint);
        form.addRow("Phone", phoneField);
        form.addRow("Email", emailField);

        JButton add = UiStyle.primaryButton("Add");
        JButton update = UiStyle.primaryButton("Update");
        JButton delete = UiStyle.dangerButton("Delete");
        JButton clear = UiStyle.secondaryButton("Clear");
        add.addActionListener(e -> addStaff());
        update.addActionListener(e -> updateStaff());
        delete.addActionListener(e -> deleteStaff());
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
        form.setPreferredSize(new Dimension(340, 0));
        return form;
    }

    /** Reloads the table from users.txt using the current filters. */
    @Override
    public void refresh() {
        String keepId = idField.getText();
        Object selected = roleFilter.getSelectedItem();
        Role role = selected instanceof Role ? (Role) selected : null;
        List<User> staff = service.search(searchField.getText(), role);

        shown.clear();
        model.setRowCount(0);
        for (User u : staff) {
            shown.put(u.getId(), u);
            model.addRow(new Object[]{u.getId(), u.getName(), u.getUsername(),
                    u.getRole().getDisplayName(), u.getPhone(), u.getEmail()});
        }
        countLabel.setText(staff.size() + " staff shown");
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
        User u = shown.get(id);
        if (u == null) {
            return;
        }
        roleField.setSelectedItem(u.getRole());
        roleField.setEnabled(false);            // role cannot change after creation
        idField.setText(u.getId());
        nameField.setText(u.getName());
        usernameField.setText(u.getUsername());
        passwordField.setText("");
        passwordHint.setText("Leave password blank to keep the current one.");
        phoneField.setText(u.getPhone());
        emailField.setText(u.getEmail());
    }

    private void clearForm() {
        roleField.setEnabled(true);
        roleField.setSelectedIndex(0);
        idField.setText("");
        nameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        passwordHint.setText("At least 6 characters. ID is generated on Add.");
        phoneField.setText("");
        emailField.setText("");
    }

    private void addStaff() {
        if (!idField.getText().isEmpty()) {
            showError("This form is showing an existing staff member.\nClick Clear first to add a new one.");
            return;
        }
        try {
            User created = service.addStaff((Role) roleField.getSelectedItem(), nameField.getText(),
                    usernameField.getText(), new String(passwordField.getPassword()),
                    phoneField.getText(), emailField.getText());
            clearForm();
            refresh();
            reselect(created.getId());
            showInfo(created.getRole() + " " + created.getName() + " added with ID " + created.getId() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateStaff() {
        String id = idField.getText();
        if (id.isEmpty()) {
            showError("Please select a staff member in the table first.");
            return;
        }
        try {
            User updated = service.updateStaff(id, nameField.getText(), usernameField.getText(),
                    new String(passwordField.getPassword()), phoneField.getText(), emailField.getText());
            refresh();
            showInfo("Details for " + updated.getName() + " (" + id + ") saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteStaff() {
        String id = idField.getText();
        if (id.isEmpty()) {
            showError("Please select a staff member in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + nameField.getText() + " (" + id + ")?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.deleteStaff(id);
            table.clearSelection();
            clearForm();
            refresh();
            showInfo("Staff " + id + " deleted.");
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
