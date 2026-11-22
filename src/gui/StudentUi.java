package gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Component;

/** Small helpers shared by the Student screens (owner: S3). */
final class StudentUi {

    private StudentUi() {
    }

    /** Standard page: heading at the top, content in the middle. */
    static JPanel page(JPanel panel, String title, String subtitle) {
        panel.setLayout(new BorderLayout(12, 12));
        panel.setBackground(UiStyle.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(UiStyle.heading(title), BorderLayout.NORTH);
        if (subtitle != null) {
            top.add(muted(subtitle), BorderLayout.SOUTH);
        }
        panel.add(top, BorderLayout.NORTH);
        return panel;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UiStyle.MUTED);
        l.setFont(UiStyle.BODY_FONT);
        return l;
    }

    /** ID in column 0 of the selected row, or null (works with a sorted table). */
    static String selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row), 0));
    }

    static void error(Component parent, Exception e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Cannot continue", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "SDAMS", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    static JComponent card(String label, JLabel value) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(UiStyle.PANEL);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0xDDD8D0)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        c.add(muted(label), BorderLayout.NORTH);
        value.setFont(UiStyle.TITLE_FONT);
        value.setForeground(UiStyle.PRIMARY);
        c.add(value, BorderLayout.CENTER);
        return c;
    }
}
