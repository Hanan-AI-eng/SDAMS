package sdams.facilities;

import java.nio.file.Paths;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import sdams.facilities.ui.FacilitiesOfficerFrame;

/**
 * STANDALONE DEMO LAUNCHER for the Facilities Officer part only.
 * It is NOT the group's real login or main menu. Once the group's main menu exists,
 * it should open FacilitiesOfficerFrame itself and this class is no longer needed.
 */
public class FacilitiesOfficerApp {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // If the native look is unavailable Swing falls back to its default look.
        }
        System.out.println("Data folder in use: " + Paths.get("data").toAbsolutePath());

        // Swing screens must be created on the "event dispatch thread"; invokeLater does that.
        SwingUtilities.invokeLater(() -> {
            FacilitiesOfficerFrame frame = FacilitiesOfficerFrame.createWithDefaultFiles();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // standalone: closing the window ends the program
            frame.setVisible(true);
        });
    }
}
