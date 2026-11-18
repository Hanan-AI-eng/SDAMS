package sdams.facilities.util;

/**
 * Reusable input checks. Every method either returns a clean value
 * or throws a ValidationException with a clear message.
 * Static methods: you call them as Validator.requireText(...) without creating an object.
 */
public final class Validator {

    private Validator() {
        // Utility class: nobody should create a Validator object.
    }

    /** A required text value: not empty, not too long, no characters that would break the text file. */
    public static String requireText(String value, String fieldName, int maxLength) throws ValidationException {
        String text = (value == null) ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        checkSafe(text, fieldName, maxLength);
        return text;
    }

    /** An optional text value: may be empty, but if present it must be safe. */
    public static String optionalText(String value, String fieldName, int maxLength) throws ValidationException {
        String text = (value == null) ? "" : value.trim();
        checkSafe(text, fieldName, maxLength);
        return text;
    }

    /** A whole number from 0 to max. Text such as "abc" or "2.5" is rejected. */
    public static int requireNonNegativeInt(String value, String fieldName, int max) throws ValidationException {
        String text = (value == null) ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        int number;
        try {
            number = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a whole number (for example 10).");
        }
        if (number < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
        if (number > max) {
            throw new ValidationException(fieldName + " cannot be more than " + max + ".");
        }
        return number;
    }

    /** The value must be one of the allowed choices (ignoring upper/lower case). Returns the official spelling. */
    public static String requireChoice(String value, String fieldName, String[] allowed) throws ValidationException {
        String text = (value == null) ? "" : value.trim();
        for (String option : allowed) {
            if (option.equalsIgnoreCase(text)) {
                return option;
            }
        }
        throw new ValidationException(fieldName + " must be one of: " + String.join(", ", allowed) + ".");
    }

    private static void checkSafe(String text, String fieldName, int maxLength) throws ValidationException {
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName + " must be at most " + maxLength + " characters.");
        }
        // The pipe character separates fields in the text file, so it is not allowed inside a field.
        if (text.indexOf('|') >= 0) {
            throw new ValidationException(fieldName + " must not contain the | character.");
        }
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            throw new ValidationException(fieldName + " must be on a single line.");
        }
    }
}
