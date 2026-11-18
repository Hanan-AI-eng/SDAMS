package sdams.facilities.ui;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * All pop-up dialogs go through this one class, so every screen gives the same style of feedback.
 * Test mode (used only by the automated tests) records the message instead of showing a dialog.
 */
public final class Messages {

    private static boolean testMode = false;
    private static boolean testConfirmAnswer = true;
    private static String lastMessage = "";

    private Messages() {
    }

    public static void setTestMode(boolean on, boolean confirmAnswer) {
        testMode = on;
        testConfirmAnswer = confirmAnswer;
        lastMessage = "";
    }

    public static String getLastMessage() {
        return lastMessage;
    }

    public static void info(Component parent, String message) {
        show(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        show(parent, message, "Please check your input", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String message) {
        show(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        if (testMode) {
            lastMessage = message;
            return testConfirmAnswer;
        }
        int choice = JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private static void show(Component parent, String message, String title, int type) {
        if (testMode) {
            lastMessage = message;
            return;
        }
        JOptionPane.showMessageDialog(parent, message, title, type);
    }
}
