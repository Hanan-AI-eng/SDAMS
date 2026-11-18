package sdams.facilities.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import sdams.facilities.model.Equipment;
import sdams.facilities.model.StockSummary;
import sdams.facilities.service.StockReportService;
import sdams.facilities.storage.StorageException;

/**
 * Screen for Function C: Equipment Inventory Stock Reports (view and search only).
 * It has no Add/Update/Delete buttons. Data comes from the inventory through StockReportService.
 */
public class StockReportPanel extends JPanel implements Refreshable {

    private static final String[] COLUMNS =
            {"Equipment ID", "Name", "Category", "Quantity", "Min. Level", "Stock Status"};

    private final StockReportService service;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);
    private final JLabel typesLabel = new JLabel();
    private final JLabel unitsLabel = new JLabel();
    private final JLabel lowLabel = new JLabel();
    private final JLabel outLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");

    public StockReportPanel(StockReportService service) {
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(buildSummary(), BorderLayout.NORTH);
        top.add(buildSearchBar(), BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        refresh();
    }

    private JPanel buildSummary() {
        JPanel summary = new JPanel(new GridLayout(1, 4, 8, 8));
        summary.setBorder(BorderFactory.createTitledBorder("Stock summary (all equipment)"));
        summary.add(typesLabel);
        summary.add(unitsLabel);
        summary.add(lowLabel);
        summary.add(outLabel);
        return summary;
    }

    private JPanel buildSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        bar.add(new JLabel("Search (equipment ID, name or category):"));
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

    private void runSearch() {
        refresh();
        String keyword = searchField.getText().trim();
        if (tableModel.getRowCount() == 0 && !keyword.isEmpty()) {
            Messages.info(this, "No stock records found matching '" + keyword + "'.");
        }
    }

    /** Reloads from the inventory. Called when this tab is opened, so inventory changes always show. */
    @Override
    public void refresh() {
        try {
            List<Equipment> rows = service.getReport(searchField.getText());
            tableModel.setRowCount(0);
            for (Equipment e : rows) {
                tableModel.addRow(new Object[]{e.getId(), e.getName(), e.getCategory(),
                        e.getQuantity(), e.getMinimumStockLevel(), e.getStockStatus()});
            }
            StockSummary s = service.getSummary();
            typesLabel.setText("Equipment types: " + s.getItemTypes());
            unitsLabel.setText("Total units: " + s.getTotalUnits());
            lowLabel.setText("Low stock: " + s.getLowStockItems());
            outLabel.setText("Out of stock: " + s.getOutOfStockItems());
            statusLabel.setText(rows.size() + " of " + s.getItemTypes() + " equipment type(s) shown.");
        } catch (StorageException ex) {
            Messages.error(this, ex.getMessage());
        }
    }

    /** Colours the Stock Status text: orange for low stock, red for out of stock. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                if (Equipment.OUT_OF_STOCK.equals(value)) {
                    cell.setForeground(new Color(180, 0, 0));
                } else if (Equipment.LOW_STOCK.equals(value)) {
                    cell.setForeground(new Color(200, 110, 0));
                } else {
                    cell.setForeground(table.getForeground());
                }
            }
            return cell;
        }
    }
}
