package gui;

import model.User;
import service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Manage Administrators: add, view, search, update, delete (passwords are never shown). */
public class ManageAdminPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final UserService service;
    private final Map<String, User> shown = new HashMap<>();

    private final JTextField searchField = new JTextField(22);
    private final DefaultTableModel model = UiStyle.readOnlyModel("ID", "Name", "Username", "Phone", "Email");
    private final JTable table = UiStyle.table(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JLabel countLabel = new JLabel();

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel passwordHint = new JLabel(" ");
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();

    public ManageAdminPanel(String managerId) {
        this.service = new UserService(managerId);
        setLayout(new BorderLayout(12, 12));
        setBackground(UiStyle.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildSearchBar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.EAST);
        clearForm();
        refresh();
    }

    private JPanel buildSearchBar() {
        // Filters the table as the user types (TableRowSorter + RowFilter on ID and Name columns).
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applySearch(); }
            public void removeUpdate(DocumentEvent e) { applySearch(); }
            public void changedUpdate(DocumentEvent e) { applySearch(); }
        });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(UiStyle.heading("Manage Administrators"));
        bar.add(new JLabel("    Search (ID or name):"));
        bar.add(searchField);
        return bar;
    }

    private JPanel buildTable() {
        table.setRowSorter(sorter);
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
        FormPanel form = new FormPanel("Administrator details");
        idField.setEditable(false);
        form.addRow("Admin ID", idField);
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
        add.addActionListener(e -> addAdmin());
        update.addActionListener(e -> updateAdmin());
        delete.addActionListener(e -> deleteAdmin());
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

    @Override
    public void refresh() {
        String keepId = idField.getText();
        List<User> admins = service.getAdministrators();
        shown.clear();
        model.setRowCount(0);
        for (User u : admins) {
            shown.put(u.getId(), u);
            model.addRow(new Object[]{u.getId(), u.getName(), u.getUsername(), u.getPhone(), u.getEmail()});
        }
        applySearch();
        reselect(keepId);
    }

    private void applySearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // (?i) = ignore case; Pattern.quote so characters like "." are matched literally
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 1));
        }
        countLabel.setText(table.getRowCount() + " of " + model.getRowCount() + " administrators shown");
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
        User u = shown.get((String) model.getValueAt(table.convertRowIndexToModel(viewRow), 0));
        if (u == null) {
            return;
        }
        idField.setText(u.getId());
        nameField.setText(u.getName());
        usernameField.setText(u.getUsername());
        passwordField.setText("");
        passwordHint.setText("Leave password blank to keep the current one.");
        phoneField.setText(u.getPhone());
        emailField.setText(u.getEmail());
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        passwordHint.setText("At least 6 characters. ID is generated on Add.");
        phoneField.setText("");
        emailField.setText("");
    }

    private void addAdmin() {
        if (!idField.getText().isEmpty()) {
            showError("This form is showing an existing administrator.\nClick Clear first to add a new one.");
            return;
        }
        try {
            User created = service.addAdministrator(nameField.getText(), usernameField.getText(),
                    new String(passwordField.getPassword()), phoneField.getText(), emailField.getText());
            clearForm();
            refresh();
            reselect(created.getId());
            showInfo("Administrator " + created.getName() + " added with ID " + created.getId() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateAdmin() {
        String id = idField.getText();
        if (id.isEmpty()) {
            showError("Please select an administrator in the table first.");
            return;
        }
        try {
            User updated = service.updateAdministrator(id, nameField.getText(), usernameField.getText(),
                    new String(passwordField.getPassword()), phoneField.getText(), emailField.getText());
            refresh();
            showInfo("Details for " + updated.getName() + " (" + id + ") saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteAdmin() {
        String id = idField.getText();
        if (id.isEmpty()) {
            showError("Please select an administrator in the table first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete administrator " + nameField.getText() + " (" + id + ")?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.deleteAdministrator(id);
            table.clearSelection();
            clearForm();
            refresh();
            showInfo("Administrator " + id + " deleted.");
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
