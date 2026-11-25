package gui;

import service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** My Account: the Manager changes their own password (extra feature). */
public class ChangePasswordPanel extends JPanel {

    private final UserService service;
    private final JPasswordField currentField = new JPasswordField();
    private final JPasswordField newField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();

    public ChangePasswordPanel(String userId) {
        this.service = new UserService(userId);
        setLayout(new BorderLayout());
        setBackground(UiStyle.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        FormPanel form = new FormPanel("Change my password");
        form.addRow("Current password", currentField);
        form.addRow("New password", newField);
        form.addRow("Confirm new password", confirmField);
        JLabel hint = new JLabel("At least 6 characters, no spaces, no | character.");
        hint.setForeground(UiStyle.MUTED);
        form.addWide(hint);
        JButton save = UiStyle.primaryButton("Save new password");
        save.addActionListener(e -> save());
        form.addWide(save);
        form.finish();
        form.setPreferredSize(new Dimension(420, 260));

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(form, BorderLayout.NORTH);
        add(holder, BorderLayout.WEST);
    }

    private void save() {
        try {
            service.changeOwnPassword(new String(currentField.getPassword()),
                    new String(newField.getPassword()), new String(confirmField.getPassword()));
            currentField.setText("");
            newField.setText("");
            confirmField.setText("");
            JOptionPane.showMessageDialog(this, "Your password has been changed.", "Done",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot continue", JOptionPane.ERROR_MESSAGE);
        }
    }
}
