package gui;

import model.Booking;
import model.TrainingClass;
import service.StudentPortalService;
import service.StudentPortalService.ClassView;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Browse upcoming classes and book one (owner: S3). */
public class BookClassPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private final StudentPortalService service;
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> disciplineBox = new JComboBox<>();
    private final DefaultTableModel model = UiStyle.readOnlyModel(
            "Class ID", "Class", "Discipline", "Instructor", "Date", "Time", "Room", "Fee (RM)", "Seats left", "Status");
    private final JTable table = UiStyle.table(model);
    private final JLabel countLabel = StudentUi.muted(" ");
    private boolean loading;

    public BookClassPanel(StudentPortalService service) {
        this.service = service;
        StudentUi.page(this, "Book a Class",
                "Upcoming classes that are open for booking. You pay after booking, from the Payments page.");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(new JLabel("Search:"));
        bar.add(searchField);
        bar.add(new JLabel("Discipline:"));
        bar.add(disciplineBox);
        JButton book = UiStyle.primaryButton("Book selected class");
        book.addActionListener(e -> bookSelected());
        bar.add(book);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { reload(); }
            public void removeUpdate(DocumentEvent e) { reload(); }
            public void changedUpdate(DocumentEvent e) { reload(); }
        });
        disciplineBox.addActionListener(e -> reload());

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
        loading = true;
        Object keep = disciplineBox.getSelectedItem();
        disciplineBox.removeAllItems();
        disciplineBox.addItem("All");
        for (String d : service.availableDisciplines()) {
            disciplineBox.addItem(d);
        }
        disciplineBox.setSelectedItem(keep == null ? "All" : keep);
        loading = false;
        reload();
    }

    private void reload() {
        if (loading) {
            return;
        }
        model.setRowCount(0);
        String discipline = (String) disciplineBox.getSelectedItem();
        int count = 0;
        for (ClassView v : service.searchAvailableClasses(searchField.getText(), discipline)) {
            TrainingClass c = v.getTrainingClass();
            String status = v.isAlreadyBooked() ? "Booked by you" : (v.getSeatsLeft() == 0 ? "Full" : "Available");
            model.addRow(new Object[]{c.getClassId(), c.getClassName(), c.getDiscipline(), v.getInstructorName(),
                    c.getDate(), c.getStartTime() + " - " + c.getEndTime(), c.getRoom(),
                    String.format("%.2f", c.getFee()), v.getSeatsLeft(), status});
            count++;
        }
        countLabel.setText(count + " classes shown");
    }

    private void bookSelected() {
        String classId = StudentUi.selectedId(table);
        if (classId == null) {
            StudentUi.info(this, "Please select a class first.");
            return;
        }
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        String summary = model.getValueAt(row, 1) + " on " + model.getValueAt(row, 4) + " ("
                + model.getValueAt(row, 5) + "), fee RM " + model.getValueAt(row, 7);
        if (!StudentUi.confirm(this, "Book " + summary + "?")) {
            return;
        }
        try {
            Booking b = service.bookClass(classId);
            StudentUi.info(this, "Booked! Your booking ID is " + b.getId()
                    + ".\nPlease pay from the Payments page.");
            reload();
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
    }
}
