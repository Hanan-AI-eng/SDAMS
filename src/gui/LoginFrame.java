package gui;

import model.User;
import service.UserService;
import util.ActivityLog;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Login screen: the single entry point of SDAMS (owner: S1).
 *
 * After a successful login it calls user.openDashboard(). The login screen
 * never checks the role itself: each User subclass opens its own dashboard.
 * That is the polymorphism to explain in the report and presentation.
 *
 * Extra feature: an account is locked for 60 seconds after 3 failed attempts.
 */
public class LoginFrame extends JFrame {

    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_MILLIS = 60_000;

    // Shared across all LoginFrame windows for this run of the program.
    private static final Map<String, Integer> failedAttempts = new HashMap<>();
    private static final Map<String, Long> lockedUntil = new HashMap<>();

    private final UserService userService = new UserService("LOGIN");
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
        JLabel subtitle = new JLabel("Management System - please log in");
        subtitle.setForeground(UiStyle.MUTED);
        c.gridy = 1;
        panel.add(subtitle, c);

        c.gridwidth = 1;
        c.gridy = 2; panel.add(new JLabel("Username"), c);
        c.gridx = 1; panel.add(usernameField, c);
        c.gridx = 0; c.gridy = 3; panel.add(new JLabel("Password"), c);
        c.gridx = 1; panel.add(passwordField, c);

        JButton login = UiStyle.primaryButton("Login");
        JButton exit = UiStyle.secondaryButton("Exit");
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(login);
        buttons.add(exit);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        panel.add(buttons, c);

        JLabel hint = new JLabel("Demo: manager01 / admin123  ·  siti01 / pass123");
        hint.setForeground(Color.GRAY);
        c.gridy = 5;
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
            showError("Please enter both username and password.");
            return;
        }

        String key = username.toLowerCase();
        Long until = lockedUntil.get(key);
        if (until != null && System.currentTimeMillis() < until) {
            long seconds = (until - System.currentTimeMillis() + 999) / 1000;
            showError("This account is locked after " + MAX_ATTEMPTS + " failed attempts.\n"
                    + "Try again in " + seconds + " seconds.");
            return;
        }

        Optional<User> user = userService.authenticate(username, password);
        if (user.isPresent()) {
            failedAttempts.remove(key);
            lockedUntil.remove(key);
            ActivityLog.log(user.get().getId(), "LOGIN", user.get().getRole().name());
            dispose();
            user.get().openDashboard();   // polymorphism: no "if role == ..." chain here
            return;
        }

        passwordField.setText("");
        if (!userService.usernameExists(username)) {
            showError("Incorrect username or password.");
            return;
        }
        int attempts = failedAttempts.merge(key, 1, Integer::sum);
        if (attempts >= MAX_ATTEMPTS) {
            failedAttempts.remove(key);
            lockedUntil.put(key, System.currentTimeMillis() + LOCK_MILLIS);
            ActivityLog.log("-", "ACCOUNT_LOCKED", username);
            showError("Incorrect password entered " + MAX_ATTEMPTS + " times.\n"
                    + "This account is locked for " + (LOCK_MILLIS / 1000) + " seconds.");
        } else {
            showError("Incorrect username or password.\n"
                    + (MAX_ATTEMPTS - attempts) + " attempt(s) left before the account is locked.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Login failed", JOptionPane.ERROR_MESSAGE);
    }
}
