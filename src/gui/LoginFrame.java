package gui;

import model.User;
import model.UserFactory;
import util.DataFiles;
import util.FileHandler;
import util.Validator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * STAND-IN for S1's login screen, included so the Administrator module can be
 * run and tested on its own. S1's real LoginFrame replaces this file.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public LoginFrame() {
        super("SDAMS - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UiStyle.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Self Defence Academy");
        title.setFont(UiStyle.TITLE_FONT);
        title.setForeground(UiStyle.PRIMARY);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(title, c);

        c.gridwidth = 1;
        c.gridy = 1; panel.add(new JLabel("Username"), c);
        c.gridx = 1; panel.add(usernameField, c);
        c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Password"), c);
        c.gridx = 1; panel.add(passwordField, c);

        JButton login = UiStyle.primaryButton("Login");
        JButton exit = UiStyle.secondaryButton("Exit");
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(login);
        buttons.add(exit);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        panel.add(buttons, c);

        JLabel hint = new JLabel("Demo admin: siti01 / pass123");
        hint.setForeground(Color.GRAY);
        c.gridy = 4;
        panel.add(hint, c);

        login.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        exit.addActionListener(e -> System.exit(0));
        getRootPane().setDefaultButton(login);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (Validator.isEmpty(username) || Validator.isEmpty(password)) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                    "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (String line : FileHandler.readAllLines(DataFiles.USERS)) {
            try {
                User user = UserFactory.fromFileLine(line);
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                    dispose();
                    user.openDashboard();   // polymorphism: each subclass opens its own screen
                    return;
                }
            } catch (RuntimeException ignored) {
                // skip malformed lines
            }
        }
        JOptionPane.showMessageDialog(this, "Incorrect username or password.",
                "Login failed", JOptionPane.ERROR_MESSAGE);
        passwordField.setText("");
    }
}
