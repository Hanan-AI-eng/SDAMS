package service;

import model.Role;
import model.User;
import model.UserFactory;
import util.DataFiles;
import util.FileHandler;
import util.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Shared logic for services that manage accounts in users.txt.
 * StaffService and StudentService both extend it (inheritance), so the
 * username check and user lookups are written only once.
 */
public abstract class AccountService {

    protected final Repository<User> users =
            new Repository<>(DataFiles.USERS, UserFactory::fromFileLine);

    /** The admin who is logged in; used for the activity log. */
    protected final String actingUserId;

    protected AccountService(String actingUserId) {
        this.actingUserId = actingUserId;
    }

    /** Usernames must be unique across ALL users, not only one role. */
    public boolean isUsernameTaken(String username, String excludeUserId) {
        for (User u : users.findAll()) {
            if (u.getUsername().equalsIgnoreCase(username.trim())
                    && !u.getId().equals(excludeUserId)) {
                return true;
            }
        }
        return false;
    }

    protected void requireUniqueUsername(String username, String excludeUserId) {
        if (username != null && isUsernameTaken(username, excludeUserId)) {
            throw new IllegalArgumentException("Username \"" + username.trim() + "\" is already taken.");
        }
    }

    protected Optional<User> findUser(String id, Role expectedRole) {
        return users.findById(id).filter(u -> u.getRole() == expectedRole);
    }

    /** Case-insensitive "contains" used by every search box. */
    protected static boolean matches(String keyword, String... values) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String k = keyword.trim().toLowerCase();
        for (String v : values) {
            if (v != null && v.toLowerCase().contains(k)) {
                return true;
            }
        }
        return false;
    }

    /** Reads a date field safely; returns null for "-" or bad values. */
    protected static LocalDate dateOrNull(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    protected static String field(String[] f, int index) {
        return index < f.length ? f[index].trim() : FileHandler.EMPTY_FIELD;
    }

    protected List<User> allUsers() {
        return users.findAll();
    }
}
