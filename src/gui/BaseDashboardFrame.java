package gui;

import model.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * STAND-IN for S6's base dashboard frame: header with the user's name and a
 * Logout button, a side menu, and a content area that swaps between sections.
 * Each role's dashboard extends this and calls addSection().
 */
public abstract class BaseDashboardFrame extends JFrame {

    private final User currentUser;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel menu = new JPanel();
    private final List<JButton> menuButtons = new ArrayList<>();

    protected BaseDashboardFrame(String title, User currentUser) {
        super("SDAMS - " + title);
        this.currentUser = currentUser;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);

        // header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiStyle.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel titleLabel = new JLabel("Self Defence Academy  |  " + title);
        titleLabel.setFont(UiStyle.TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        JLabel userLabel = new JLabel("Logged in as " + currentUser.getName()
                + " (" + currentUser.getId() + ")   ");
        userLabel.setForeground(Color.WHITE);
        JButton logout = UiStyle.secondaryButton("Logout");
        logout.addActionListener(e -> logout());
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(userLabel);
        right.add(logout);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        // side menu
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(UiStyle.PRIMARY_DARK);
        menu.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        menu.setPreferredSize(new Dimension(190, 0));

        content.setBackground(UiStyle.BACKGROUND);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(menu, BorderLayout.WEST);
        getContentPane().add(content, BorderLayout.CENTER);
    }

    /** Adds a menu button that shows the given panel. */
    protected void addSection(String name, JPanel panel) {
        content.add(panel, name);
        JButton b = UiStyle.secondaryButton(name);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.addActionListener(e -> showSection(name, b, panel));
        menuButtons.add(b);
        menu.add(b);
        menu.add(Box.createVerticalStrut(8));
        if (menuButtons.size() == 1) {
            showSection(name, b, panel);
        }
    }

    private void showSection(String name, JButton selected, JPanel panel) {
        for (JButton b : menuButtons) {
            b.setBackground(b == selected ? Color.WHITE : new Color(0xE4E0DA));
        }
        if (panel instanceof Refreshable) {
            ((Refreshable) panel).refresh();
        }
        cards.show(content, name);
    }

    protected void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out of SDAMS?", "Logout",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /** Panels that reload their data whenever they are shown. */
    public interface Refreshable {
        void refresh();
    }
}
