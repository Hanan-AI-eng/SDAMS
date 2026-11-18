package sdams.facilities.util;

/**
 * Thrown when something the user typed is not acceptable
 * (empty field, text instead of a number, unknown ID, and so on).
 * The message is written for the user and is shown in a dialog box.
 */
public class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}
