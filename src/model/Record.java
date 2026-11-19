package model;

/**
 * Base class for everything stored in a text file (owner: S2, Administrator).
 *
 * Abstraction: callers only know that a Record has an ID and can turn itself
 * into one line of text. How each subclass builds that line is hidden inside it.
 *
 * Every model class in the team (User, StudentProfile, ClassSession, Booking,
 * Payment, Equipment, ...) extends Record, so FileHandler and Repository can
 * save any of them through toFileLine() without knowing the exact type.
 *
 * Why an abstract class and not an interface: every record shares the same ID
 * field and the same ID validation, and an abstract class can hold that shared
 * state and code once instead of repeating it in every model.
 */
public abstract class Record {

    private final String id;

    protected Record(String id) {
        if (id == null || !id.matches("[A-Z]{1,2}\\d{3,}")) {
            throw new IllegalArgumentException("Invalid ID: " + id);
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /** Converts this object into one pipe-separated line for its text file. */
    public abstract String toFileLine();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id.equals(((Record) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }
}
