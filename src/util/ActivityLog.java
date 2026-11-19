package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Activity log (extra feature, owner: S2). Records who changed what and when
 * in data/activity_log.txt:  timestamp | userId | action | details
 */
public final class ActivityLog {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ActivityLog() {
    }

    public static void log(String userId, String action, String details) {
        String clean = details == null ? "-" : details.replace("|", "/");
        FileHandler.appendLine(DataFiles.ACTIVITY_LOG,
                FileHandler.join(LocalDateTime.now().format(FORMAT), userId, action, clean));
    }
}
