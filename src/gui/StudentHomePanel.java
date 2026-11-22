package gui;

import model.TrainingClass;
import service.StudentPortalService;
import service.StudentPortalService.BookingView;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;

/** Overview: upcoming classes, amount owed and attendance rate (owner: S3). */
public class StudentHomePanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final StudentPortalService service;
    private final JLabel upcomingValue = new JLabel();
    private final JLabel owedValue = new JLabel();
    private final JLabel attendanceValue = new JLabel();
    private final JLabel welcome = StudentUi.muted("");
    private final DefaultTableModel model = UiStyle.readOnlyModel(
            "Booking", "Class", "Date", "Time", "Room", "Instructor", "Payment");
    private final JTable table = UiStyle.table(model);

    public StudentHomePanel(StudentPortalService service) {
        this.service = service;
        StudentUi.page(this, "Welcome", null);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.setOpaque(false);
        cards.add(StudentUi.card("Upcoming classes", upcomingValue));
        cards.add(StudentUi.card("Amount to pay", owedValue));
        cards.add(StudentUi.card("Attendance rate", attendanceValue));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(welcome, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        center.add(top, BorderLayout.NORTH);

        JPanel list = new JPanel(new BorderLayout(0, 6));
        list.setOpaque(false);
        list.add(UiStyle.heading("My upcoming classes"), BorderLayout.NORTH);
        list.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(list, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        try {
            welcome.setText("Hello, " + service.getAccount().getName()
                    + ". Here is what is coming up for you.");
        } catch (RuntimeException e) {
            welcome.setText(e.getMessage());
        }
        model.setRowCount(0);
        int upcoming = 0;
        for (BookingView v : service.getMyBookings()) {
            TrainingClass c = v.getTrainingClass();
            if (c == null || !v.getBooking().getStatus().isActive()) {
                continue;
            }
            LocalDate d = util.Validator.parseDate(c.getDate());
            if (d == null || d.isBefore(LocalDate.now())) {
                continue;
            }
            upcoming++;
            model.addRow(new Object[]{v.getBooking().getId(), c.getClassName(), c.getDate(),
                    c.getStartTime() + " - " + c.getEndTime(), c.getRoom(), v.getInstructorName(),
                    v.getBooking().getPaymentStatus()});
        }
        upcomingValue.setText(String.valueOf(upcoming));
        owedValue.setText("RM " + service.outstandingBalance());
        attendanceValue.setText(service.attendanceRate() + "%");
    }
}
