package sdams.facilities.storage;

/**
 * Thrown when a text file cannot be read or written
 * (missing folder permissions, file locked, file is really a folder, etc.).
 */
public class StorageException extends Exception {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
