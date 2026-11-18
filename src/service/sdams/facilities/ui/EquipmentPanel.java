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
import sdams.facilities.model.Equipment;
import sdams.facilities.service.EquipmentService;
import sdams.facilities.storage.StorageException;
import sdams.facilities.util.ValidationException;

/**
 * Screen for Function A: Equipment Inventory Management (add, view, search, update, delete).
 * "extends JPanel" means this class IS a panel (inheritance) with extra behaviour added.
 */
public class EquipmentPanel extends JPanel implements Refreshable {

    private static final String[] COLUMNS =
            {"ID", "Name", "Category", "Quantity", "Min. Level", "Condition", "Stock Status"};

    private final EquipmentService service;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);

    private final JTextField nameField = new JTextField(16);
    private final JComboBox<String> categoryBox = new JComboBox<>(Equipment.CATEGORIES);
    private final JTextField quantityField = new JTextField(6);
    private final JTextField minimumField = new JTextField(6);
    private final JComboBox<String> conditionBox = new JComboBox<>(Equipment.CONDITIONS);
    private final JLabel selectedIdLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final JButton addButton = new JButton("Add");
    private final JButton updateButton = new JButton("Update Selected");
    private final JButton deleteButton = new JButton("Delete Selected");
    private final JButton clearButton = new JButton("Clear Form");

    /** The ID of the row the user clicked, or null if nothing is selected. */
    private String selectedId = null;

    public EquipmentPanel(EquipmentService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // A table model whose cells cannot be edited directly; numbers sort as numbers.
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return (column == 3 || column == 4) ? Integer.class : String.class;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true); // click a column header to sort

        categoryBox.setEditable(true); // allows a category that is not in the list
        conditionBox.setSelectedIndex(1); // start on "Good"

        add(buildSearchBar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        connectEvents();
        resetSelection();
        refresh();
    }

    // ---------- building the screen ----------

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        bar.add(new JLabel("Search (ID, name or category):"));
        bar.add(searchField);
        bar.add(searchButton);
        bar.add(showAllButton);

        searchButton.addActionListener(e -> runSearch());
        searchField.addActionListener(e -> runSearch()); // pressing Enter also searches
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            refresh();
        });
        return bar;
    }

    private JPanel buildBottom() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Equipment details"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        addField(form, c, 0, 0, "Name:", nameField);
        addField(form, c, 2, 0, "Category:", categoryBox);
        addField(form, c, 4, 0, "Condition:", conditionBox);
        addField(form, c, 0, 1, "Quantity:", quantityField);
        addField(form, c, 2, 1, "Minimum stock level:", minimumField);
        c.gridx = 4;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(selectedIdLabel, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 6;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(buttons, c);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(form, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        return bottom;
    }

    private void addField(JPanel form, GridBagConstraints c, int x, int y, String label,
                          java.awt.Component field) {
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel(label), c);
        c.gridx = x + 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    private void connectEvents() {
        // Clicking a table row copies that record into the form.
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowIntoForm();
            }
        });
        addButton.addActionListener(e -> addEquipment());
        updateButton.addActionListener(e -> updateEquipment());
        deleteButton.addActionListener(e -> deleteEquipment());
        clearButton.addActionListener(e -> {
            table.clearSelection();
            clearForm();
        });
    }

    // ---------- actions (each one: read input -> call service -> show result -> reload table) ----------

    private void addEquipment() {
        try {
            Equipment created = service.add(nameField.getText(), comboText(categoryBox),
                    quantityField.getText(), minimumField.getText(), comboText(conditionBox));
            searchField.setText(""); // show everything so the new row is visible
            clearForm();
            refresh();
            statusLabel.setText("Added " + created.getId() + " - " + created.getName() + ".");
            Messages.info(this, "Equipment added with ID " + created.getId() + ".");
        } catch (ValidationException ex) {
            Messages.warn(this, ex.getMessage());
        } catch (StorageException ex) {
            Messages.error(this, ex.getMessage());
        }
    }

    private void updateEquipment() {
        if (selectedId == null) {
            Messages.warn(this, "Please click a row in the table first, then change the details and press Update.");
            return;
        }
        try {
            Equipment changed = service.update(selectedId, nameField.getText(), comboText(categoryBox),
                    quantityField.getText(), minimumField.getText(), comboText(conditionBox));
            clearForm();
            refresh();
            statusLabel.setText("Updated " + changed.getId() + " - " + changed.getName() + ".");
            Messages.info(this, "Equipment " + changed.getId() + " updated.");
        } catch (ValidationException ex) {
            Messages.warn(this, ex.getMessage());
        } catch (StorageException ex) {
            Messages.error(this, ex.getMessage());
        }
    }

    private void deleteEquipment() {
        if (selectedId == null) {
            Messages.warn(this, "Please click the row you want to delete first.");
            return;
        }
        if (!Messages.confirm(this, "Delete " + selectedId + " (" + nameField.getText()
                + ")?\nThis cannot be undone.")) {
            return;
        }
        try {
            String deletedId = selectedId;
            service.delete(deletedId);
            clearForm();
            refresh();
            statusLabel.setText("Deleted " + deletedId + ".");
            Messages.info(this, "Equipment " + deletedId + " deleted.");
        } catch (ValidationException ex) {
            Messages.warn(this, ex.getMessage());
            refresh();
        } catch (StorageException ex) {
            Messages.error(this, ex.getMessage());
        }
    }

    private void runSearch() {
        refresh();
        String keyword = searchField.getText().trim();
        if (tableModel.getRowCount() == 0 && !keyword.isEmpty()) {
            Messages.info(this, "No equipment found matching '" + keyword + "'.");
        }
    }

    // ---------- keeping the table and form in step with the file ----------

    /** Reloads the table from the text file using the current search text. */
    @Override
    public void refresh() {
        try {
            List<Equipment> rows = service.search(searchField.getText());
            tableModel.setRowCount(0);
            for (Equipment e : rows) {
                tableModel.addRow(new Object[]{e.getId(), e.getName(), e.getCategory(),
                        e.getQuantity(), e.getMinimumStockLevel(), e.getCondition(), e.getStockStatus()});
            }
            resetSelection();
            String text = rows.size() + " record(s) shown.";
            if (service.getSkippedLineCount() > 0) {
                text += "  WARNING: " + service.getSkippedLineCount()
                        + " damaged line(s) in equipment.txt were ignored.";
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
        // getValueAt uses the row as displayed, so it stays correct even when the table is sorted.
        selectedId = String.valueOf(table.getValueAt(row, 0));
        nameField.setText(String.valueOf(table.getValueAt(row, 1)));
        categoryBox.setSelectedItem(String.valueOf(table.getValueAt(row, 2)));
        quantityField.setText(String.valueOf(table.getValueAt(row, 3)));
        minimumField.setText(String.valueOf(table.getValueAt(row, 4)));
        conditionBox.setSelectedItem(String.valueOf(table.getValueAt(row, 5)));
        selectedIdLabel.setText("Selected ID: " + selectedId);
    }

    private void resetSelection() {
        selectedId = null;
        selectedIdLabel.setText("New item (ID is generated automatically)");
    }

    private void clearForm() {
        nameField.setText("");
        categoryBox.setSelectedIndex(0);
        quantityField.setText("");
        minimumField.setText("");
        conditionBox.setSelectedIndex(1); // "Good"
        resetSelection();
    }

    private String comboText(JComboBox<String> box) {
        Object item = box.getSelectedItem();
        return item == null ? "" : item.toString();
    }
}
