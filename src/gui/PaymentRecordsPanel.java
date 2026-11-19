package gui;

import service.PaymentRecordService;
import service.PaymentRecordService.PaymentRow;
import util.Validator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Payment Records: view only. Filter by student, date range and status; see the total. */
public class PaymentRecordsPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final PaymentRecordService service;
    private List<PaymentRow> currentRows = new ArrayList<>();
    private String currentFilterSummary = "none";

    private final JTextField studentField = new JTextField(12);
    private final JTextField fromField = new JTextField(9);
    private final JTextField toField = new JTextField(9);
    private final JComboBox<String> statusField = new JComboBox<>(PaymentRecordService.STATUS_OPTIONS);

    private final DefaultTableModel model = UiStyle.readOnlyModel("Payment ID", "Student ID", "Student name",
            "Booking ID", "Class ID", "Amount (RM)", "Date", "Method", "Status");
    private final JTable table = UiStyle.table(model);
    private final JLabel summaryLabel = new JLabel();

    public PaymentRecordsPanel(String adminId) {
        this.service = new PaymentRecordService(adminId);
        setLayout(new BorderLayout(12, 12));
        setBackground(UiStyle.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildFilterBar(), BorderLayout.NORTH);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        add(new JScrollPane(table), BorderLayout.CENTER);

        summaryLabel.setFont(UiStyle.HEADING_FONT);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(summaryLabel);
        JLabel note = new JLabel("     View only: payments are created by students and cannot be edited here.");
        note.setForeground(UiStyle.MUTED);
        bottom.add(note);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildFilterBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(UiStyle.heading("Payment Records"), BorderLayout.NORTH);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        bar.add(new JLabel("Student ID / name:"));
        bar.add(studentField);
        bar.add(new JLabel("  From (yyyy-MM-dd):"));
        bar.add(fromField);
        bar.add(new JLabel("To:"));
        bar.add(toField);
        bar.add(new JLabel("  Status:"));
        bar.add(statusField);

        JButton filter = UiStyle.primaryButton("Filter");
        JButton reset = UiStyle.secondaryButton("Reset");
        JButton export = UiStyle.secondaryButton("Export to .txt");
        filter.addActionListener(e -> applyFilter());
        reset.addActionListener(e -> {
            studentField.setText("");
            fromField.setText("");
            toField.setText("");
            statusField.setSelectedIndex(0);
            applyFilter();
        });
        export.addActionListener(e -> export());
        studentField.addActionListener(e -> applyFilter());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(filter);
        buttons.add(reset);
        buttons.add(export);
        wrapper.add(bar, BorderLayout.CENTER);
        wrapper.add(buttons, BorderLayout.SOUTH);
        return wrapper;
    }

    @Override
    public void refresh() {
        applyFilter();
    }

    private void applyFilter() {
        LocalDate from;
        LocalDate to;
        try {
            from = readDate(fromField.getText(), "From");
            to = readDate(toField.getText(), "To");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        String status = (String) statusField.getSelectedItem();
        String student = studentField.getText();
        if (Validator.hasPipe(student)) {
            showError("Search text must not contain the | character.");
            return;
        }
        try {
            currentRows = service.filter(student, from, to, status);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        currentFilterSummary = "student=" + (student.trim().isEmpty() ? "any" : student.trim())
                + ", from=" + (from == null ? "any" : from)
                + ", to=" + (to == null ? "any" : to)
                + ", status=" + status;

        model.setRowCount(0);
        for (PaymentRow r : currentRows) {
            model.addRow(new Object[]{r.getPaymentId(), r.getStudentId(), r.getStudentName(),
                    r.getBookingId(), r.getClassId(), r.getAmount().toPlainString(),
                    r.getPaymentDate().toString(), r.getMethod(), r.getStatus()});
        }
        BigDecimal total = service.total(currentRows);
        summaryLabel.setText(currentRows.isEmpty()
                ? "No records match these filters.   Total: RM 0.00"
                : "Records: " + currentRows.size() + "   Total: RM " + total.toPlainString());
    }

    /** Blank means "no limit". Anything else must be a real yyyy-MM-dd date. */
    private LocalDate readDate(String text, String label) {
        if (Validator.isEmpty(text)) {
            return null;
        }
        LocalDate d = Validator.parseDate(text);
        if (d == null) {
            throw new IllegalArgumentException("\"" + label + "\" must be a real date in yyyy-MM-dd format.");
        }
        return d;
    }

    private void export() {
        if (currentRows.isEmpty()) {
            showError("There are no records to export.");
            return;
        }
        try {
            Path file = service.exportToFile(currentRows, currentFilterSummary);
            JOptionPane.showMessageDialog(this, "Exported " + currentRows.size() + " records to:\n"
                    + file.toAbsolutePath(), "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Could not write the export file: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Cannot continue", JOptionPane.ERROR_MESSAGE);
    }
}
