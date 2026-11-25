package gui;

import service.RevenueReport;
import util.ReportExporter;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

/** Revenue Report: Monthly or Yearly, PAID payments only, with a total row and a bar chart. */
public class RevenueReportPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final RevenueReport report = new RevenueReport();

    private final JRadioButton monthly = new JRadioButton("Monthly", true);
    private final JRadioButton yearly = new JRadioButton("Yearly");
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JComboBox<Integer> yearBox = new JComboBox<>();

    private DefaultTableModel model = UiStyle.readOnlyModel("Report");
    private final JTable table = UiStyle.table(model);
    private final RevenueChart chart = new RevenueChart();
    private final JLabel summaryLabel = new JLabel(" ");

    public RevenueReportPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiStyle.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        for (int m = 1; m <= 12; m++) {
            monthBox.addItem(RevenueReport.monthName(m));
        }
        monthBox.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        reloadYears();

        ButtonGroup group = new ButtonGroup();
        group.add(monthly);
        group.add(yearly);
        monthly.setOpaque(false);
        yearly.setOpaque(false);
        monthly.addActionListener(e -> monthBox.setEnabled(true));
        yearly.addActionListener(e -> monthBox.setEnabled(false));

        JButton generate = UiStyle.primaryButton("Generate");
        generate.addActionListener(e -> generate());
        JButton export = UiStyle.secondaryButton("Export to .txt");
        export.addActionListener(e -> export());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(UiStyle.heading("Revenue Report"));
        bar.add(new JLabel("    "));
        bar.add(monthly);
        bar.add(yearly);
        bar.add(new JLabel("  Month:"));
        bar.add(monthBox);
        bar.add(new JLabel("  Year:"));
        bar.add(yearBox);
        bar.add(generate);
        bar.add(export);
        add(bar, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(false);   // keep the TOTAL row at the bottom
        table.setRowSorter(null);             // remove the sorter UiStyle.table() attached
        table.setPreferredScrollableViewportSize(new java.awt.Dimension(0, 13 * 24 + 4)); // 12 months + TOTAL
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), chart);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        summaryLabel.setFont(UiStyle.HEADING_FONT);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(summaryLabel);
        JLabel note = new JLabel("     Only PAID payments count (UNPAID and REFUNDED are excluded). "
                + "One payment = one class registration.");
        note.setForeground(UiStyle.MUTED);
        bottom.add(note);
        add(bottom, BorderLayout.SOUTH);

        generate();
    }

    @Override
    public void refresh() {
        reloadYears();
        generate();
    }

    private void reloadYears() {
        Object keep = yearBox.getSelectedItem();
        yearBox.removeAllItems();
        for (Integer y : RevenueReport.availableYears()) {
            yearBox.addItem(y);
        }
        if (keep != null) {
            yearBox.setSelectedItem(keep);
        }
    }

    private void generate() {
        Integer year = (Integer) yearBox.getSelectedItem();
        if (year == null) {
            JOptionPane.showMessageDialog(this, "Please choose a year.", "Cannot continue",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (monthly.isSelected()) {
            report.generateMonthly(year, monthBox.getSelectedIndex() + 1);
        } else {
            report.generateYearly(year);
        }
        showReport();
    }

    private void showReport() {
        model = UiStyle.readOnlyModel(report.getColumns());
        for (RevenueReport.Row r : report.getRows()) {
            model.addRow(r.getCells());
        }
        if (!report.hasRecords() && report.getPeriod() == RevenueReport.Period.MONTHLY) {
            String[] empty = new String[report.getColumns().length];
            empty[0] = "No records for this period";
            model.addRow(empty);
        }
        model.addRow(report.getTotalRow());
        table.setModel(model);

        // right-align the money column and bold the TOTAL row
        int moneyCol = report.getColumns().length - 1;
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                boolean totalRow = row == t.getRowCount() - 1;
                c.setFont(totalRow ? c.getFont().deriveFont(Font.BOLD) : c.getFont().deriveFont(Font.PLAIN));
                setHorizontalAlignment(col == moneyCol || (report.getPeriod() == RevenueReport.Period.YEARLY
                        && col == 1) ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        chart.setData(report.getPeriod() == RevenueReport.Period.YEARLY
                ? "Revenue per month (RM)" : "Revenue per class (RM)", report.chartData());

        summaryLabel.setText(report.getTitle() + ":   " + report.getTotalRegistrations()
                + " registration(s),   RM " + RevenueReport.money(report.getTotalRevenue())
                + (report.hasRecords() ? "" : "   (No records)"));
    }

    private void export() {
        try {
            Path file = ReportExporter.export(report);
            JOptionPane.showMessageDialog(this, "Report saved to:\n" + file.toAbsolutePath(),
                    "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the report: " + ex.getMessage(),
                    "Cannot continue", JOptionPane.ERROR_MESSAGE);
        }
    }
}
