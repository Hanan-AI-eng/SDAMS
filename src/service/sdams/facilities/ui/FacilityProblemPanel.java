package sdams.facilities.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import sdams.facilities.model.FacilityProblem;
import sdams.facilities.service.FacilityProblemService;
import sdams.facilities.storage.StorageException;
import sdams.facilities.util.ValidationException;

/**
 * Screen for Function B: Facility Problem Management (view, search, update).
 * The reported details are read-only. Only Status and Remarks can be changed.
 * There is no Add or Delete button because the Instructor creates problems.
 */
public class FacilityProblemPanel extends JPanel implements Refreshable {

    private static final String[] COLUMNS = {"Problem ID", "Description", "Location",
            "Date Reported", "Reported By", "Status", "Remarks", "Last Updated"};

    private final FacilityProblemService service;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);

    private final JTextField idField = readOnlyField(8);
    private final JTextField descriptionField = readOnlyField(30);
    private final JTextField locationField = readOnlyField(12);
    private final JTextField reportedByField = readOnlyField(10);
    private final JTextField dateReportedField = readOnlyField(10);
    private final JComboBox<String> statusBox = new JComboBox<>(FacilityProblem.STATUSES);
    private final JTextField remarksField = new JTextField(30);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton saveButton = new JButton("Save Update");

    private String selectedId = null;

    public FacilityProblemPanel(FacilityProblemService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        add(buildSearchBar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowIntoForm();
            }
        });
        saveButton.addActionListener(e -> saveUpdate());
        refresh();
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        bar.add(new JLabel("Search (ID, description, location, status...):"));
        bar.add(searchField);
        bar.add(searchButton);
        bar.add(showAllButton);
        searchButton.addActionListener(e -> runSearch());
        searchField.addActionListener(e -> runSearch());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            refresh();
        });
        return bar;
    }

    private JPanel buildBottom() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Selected problem (grey fields were reported by the Instructor and cannot be changed)"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        addField(form, c, 0, 0, 1, "Problem ID:", idField);
        addField(form, c, 2, 0, 1, "Date reported:", dateReportedField);
        addField(form, c, 0, 1, 1, "Location:", locationField);
        addField(form, c, 2, 1, 1, "Reported by:", reportedByField);
        addField(form, c, 0, 2, 3, "Description:", descriptionField);
        addField(form, c, 0, 3, 1, "New status:", statusBox);
        addField(form, c, 2, 3, 1, "Remarks:", remarksField);

        c.gridx = 3;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        form.add(saveButton, c);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(form, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        return bottom;
    }

    /** Puts a label at column x and its field at column x+1 (the field may span several columns). */
    private void addField(JPanel form, GridBagConstraints c, int x, int y, int fieldSpan,
                          String label, java.awt.Component field) {
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(label), c);
        c.gridx = x + 1;
        c.gridwidth = fieldSpan;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    private static JTextField readOnlyField(int columns) {
        JTextField field = new JTextField(columns);
        field.setEditable(false);
        return field;
    }

    private void saveUpdate() {
        if (selectedId == null) {
            Messages.warn(this, "Please click a problem in the table first.");
            return;
        }
        try {
            FacilityProblem changed = service.update(selectedId,
                    String.valueOf(statusBox.getSelectedItem()), remarksField.getText());
            refresh();
            statusLabel.setText("Updated " + changed.getProblemId() + " to status '"
                    + changed.getStatus() + "'.");
            Messages.info(this, "Problem " + changed.getProblemId() + " updated.");
        } catch (ValidationException ex) {
            Messages.warn(this, ex.getMessage());
        } catch (StorageException ex) {
            Messages.error(this, ex.getMessage());
        }
    }

    private void runSearch() {
        refresh();
        String keyword = searchField.getText().trim();
        if (tableModel.getRowCount() == 0 && !keyword.isEmpty()) {
            Messages.info(this, "No facility problems found matching '" + keyword + "'.");
        }
    }

    @Override
    public void refresh() {
        try {
            List<FacilityProblem> rows = service.search(searchField.getText());
            tableModel.setRowCount(0);
            for (FacilityProblem p : rows) {
                tableModel.addRow(new Object[]{p.getProblemId(), p.getDescription(), p.getLocation(),
                        p.getDateReported(), p.getReportedBy(), p.getStatus(), p.getRemarks(),
                        p.getDateUpdated()});
            }
            clearDetails();
            String text = rows.size() + " problem(s) shown.";
            if (service.getSkippedLineCount() > 0) {
                text += "  WARNING: " + service.getSkippedLineCount()
                        + " damaged line(s) in facility_problems.txt were ignored.";
            }
            statusLabel.setText(text);
        } catch (StorageException ex) {
            Messages.error(this, ex.getMessage());
        }
    }

    private void loadSelectedRowIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        selectedId = String.valueOf(table.getValueAt(row, 0));
        idField.setText(selectedId);
        descriptionField.setText(String.valueOf(table.getValueAt(row, 1)));
        locationField.setText(String.valueOf(table.getValueAt(row, 2)));
        dateReportedField.setText(String.valueOf(table.getValueAt(row, 3)));
        reportedByField.setText(String.valueOf(table.getValueAt(row, 4)));
        statusBox.setSelectedItem(String.valueOf(table.getValueAt(row, 5)));
        remarksField.setText(String.valueOf(table.getValueAt(row, 6)));
    }

    private void clearDetails() {
        selectedId = null;
        idField.setText("");
        descriptionField.setText("");
        locationField.setText("");
        dateReportedField.setText("");
        reportedByField.setText("");
        remarksField.setText("");
        statusBox.setSelectedIndex(0);
    }
}
