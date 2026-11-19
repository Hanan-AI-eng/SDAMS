package gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Simple two-column form (label on the left, field on the right). */
public class FormPanel extends JPanel {

    private int row = 0;

    public FormPanel(String title) {
        super(new GridBagLayout());
        setBackground(UiStyle.PANEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0xDDD8D0)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        GridBagConstraints c = constraints(0);
        c.gridwidth = 2;
        add(UiStyle.heading(title), c);
        row++;
    }

    public void addRow(String label, JComponent field) {
        GridBagConstraints c = constraints(0);
        c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(UiStyle.BODY_FONT);
        add(l, c);
        c = constraints(1);
        c.weightx = 1;
        add(field, c);
        row++;
    }

    /** A full-width component, e.g. a hint label or a row of buttons. */
    public void addWide(JComponent component) {
        GridBagConstraints c = constraints(0);
        c.gridwidth = 2;
        add(component, c);
        row++;
    }

    /** Pushes everything to the top. Call once after the last row. */
    public void finish() {
        GridBagConstraints c = constraints(0);
        c.weighty = 1;
        add(new JLabel(), c);
    }

    private GridBagConstraints constraints(int column) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }
}
