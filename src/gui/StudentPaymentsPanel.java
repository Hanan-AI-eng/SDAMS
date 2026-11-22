package gui;

import model.Payment;
import model.PaymentMethod;
import model.PaymentStatus;
import service.StudentPortalService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Payment history and paying for bookings (owner: S3). */
public class StudentPaymentsPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final StudentPortalService service;
    private final DefaultTableModel model = UiStyle.readOnlyModel(
            "Payment", "Booking", "Class ID", "Class", "Amount (RM)", "Date", "Method", "Status");
    private final JTable table = UiStyle.table(model);
    private final JComboBox<PaymentMethod> methodBox = new JComboBox<>(PaymentMethod.values());
    private final JLabel totals = StudentUi.muted(" ");

    public StudentPaymentsPanel(StudentPortalService service) {
        this.service = service;
        StudentUi.page(this, "Payments",
                "Select an UNPAID payment, choose a method and click Pay. For unpaid items the date is when it was raised.");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(new JLabel("Method:"));
        bar.add(methodBox);
        JButton pay = UiStyle.primaryButton("Pay selected");
        pay.addActionListener(e -> paySelected());
        bar.add(pay);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(bar, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        totals.setFont(UiStyle.BODY_FONT.deriveFont(java.awt.Font.BOLD));
        center.add(totals, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (Payment p : service.getMyPayments()) {
            model.addRow(new Object[]{p.getId(), p.getBookingId(), p.getClassId(), service.className(p.getClassId()),
                    p.getAmount().toPlainString(), p.getPaymentDate().toString(), methodLabel(p.getMethod()),
                    p.getStatus()});
        }
        totals.setText("Total paid: RM " + service.totalPaid() + "      Still to pay: RM "
                + service.outstandingBalance());
    }

    private static String methodLabel(String stored) {
        for (PaymentMethod m : PaymentMethod.values()) {
            if (m.name().equalsIgnoreCase(stored)) {
                return m.toString();
            }
        }
        return stored;
    }

    private void paySelected() {
        String id = StudentUi.selectedId(table);
        if (id == null) {
            StudentUi.info(this, "Please select a payment first.");
            return;
        }
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        if (model.getValueAt(row, 7) != PaymentStatus.UNPAID) {
            StudentUi.info(this, "This payment is already " + model.getValueAt(row, 7) + ".");
            return;
        }
        PaymentMethod method = (PaymentMethod) methodBox.getSelectedItem();
        if (!StudentUi.confirm(this, "Pay RM " + model.getValueAt(row, 4) + " for " + model.getValueAt(row, 3)
                + " by " + method + "?")) {
            return;
        }
        try {
            service.makePayment(id, method);
            StudentUi.info(this, "Payment " + id + " received. Thank you!");
            refresh();
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
    }
}
