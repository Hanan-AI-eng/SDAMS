import gui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point: opens the login screen. Run from the project root so data/ is found. */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // default look and feel is fine
            }
            new LoginFrame().setVisible(true);
        });
    }
}
