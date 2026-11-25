package service;

import model.Role;
import model.User;
import model.UserFactory;
import util.ActivityLog;
import util.DataFiles;
import util.IdGenerator;
import util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * User accounts for the login screen and the Manager's "Manage Administrators"
 * feature (owner: S1). Reuses S2's AccountService for the username check.
 */
public class UserService extends AccountService {

    public UserService(String actingUserId) {
        super(actingUserId);
    }

    /** Returns the user whose username and password match, if any. */
    public Optional<User> authenticate(String username, String password) {
        for (User u : allUsers()) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    public boolean usernameExists(String username) {
        return isUsernameTaken(username, null);
    }

    public List<User> getAllUsers() {
        return allUsers();
    }

    // ---------------- Manage Administrators ----------------

    public List<User> getAdministrators() {
        return searchAdministrators("");
    }

    /** Search administrators by ID or name (also username). */
    public List<User> searchAdministrators(String keyword) {
        List<User> result = new ArrayList<>();
        for (User u : allUsers()) {
            if (u.getRole() == Role.ADMINISTRATOR && matches(keyword, u.getId(), u.getName(), u.getUsername())) {
                result.add(u);
            }
        }
        return result;
    }

    public User addAdministrator(String name, String username, String password, String phone, String email) {
        requireUniqueUsername(username, null);
        String id = IdGenerator.nextId(DataFiles.USERS, Role.ADMINISTRATOR.getIdPrefix());
        User admin = UserFactory.create(Role.ADMINISTRATOR, id, name, username, password, phone, email);
        if (!users.add(admin)) {
            throw new IllegalStateException("Could not save to " + DataFiles.USERS + ".");
        }
        ActivityLog.log(actingUserId, "ADD_ADMIN", id + " " + admin.getName());
        return admin;
    }

    /** Leave newPassword blank to keep the current password. */
    public User updateAdministrator(String id, String name, String username, String newPassword,
                                    String phone, String email) {
        User existing = findUser(id, Role.ADMINISTRATOR)
                .orElseThrow(() -> new IllegalArgumentException("Administrator " + id + " was not found."));
        requireUniqueUsername(username, id);
        String password = Validator.isEmpty(newPassword) ? existing.getPassword() : newPassword;
        User updated = UserFactory.create(Role.ADMINISTRATOR, id, name, username, password, phone, email);
        if (!users.update(updated)) {
            throw new IllegalStateException("Could not update " + DataFiles.USERS + ".");
        }
        ActivityLog.log(actingUserId, "UPDATE_ADMIN", id + " " + updated.getName());
        return updated;
    }

    public void deleteAdministrator(String id) {
        if (id.equals(actingUserId)) {
            throw new IllegalStateException("You cannot delete your own account.");
        }
        User existing = findUser(id, Role.ADMINISTRATOR)
                .orElseThrow(() -> new IllegalArgumentException("Administrator " + id + " was not found."));
        if (!users.delete(id)) {
            throw new IllegalStateException("Could not delete from " + DataFiles.USERS + ".");
        }
        ActivityLog.log(actingUserId, "DELETE_ADMIN", id + " " + existing.getName());
    }

    // ---------------- Own password (extra feature) ----------------

    public void changeOwnPassword(String currentPassword, String newPassword, String confirmPassword) {
        User me = users.findById(actingUserId)
                .orElseThrow(() -> new IllegalStateException("Your account was not found."));
        if (!me.getPassword().equals(currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password must be different from the current one.");
        }
        me.setPassword(newPassword);   // validated setter
        if (!users.update(me)) {
            throw new IllegalStateException("Could not update " + DataFiles.USERS + ".");
        }
        ActivityLog.log(actingUserId, "CHANGE_PASSWORD", actingUserId);
    }
}
