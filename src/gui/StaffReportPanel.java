package gui;

import model.Role;
import model.User;
import service.StaffReport;
import util.ReportExporter;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Staff Report: filter by role and name, with totals per role under the table. */
public class StaffReportPanel extends JPanel implements BaseDashboardFrame.Refreshable {

    private static final String ALL = "All";

    private final StaffReport report = new StaffReport();
    private final JComboBox<Object> roleFilter = new JComboBox<>();
    private final JTextField nameField = new JTextField(18);
    private final DefaultTableModel model = UiStyle.readOnlyModel("ID", "Name", "Role", "Phone", "Email");
    private final JTable table = UiStyle.table(model);
    private final JLabel totalsLabel = new JLabel();

    public StaffReportPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiStyle.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
        m.addElement(ALL);
        for (Role r : StaffReport.REPORT_ROLES) {
            m.addElement(r);
        }
        roleFilter.setModel(m);
        roleFilter.addActionListener(e -> refresh());
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        JButton export = UiStyle.secondaryButton("Export to .txt");
        export.addActionListener(e -> export());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(UiStyle.heading("Staff Report"));
        bar.add(new JLabel("    Role:"));
        bar.add(roleFilter);
        bar.add(new JLabel("  Name or ID:"));
        bar.add(nameField);
        bar.add(export);
        add(bar, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        totalsLabel.setFont(UiStyle.HEADING_FONT);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(totalsLabel);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    /** Re-runs the report whenever a filter changes, so totals always match the table. */
    @Override
    public void refresh() {
        Object sel = roleFilter.getSelectedItem();
        Role role = sel instanceof Role ? (Role) sel : null;
        List<User> rows = report.generate(role, nameField.getText());
        model.setRowCount(0);
        for (User u : rows) {
            model.addRow(new Object[]{u.getId(), u.getName(), u.getRole().getDisplayName(),
                    u.getPhone(), u.getEmail()});
        }
        StringBuilder sb = new StringBuilder("<html>");
        for (Map.Entry<Role, Integer> e : report.totalsByRole().entrySet()) {
            sb.append(e.getKey().getDisplayName()).append("s: <b>").append(e.getValue()).append("</b>&nbsp;&nbsp;&nbsp; ");
        }
        sb.append("Total: <b>").append(report.total()).append("</b>");
        if (rows.isEmpty()) {
            sb.append("&nbsp;&nbsp;&nbsp; (No records match these filters)");
        }
        totalsLabel.setText(sb.append("</html>").toString());
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
