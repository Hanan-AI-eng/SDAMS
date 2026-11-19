package util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * STAND-IN for S5's Validator. Replace with S5's version when it is ready;
 * keep the method names below or update the calls in the Administrator module.
 */
public final class Validator {

    private static final String EMAIL_REGEX = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^0\\d{9,10}$";          // e.g. 0123456789
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_]{4,20}$";
    public static final int MIN_PASSWORD_LENGTH = 6;

    private Validator() {
    }

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** True if the value contains the field delimiter, which would corrupt the file. */
    public static boolean hasPipe(String value) {
        return value != null && value.contains("|");
    }

    public static boolean isValidEmail(String value) {
        return value != null && value.trim().matches(EMAIL_REGEX);
    }

    public static boolean isValidPhone(String value) {
        return value != null && value.trim().matches(PHONE_REGEX);
    }

    public static boolean isValidUsername(String value) {
        return value != null && value.trim().matches(USERNAME_REGEX);
    }

    public static boolean isValidPassword(String value) {
        return value != null && value.length() >= MIN_PASSWORD_LENGTH && !value.contains(" ");
    }

    public static boolean isWithinLength(String value, int max) {
        return value != null && value.trim().length() <= max;
    }

    /** Parses yyyy-MM-dd. Returns null if the text is not a real date. */
    public static LocalDate parseDate(String value) {
        if (isEmpty(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Checks a required text field and throws a readable message if it is invalid. */
    public static String requireText(String value, String fieldName, int maxLength) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (hasPipe(value)) {
            throw new IllegalArgumentException(fieldName + " must not contain the | character.");
        }
        if (!isWithinLength(value, maxLength)) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return value.trim();
    }
}
