package util;

import model.Record;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared file utility for the whole team (owner: S2, Administrator).
 *
 * Every text file lives in the data/ folder and stores one record per line,
 * with fields separated by a pipe (|). The first field of every line is the
 * record ID, which is how update and delete find the right line.
 *
 * All IOExceptions are handled here, in one place, so the rest of the system
 * never has to deal with them. Methods return false (or an empty list) when
 * something goes wrong instead of crashing the program.
 *
 * How teammates use it:
 *   List<String> lines = FileHandler.readAllLines(DataFiles.CLASSES);
 *   FileHandler.appendLine(DataFiles.BOOKINGS, booking.toFileLine());
 *   FileHandler.save(DataFiles.BOOKINGS, booking);          // any Record
 *   FileHandler.updateLineById(DataFiles.BOOKINGS, "B001", newLine);
 *   FileHandler.deleteLineById(DataFiles.BOOKINGS, "B001");
 *   String[] f = FileHandler.split(line);                   // safe split
 */
public final class FileHandler {

    public static final String DATA_DIR = "data";
    public static final String DELIMITER = "|";
    public static final String EMPTY_FIELD = "-";

    private FileHandler() {
        // utility class: no objects
    }

    /** Returns the path of a data file, creating the folder and file if missing. */
    private static Path pathOf(String fileName) throws IOException {
        Path dir = Paths.get(DATA_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path file = dir.resolve(fileName);
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        return file;
    }

    /** Reads every non-blank line. A missing file is created and an empty list returned. */
    public static List<String> readAllLines(String fileName) {
        List<String> result = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(pathOf(fileName), StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    result.add(line);
                }
            }
        } catch (IOException e) {
            logError("read", fileName, e);
        }
        return result;
    }

    /**
     * Replaces the whole file with the given lines.
     * Writes to a temporary file first, then swaps it in, so a crash halfway
     * through never leaves a half-written data file.
     */
    public static boolean writeAllLines(String fileName, List<String> lines) {
        try {
            Path target = pathOf(fileName);
            Path temp = target.resolveSibling(fileName + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            logError("write", fileName, e);
            return false;
        }
    }

    /** Adds one line to the end of the file. */
    public static boolean appendLine(String fileName, String line) {
        try {
            Path file = pathOf(fileName);
            // make sure the previous last line ends with a newline
            String prefix = "";
            if (Files.size(file) > 0) {
                byte[] bytes = Files.readAllBytes(file);
                if (bytes[bytes.length - 1] != '\n') {
                    prefix = System.lineSeparator();
                }
            }
            Files.write(file, (prefix + line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            logError("append to", fileName, e);
            return false;
        }
    }

    /** Returns the line whose first field equals the ID, or null if there is none. */
    public static String findLineById(String fileName, String id) {
        for (String line : readAllLines(fileName)) {
            if (idOf(line).equals(id)) {
                return line;
            }
        }
        return null;
    }

    /**
     * Replaces the line whose first field equals the ID.
     * Reads all lines, swaps only the matching one, and writes everything back,
     * so every other line is copied unchanged.
     *
     * @return false if the ID was not found or the file could not be written
     */
    public static boolean updateLineById(String fileName, String id, String newLine) {
        List<String> lines = readAllLines(fileName);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (idOf(lines.get(i)).equals(id)) {
                lines.set(i, newLine);
                found = true;
                break;
            }
        }
        return found && writeAllLines(fileName, lines);
    }

    /**
     * Removes the line whose first field equals the ID.
     *
     * @return false if the ID was not found or the file could not be written
     */
    public static boolean deleteLineById(String fileName, String id) {
        List<String> lines = readAllLines(fileName);
        boolean removed = lines.removeIf(line -> idOf(line).equals(id));
        return removed && writeAllLines(fileName, lines);
    }

    // ---- Record-aware helpers (polymorphism: works for ANY subclass of Record) ----

    /** Appends any Record. FileHandler does not need to know what kind of record it is. */
    public static boolean save(String fileName, Record record) {
        return appendLine(fileName, record.toFileLine());
    }

    /** Rewrites the line of any Record, matched by its ID. */
    public static boolean update(String fileName, Record record) {
        return updateLineById(fileName, record.getId(), record.toFileLine());
    }

    // ---- Line helpers ----

    /** Splits a line on | and keeps empty trailing fields. */
    public static String[] split(String line) {
        return line.split("\\|", -1);
    }

    /** Joins fields with |. Null or blank values become "-" so no field is ever empty. */
    public static String join(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(DELIMITER);
            }
            String value = fields[i] == null ? "" : fields[i].toString().trim();
            sb.append(value.isEmpty() ? EMPTY_FIELD : value);
        }
        return sb.toString();
    }

    private static String idOf(String line) {
        int cut = line.indexOf(DELIMITER);
        return (cut < 0 ? line : line.substring(0, cut)).trim();
    }

    private static void logError(String action, String fileName, IOException e) {
        System.err.println("FileHandler: could not " + action + " " + fileName + ": " + e.getMessage());
    }
}
