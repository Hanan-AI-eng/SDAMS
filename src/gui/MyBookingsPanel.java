package gui;

import model.Booking;
import model.TrainingClass;
import service.StudentPortalService;
import service.StudentPortalService.BookingView;
import service.StudentPortalService.ClassView;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** View, cancel and reschedule own bookings (owner: S3). */
public class MyBookingsPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private static final String ALL = "All";

    private final StudentPortalService service;
    private final JComboBox<String> statusBox =
            new JComboBox<>(new String[]{ALL, "BOOKED", "RESCHEDULED", "CANCELLED", "ATTENDED"});
    private final DefaultTableModel model = UiStyle.readOnlyModel(
            "Booking", "Class ID", "Class", "Instructor", "Class date", "Time", "Booked on", "Status", "Payment");
    private final JTable table = UiStyle.table(model);
    private final JLabel countLabel = StudentUi.muted(" ");

    public MyBookingsPanel(StudentPortalService service) {
        this.service = service;
        StudentUi.page(this, "My Bookings",
                "You can cancel or reschedule a booking up to the day before the class.");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(new JLabel("Status:"));
        bar.add(statusBox);
        JButton reschedule = UiStyle.primaryButton("Reschedule");
        JButton cancel = UiStyle.dangerButton("Cancel booking");
        reschedule.addActionListener(e -> rescheduleSelected());
        cancel.addActionListener(e -> cancelSelected());
        bar.add(reschedule);
        bar.add(cancel);
        statusBox.addActionListener(e -> refresh());

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(bar, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(countLabel, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        String filter = (String) statusBox.getSelectedItem();
        int count = 0;
        for (BookingView v : service.getMyBookings()) {
            Booking b = v.getBooking();
            if (filter != null && !filter.equals(ALL) && !b.getStatus().name().equals(filter)) {
                continue;
            }
            TrainingClass c = v.getTrainingClass();
            model.addRow(new Object[]{b.getId(), b.getClassId(),
                    c == null ? "(class not found)" : c.getClassName(), v.getInstructorName(),
                    c == null ? "-" : c.getDate(), c == null ? "-" : c.getStartTime() + " - " + c.getEndTime(),
                    b.getBookingDate().toString(), b.getStatus(), b.getPaymentStatus()});
            count++;
        }
        countLabel.setText(count + " bookings shown");
    }

    private void cancelSelected() {
        String id = StudentUi.selectedId(table);
        if (id == null) {
            StudentUi.info(this, "Please select a booking first.");
            return;
        }
        if (!StudentUi.confirm(this, "Cancel booking " + id + "?\nA paid booking will be refunded.")) {
            return;
        }
        try {
            service.cancelBooking(id);
            StudentUi.info(this, "Booking " + id + " was cancelled.");
            refresh();
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
    }

    private void rescheduleSelected() {
        String id = StudentUi.selectedId(table);
        if (id == null) {
            StudentUi.info(this, "Please select a booking first.");
            return;
        }
        try {
            List<ClassView> options = service.rescheduleOptions(id);
            if (options.isEmpty()) {
                StudentUi.info(this, "There are no other upcoming classes of the same discipline with free seats.");
                return;
            }
            String[] labels = new String[options.size()];
            for (int i = 0; i < options.size(); i++) {
                TrainingClass c = options.get(i).getTrainingClass();
                labels[i] = c.getClassId() + "  |  " + c.getClassName() + "  |  " + c.getDate() + " "
                        + c.getStartTime() + "-" + c.getEndTime() + "  |  RM " + String.format("%.2f", c.getFee());
            }
            Object choice = JOptionPane.showInputDialog(this, "Move booking " + id + " to:", "Reschedule",
                    JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
            if (choice == null) {
                return;
            }
            String newClassId = choice.toString().split("\\s")[0];
            service.rescheduleBooking(id, newClassId);
            StudentUi.info(this, "Booking " + id + " was moved to " + newClassId + ".");
            refresh();
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
    }
}
