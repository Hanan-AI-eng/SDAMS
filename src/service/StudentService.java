package service;

import model.Role;
import model.StudentProfile;
import model.User;
import model.UserFactory;
import util.ActivityLog;
import util.DataFiles;
import util.FileHandler;
import util.IdGenerator;
import util.Repository;
import util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Add, view, search, update and delete students.
 * Login fields go in users.txt; profile fields go in student_profiles.txt.
 * Both files use the same S### ID and are always changed together.
 */
public class StudentService extends AccountService {

    // bookings.txt: bookingId|studentId|classId|bookingDate|status|paymentStatus
    private static final int BOOKING_STUDENT = 1;
    private static final int BOOKING_STATUS = 4;

    private final Repository<StudentProfile> profiles =
            new Repository<>(DataFiles.STUDENT_PROFILES, StudentProfile::fromFileLine);

    public StudentService(String actingUserId) {
        super(actingUserId);
    }

    /** A student's account and profile shown together. Profile is null if the files are out of sync. */
    public static class StudentEntry {
        private final User account;
        private final StudentProfile profile;

        public StudentEntry(User account, StudentProfile profile) {
            this.account = account;
            this.profile = profile;
        }

        public User getAccount() { return account; }
        public StudentProfile getProfile() { return profile; }
        public boolean hasProfile() { return profile != null; }
    }

    public List<StudentEntry> getAllStudents() {
        Map<String, StudentProfile> byId = new HashMap<>();
        for (StudentProfile p : profiles.findAll()) {
            byId.put(p.getId(), p);
        }
        List<StudentEntry> list = new ArrayList<>();
        for (User u : allUsers()) {
            if (u.getRole() == Role.STUDENT) {
                list.add(new StudentEntry(u, byId.get(u.getId())));
            }
        }
        return list;
    }

    /** Search by ID, name, username, discipline or belt rank. */
    public List<StudentEntry> search(String keyword) {
        List<StudentEntry> result = new ArrayList<>();
        for (StudentEntry e : getAllStudents()) {
            User u = e.getAccount();
            StudentProfile p = e.getProfile();
            if (matches(keyword, u.getId(), u.getName(), u.getUsername(),
                    p == null ? null : p.getDiscipline(), p == null ? null : p.getBeltRank())) {
                result.add(e);
            }
        }
        return result;
    }

    public StudentEntry addStudent(String name, String username, String password, String phone,
                                   String email, String discipline, String beltRank,
                                   String dateJoinedText, String emergencyContact) {
        requireUniqueUsername(username, null);
        String id = IdGenerator.nextId(DataFiles.USERS, Role.STUDENT.getIdPrefix());

        // Build both objects first: if anything is invalid, nothing is written.
        User account = UserFactory.create(Role.STUDENT, id, name, username, password, phone, email);
        StudentProfile profile = new StudentProfile(id, discipline, beltRank,
                parseJoinDate(dateJoinedText), emergencyContact);

        if (!users.add(account)) {
            throw new IllegalStateException("Could not save to " + DataFiles.USERS + ".");
        }
        profiles.delete(id); // clear any stale profile left with this ID
        if (!profiles.add(profile)) {
            users.delete(id); // roll back so the two files stay consistent
            throw new IllegalStateException("Could not save to " + DataFiles.STUDENT_PROFILES
                    + ". The student was not added.");
        }
        ActivityLog.log(actingUserId, "ADD_STUDENT", id + " " + account.getName());
        return new StudentEntry(account, profile);
    }

    /** Leave newPassword blank to keep the current password. */
    public StudentEntry updateStudent(String id, String name, String username, String newPassword,
                                      String phone, String email, String discipline, String beltRank,
                                      String dateJoinedText, String emergencyContact) {
        User existing = findUser(id, Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Student " + id + " was not found."));
        requireUniqueUsername(username, id);
        String password = Validator.isEmpty(newPassword) ? existing.getPassword() : newPassword;

        User account = UserFactory.create(Role.STUDENT, id, name, username, password, phone, email);
        StudentProfile profile = new StudentProfile(id, discipline, beltRank,
                parseJoinDate(dateJoinedText), emergencyContact);

        if (!users.update(account)) {
            throw new IllegalStateException("Could not update " + DataFiles.USERS + ".");
        }
        // Update the profile, or recreate it if it was missing (repairs out-of-sync data).
        boolean profileSaved = profiles.exists(id) ? profiles.update(profile) : profiles.add(profile);
        if (!profileSaved) {
            throw new IllegalStateException("Login details were saved, but "
                    + DataFiles.STUDENT_PROFILES + " could not be updated.");
        }
        ActivityLog.log(actingUserId, "UPDATE_STUDENT", id + " " + account.getName());
        return new StudentEntry(account, profile);
    }

    /** Number of BOOKED or RESCHEDULED bookings this student still has. */
    public int countActiveBookings(String studentId) {
        int count = 0;
        for (String line : FileHandler.readAllLines(DataFiles.BOOKINGS)) {
            String[] f = FileHandler.split(line);
            String status = field(f, BOOKING_STATUS).toUpperCase();
            if (field(f, BOOKING_STUDENT).equals(studentId)
                    && (status.equals("BOOKED") || status.equals("RESCHEDULED"))) {
                count++;
            }
        }
        return count;
    }

    /** Deletes the student from BOTH files. Blocked while the student has active bookings. */
    public void deleteStudent(String id) {
        User existing = findUser(id, Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Student " + id + " was not found."));
        int active = countActiveBookings(id);
        if (active > 0) {
            throw new IllegalStateException(existing.getName() + " has " + active
                    + " active booking(s). Cancel them before deleting this student.");
        }
        if (!users.delete(id)) {
            throw new IllegalStateException("Could not delete from " + DataFiles.USERS + ".");
        }
        profiles.delete(id); // returns false if there was no profile; that is fine
        ActivityLog.log(actingUserId, "DELETE_STUDENT", id + " " + existing.getName());
    }

    private static LocalDate parseJoinDate(String text) {
        LocalDate date = Validator.parseDate(text);
        if (date == null) {
            throw new IllegalArgumentException("Join date must be a real date in yyyy-MM-dd format.");
        }
        return date;
    }
}
