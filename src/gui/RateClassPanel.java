package gui;

import model.Rating;
import model.TrainingClass;
import service.StudentPortalService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Rate classes the student has taken and see past ratings (owner: S3). */
public class RateClassPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private static final String[] STAR_LABELS = {
            "5 - Excellent", "4 - Good", "3 - Average", "2 - Poor", "1 - Very poor"};

    private final StudentPortalService service;
    private final JComboBox<String> classBox = new JComboBox<>();
    private final JComboBox<String> starsBox = new JComboBox<>(STAR_LABELS);
    private final JTextArea commentArea = new JTextArea(5, 20);
    private final DefaultTableModel model = UiStyle.readOnlyModel("Rating", "Class", "Stars", "Comment", "Date");
    private final JTable table = UiStyle.table(model);

    public RateClassPanel(StudentPortalService service) {
        this.service = service;
        StudentUi.page(this, "Rate a Class", "You can rate each class you have taken once.");

        FormPanel form = new FormPanel("New rating");
        form.addRow("Class", classBox);
        form.addRow("Rating", starsBox);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        form.addRow("Comment", new JScrollPane(commentArea));
        form.addWide(StudentUi.muted("Optional, up to " + Rating.MAX_COMMENT + " characters."));
        JButton submit = UiStyle.primaryButton("Submit rating");
        submit.addActionListener(e -> submit());
        form.addWide(submit);
        form.finish();
        form.setPreferredSize(new Dimension(360, 0));

        JPanel history = new JPanel(new BorderLayout(0, 6));
        history.setOpaque(false);
        history.add(UiStyle.heading("My ratings"), BorderLayout.NORTH);
        history.add(new JScrollPane(table), BorderLayout.CENTER);

        add(history, BorderLayout.CENTER);
        add(form, BorderLayout.EAST);
        refresh();
    }

    @Override
    public void refresh() {
        classBox.removeAllItems();
        for (TrainingClass c : service.classesToRate()) {
            classBox.addItem(c.getClassId() + " - " + c.getClassName() + " (" + c.getDate() + ")");
        }
        model.setRowCount(0);
        for (Rating r : service.getMyRatings()) {
            model.addRow(new Object[]{r.getId(), r.getClassId() + " - " + service.className(r.getClassId()),
                    stars(r.getStars()), r.getComment(), r.getRatingDate().toString()});
        }
    }

    private static String stars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Rating.MAX_STARS; i++) {
            sb.append(i < n ? '★' : '☆');
        }
        return sb.toString();
    }

    private void submit() {
        Object selected = classBox.getSelectedItem();
        if (selected == null) {
            StudentUi.info(this, "There are no classes waiting for a rating.");
            return;
        }
        String classId = selected.toString().split(" - ")[0].trim();
        int stars = Integer.parseInt(((String) starsBox.getSelectedItem()).substring(0, 1));
        try {
            service.rateClass(classId, stars, commentArea.getText());
            commentArea.setText("");
            StudentUi.info(this, "Thank you for your feedback!");
            refresh();
        } catch (RuntimeException e) {
            StudentUi.error(this, e);
        }
    }
}
