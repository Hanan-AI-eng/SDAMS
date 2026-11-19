package service;

import model.Role;
import model.User;
import model.UserFactory;
import util.ActivityLog;
import util.DataFiles;
import util.FileHandler;
import util.IdGenerator;
import util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Add, view, search, update and delete staff accounts in users.txt.
 *
 * Assumption: the Administrator manages Instructors, Facilities Officers and
 * Attendance Officers (the brief does not say who creates Attendance Officers).
 */
public class StaffService extends AccountService {

    public static final List<Role> STAFF_ROLES = Arrays.asList(
            Role.INSTRUCTOR, Role.FACILITIES_OFFICER, Role.ATTENDANCE_OFFICER);

    // classes.txt: classId|instructorId|discipline|title|date|startTime|endTime|room|capacity|fee|status
    private static final int CLASS_INSTRUCTOR = 1;
    private static final int CLASS_TITLE = 3;
    private static final int CLASS_DATE = 4;
    private static final int CLASS_STATUS = 10;

    public StaffService(String actingUserId) {
        super(actingUserId);
    }

    public List<User> getAllStaff() {
        List<User> staff = new ArrayList<>();
        for (User u : allUsers()) {
            if (STAFF_ROLES.contains(u.getRole())) {
                staff.add(u);
            }
        }
        return staff;
    }

    /** Search by ID, name or username; roleFilter null means all staff roles. */
    public List<User> search(String keyword, Role roleFilter) {
        List<User> result = new ArrayList<>();
        for (User u : getAllStaff()) {
            boolean roleOk = roleFilter == null || u.getRole() == roleFilter;
            if (roleOk && matches(keyword, u.getId(), u.getName(), u.getUsername(),
                    u.getRole().getDisplayName())) {
                result.add(u);
            }
        }
        return result;
    }

    public User addStaff(Role role, String name, String username, String password,
                         String phone, String email) {
        if (!STAFF_ROLES.contains(role)) {
            throw new IllegalArgumentException("Please choose a staff role.");
        }
        requireUniqueUsername(username, null);
        String id = IdGenerator.nextId(DataFiles.USERS, role.getIdPrefix());
        User staff = UserFactory.create(role, id, name, username, password, phone, email); // validates
        if (!users.add(staff)) {
            throw new IllegalStateException("Could not save to " + DataFiles.USERS + ".");
        }
        ActivityLog.log(actingUserId, "ADD_STAFF", id + " " + staff.getName() + " (" + role.name() + ")");
        return staff;
    }

    /**
     * Updates a staff member. The role cannot change after creation (assumption).
     * Leave newPassword blank to keep the current password.
     */
    public User updateStaff(String id, String name, String username, String newPassword,
                            String phone, String email) {
        User existing = users.findById(id)
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff " + id + " was not found."));
        requireUniqueUsername(username, id);
        String password = Validator.isEmpty(newPassword) ? existing.getPassword() : newPassword;
        User updated = UserFactory.create(existing.getRole(), id, name, username, password, phone, email);
        if (!users.update(updated)) {
            throw new IllegalStateException("Could not update " + DataFiles.USERS + ".");
        }
        ActivityLog.log(actingUserId, "UPDATE_STAFF", id + " " + updated.getName());
        return updated;
    }

    /** Titles and dates of classes this instructor still has today or later. */
    public List<String> getUpcomingClasses(String instructorId) {
        List<String> upcoming = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (String line : FileHandler.readAllLines(DataFiles.CLASSES)) {
            String[] f = FileHandler.split(line);
            if (!field(f, CLASS_INSTRUCTOR).equals(instructorId)) {
                continue;
            }
            LocalDate date = dateOrNull(field(f, CLASS_DATE));
            String status = field(f, CLASS_STATUS).toUpperCase();
            boolean finished = status.equals("CANCELLED") || status.equals("COMPLETED");
            if (date != null && !date.isBefore(today) && !finished) {
                upcoming.add(field(f, 0) + " " + field(f, CLASS_TITLE) + " on " + date);
            }
        }
        return upcoming;
    }

    /** Deletes a staff member. Blocked if an instructor still has upcoming classes. */
    public void deleteStaff(String id) {
        User existing = users.findById(id)
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff " + id + " was not found."));
        if (existing.getRole() == Role.INSTRUCTOR) {
            List<String> upcoming = getUpcomingClasses(id);
            if (!upcoming.isEmpty()) {
                throw new IllegalStateException(existing.getName() + " still has upcoming classes:\n  "
                        + String.join("\n  ", upcoming)
                        + "\nReassign or cancel these classes before deleting.");
            }
        }
        if (!users.delete(id)) {
            throw new IllegalStateException("Could not delete from " + DataFiles.USERS + ".");
        }
        ActivityLog.log(actingUserId, "DELETE_STAFF", id + " " + existing.getName());
    }
}
