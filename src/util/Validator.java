package util;

public class Validator {

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidId(String id) {
        return id != null && id.matches("[A-Z]\\d{3}");
    }
}
