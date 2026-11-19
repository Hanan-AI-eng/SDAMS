package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STAND-IN for S5's IdGenerator. Next ID = highest existing number + 1,
 * formatted as prefix + 3 digits (A001, I002, S015 ...).
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String nextId(String fileName, String prefix) {
        // Exact prefix + digits only, so prefix "F" never matches "FP001".
        Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
        int highest = 0;
        for (String line : FileHandler.readAllLines(fileName)) {
            Matcher m = pattern.matcher(FileHandler.split(line)[0].trim());
            if (m.matches()) {
                highest = Math.max(highest, Integer.parseInt(m.group(1)));
            }
        }
        return String.format("%s%03d", prefix, highest + 1);
    }
}
