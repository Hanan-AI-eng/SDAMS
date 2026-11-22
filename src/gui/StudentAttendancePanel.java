package gui;

import service.StudentPortalService;
import service.StudentPortalService.AttendanceView;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

/** Read-only view of own attendance (owner: S3). Attendance is marked by the Attendance Officer. */
public class StudentAttendancePanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final StudentPortalService service;
    private final DefaultTableModel model = UiStyle.readOnlyModel("Record", "Class ID", "Class", "Date", "Status");
    private final JTable table = UiStyle.table(model);
    private final JLabel summary = StudentUi.muted(" ");

    public StudentAttendancePanel(StudentPortalService service) {
        this.service = service;
        StudentUi.page(this, "My Attendance", "Attendance is recorded by the Attendance Officer.");
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        summary.setFont(UiStyle.BODY_FONT.deriveFont(java.awt.Font.BOLD));
        center.add(summary, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        List<AttendanceView> rows = service.getMyAttendance();
        for (AttendanceView v : rows) {
            model.addRow(new Object[]{v.getRecord().getAttendanceId(), v.getRecord().getClassId(),
                    v.getClassName(), v.getRecord().getDate(), v.getRecord().getStatus()});
        }
        summary.setText(rows.size() + " records   |   Attendance rate: " + service.attendanceRate() + "%");
    }
}
