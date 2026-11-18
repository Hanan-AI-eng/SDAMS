package sdams.facilities.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic "read and write a text file of records" logic, written ONCE.
 * Each record is one line; fields are separated by the | character.
 *
 * It is abstract: it cannot be used directly. A child class (EquipmentRepository,
 * FacilityProblemRepository) fills in the two missing pieces:
 *   - parseLine:    turn one text line into an object
 *   - formatRecord: turn one object into a text line
 * T stands for "whatever type of record the child stores".
 */
public abstract class TextFileRepository<T> {

    protected static final String SEPARATOR = "|";

    private final Path filePath;
    private int skippedLineCount = 0;

    protected TextFileRepository(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    /** Convert one line to a record. Throw IllegalArgumentException if the line is damaged. */
    protected abstract T parseLine(String line);

    /** Convert one record to one line of text (no line break). */
    protected abstract String formatRecord(T record);

    public Path getFilePath() {
        return filePath;
    }

    /** How many damaged lines were ignored by the most recent loadAll(). */
    public int getSkippedLineCount() {
        return skippedLineCount;
    }

    /** Read every record from the file. A missing file is created empty. */
    public List<T> loadAll() throws StorageException {
        ensureFileExists();
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not read the file " + filePath.toAbsolutePath()
                    + ". Check that it exists and is not open or locked. (" + e.getMessage() + ")", e);
        }
        List<T> records = new ArrayList<>();
        skippedLineCount = 0;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue; // ignore blank lines
            }
            try {
                records.add(parseLine(line));
            } catch (IllegalArgumentException e) {
                skippedLineCount++; // damaged line: skip it instead of crashing
            }
        }
        return records;
    }

    /**
     * Replace the whole file with the given records.
     * It writes to a temporary file first and then swaps it in, so a failure
     * half-way through cannot leave the real file empty or half-written.
     */
    public void saveAll(List<T> records) throws StorageException {
        List<String> lines = new ArrayList<>();
        for (T record : records) {
            lines.add(formatRecord(record));
        }
        try {
            Path folder = filePath.toAbsolutePath().getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
            Path temp = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Could not save to " + filePath.toAbsolutePath()
                    + ". Check folder permissions and that the file is not open elsewhere. ("
                    + e.getMessage() + ")", e);
        }
    }

    private void ensureFileExists() throws StorageException {
        if (Files.exists(filePath)) {
            return;
        }
        try {
            Path folder = filePath.toAbsolutePath().getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new StorageException("The file " + filePath.toAbsolutePath()
                    + " does not exist and could not be created. (" + e.getMessage() + ")", e);
        }
    }

    /** Split a line into fields. The -1 keeps empty fields at the end of the line. */
    protected static String[] splitFields(String line) {
        return line.split("\\|", -1);
    }
}
