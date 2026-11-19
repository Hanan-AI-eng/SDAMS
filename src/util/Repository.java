package util;

import model.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generic repository reused by every service (extra feature, owner: S2).
 *
 * T is any subclass of Record. The repository needs only a file name and a
 * function that rebuilds a T from one line; everything else (load, find, add,
 * update, delete) is written once here and reused for users, profiles,
 * classes, bookings and so on.
 *
 * Example:
 *   Repository<StudentProfile> profiles =
 *       new Repository<>(DataFiles.STUDENT_PROFILES, StudentProfile::fromFileLine);
 */
public class Repository<T extends Record> {

    private final String fileName;
    private final Function<String, T> parser;

    public Repository(String fileName, Function<String, T> parser) {
        this.fileName = fileName;
        this.parser = parser;
    }

    /** Loads every valid line. A broken line is skipped and reported, never fatal. */
    public List<T> findAll() {
        List<T> items = new ArrayList<>();
        for (String line : FileHandler.readAllLines(fileName)) {
            try {
                items.add(parser.apply(line));
            } catch (RuntimeException e) {
                System.err.println("Skipping bad line in " + fileName + ": " + line
                        + " (" + e.getMessage() + ")");
            }
        }
        return items;
    }

    public Optional<T> findById(String id) {
        String line = FileHandler.findLineById(fileName, id);
        if (line == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(parser.apply(line));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public boolean exists(String id) {
        return FileHandler.findLineById(fileName, id) != null;
    }

    public boolean add(T item) {
        return FileHandler.save(fileName, item);
    }

    public boolean update(T item) {
        return FileHandler.update(fileName, item);
    }

    public boolean delete(String id) {
        return FileHandler.deleteLineById(fileName, id);
    }

    public String getFileName() {
        return fileName;
    }
}
