package gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

/**
 * STAND-IN for S6's shared look (colours, fonts, button and table styles).
 * Replace with S6's version; keep these method names or update the calls.
 */
public final class UiStyle {

    public static final Color PRIMARY = new Color(0x8B1E1E);      // deep red
    public static final Color PRIMARY_DARK = new Color(0x5E1414);
    public static final Color BACKGROUND = new Color(0xF6F4F1);
    public static final Color PANEL = Color.WHITE;
    public static final Color DANGER = new Color(0xB3261E);
    public static final Color MUTED = new Color(0x6B6B6B);

    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 20);
    public static final Font HEADING_FONT = new Font("SansSerif", Font.BOLD, 15);
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private UiStyle() {
    }

    public static JButton primaryButton(String text) {
        return styled(new JButton(text), PRIMARY, Color.WHITE);
    }

    public static JButton secondaryButton(String text) {
        return styled(new JButton(text), new Color(0xE4E0DA), Color.BLACK);
    }

    public static JButton dangerButton(String text) {
        return styled(new JButton(text), DANGER, Color.WHITE);
    }

    private static JButton styled(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(BODY_FONT.deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return b;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(HEADING_FONT);
        return l;
    }

    /** A table model whose cells cannot be edited by typing in them. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(BODY_FONT);
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(BODY_FONT.deriveFont(Font.BOLD));
        t.getTableHeader().setReorderingAllowed(false);
        t.setAutoCreateRowSorter(true);
        return t;
    }
}
