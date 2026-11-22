package service;

import model.AttendanceRecord;
import model.Booking;
import model.BookingStatus;
import model.Payment;
import model.PaymentMethod;
import model.PaymentStatus;
import model.Rating;
import model.Role;
import model.StudentProfile;
import model.TrainingClass;
import model.User;
import model.UserFactory;
import util.ActivityLog;
import util.DataFiles;
import util.IdGenerator;
import util.Repository;
import util.Validator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything a logged-in student can do (owner: S3, Student).
 *
 *  - view and update their own profile and password
 *  - browse upcoming classes and book a seat
 *  - view, cancel and reschedule their bookings
 *  - pay for bookings and see their payment history
 *  - view their attendance
 *  - rate classes they attended
 *
 * The GUI only calls methods here. Every rule (seat limits, deadlines, one
 * rating per class...) is checked in this class, and problems are reported as
 * IllegalArgumentException / IllegalStateException with a readable message.
 */
public class StudentPortalService {

    /** Class statuses that still accept bookings. */
    private static final List<String> BOOKABLE_CLASS_STATUSES = java.util.Arrays.asList("OPEN", "ACTIVE");

    private final String studentId;
    private final Repository<User> users = new Repository<>(DataFiles.USERS, UserFactory::fromFileLine);
    private final Repository<StudentProfile> profiles =
            new Repository<>(DataFiles.STUDENT_PROFILES, StudentProfile::fromFileLine);
    private final Repository<Booking> bookings = new Repository<>(DataFiles.BOOKINGS, Booking::fromFileLine);
    private final Repository<Payment> payments = new Repository<>(DataFiles.PAYMENTS, Payment::fromFileLine);
    private final Repository<Rating> ratings = new Repository<>(DataFiles.RATINGS, Rating::fromFileLine);
    private final ClassService classService = new ClassService();
    private final AttendanceService attendanceService = new AttendanceService();

    public StudentPortalService(String studentId) {
        if (studentId == null || !studentId.startsWith(Role.STUDENT.getIdPrefix())) {
            throw new IllegalArgumentException("The student portal needs a student ID (S###).");
        }
        this.studentId = studentId;
    }

    /** Today's date. Kept in one method so the whole module agrees on "today". */
    protected LocalDate today() {
        return LocalDate.now();
    }

    public String getStudentId() {
        return studentId;
    }

    // =====================================================================
    // Profile
    // =====================================================================

    public User getAccount() {
        return users.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Your account was not found in users.txt."));
    }

    /** May be empty if the Administrator has not created a profile yet. */
    public Optional<StudentProfile> getProfile() {
        return profiles.findById(studentId);
    }

    /**
     * Updates the details a student may change themselves. Username, discipline,
     * belt rank and join date are managed by staff and cannot be changed here.
     */
    public void updateProfile(String name, String phone, String email, String emergencyContact) {
        User account = getAccount();
        // validate everything before writing anything
        account.setName(name);
        account.setPhone(phone);
        account.setEmail(email);
        for (User other : users.findAll()) {
            if (!other.getId().equals(studentId) && other.getEmail().equalsIgnoreCase(account.getEmail())) {
                throw new IllegalArgumentException("That email is already used by another account.");
            }
        }
        StudentProfile profile = getProfile().orElse(null);
        if (profile != null) {
            profile.setEmergencyContact(emergencyContact);
        }
        if (!users.update(account)) {
            throw new IllegalStateException("Could not save your account details. Please try again.");
        }
        if (profile != null && !profiles.update(profile)) {
            throw new IllegalStateException("Account saved, but the emergency contact could not be saved.");
        }
        ActivityLog.log(studentId, "UPDATE_PROFILE", "Student updated own profile");
    }

    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        User account = getAccount();
        if (currentPassword == null || !currentPassword.equals(account.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("The new passwords do not match.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("The new password must be different from the current one.");
        }
        account.setPassword(newPassword);
        if (!users.update(account)) {
            throw new IllegalStateException("Could not save your new password. Please try again.");
        }
        ActivityLog.log(studentId, "CHANGE_PASSWORD", "Student changed own password");
    }

    // =====================================================================
    // Classes
    // =====================================================================

    /** One class as the student sees it: details plus instructor name and free seats. */
    public static class ClassView {
        private final TrainingClass trainingClass;
        private final String instructorName;
        private final int seatsLeft;
        private final boolean alreadyBooked;

        ClassView(TrainingClass trainingClass, String instructorName, int seatsLeft, boolean alreadyBooked) {
            this.trainingClass = trainingClass;
            this.instructorName = instructorName;
            this.seatsLeft = seatsLeft;
            this.alreadyBooked = alreadyBooked;
        }

        public TrainingClass getTrainingClass() { return trainingClass; }
        public String getInstructorName() { return instructorName; }
        public int getSeatsLeft() { return seatsLeft; }
        public boolean isAlreadyBooked() { return alreadyBooked; }
    }

    private List<TrainingClass> allClasses() {
        try {
            return classService.getAllClasses();
        } catch (IOException e) {
            System.err.println("Could not read classes: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Optional<TrainingClass> findClass(String classId) {
        for (TrainingClass c : allClasses()) {
            if (c.getClassId().equalsIgnoreCase(classId)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private Map<String, String> userNames() {
        Map<String, String> names = new HashMap<>();
        for (User u : users.findAll()) {
            names.put(u.getId(), u.getName());
        }
        return names;
    }

    /** Number of bookings (by anyone) that still hold a seat in the class. */
    private int seatsTaken(String classId, List<Booking> all) {
        int taken = 0;
        for (Booking b : all) {
            if (b.getClassId().equalsIgnoreCase(classId) && b.getStatus().isActive()) {
                taken++;
            }
        }
        return taken;
    }

    private static LocalDate dateOf(TrainingClass c) {
        return Validator.parseDate(c.getDate());
    }

    private boolean isUpcoming(TrainingClass c) {
        LocalDate d = dateOf(c);
        return d != null && d.isAfter(today());
    }

    private boolean isBookable(TrainingClass c) {
        return c.getStatus() != null && BOOKABLE_CLASS_STATUSES.contains(c.getStatus().trim().toUpperCase())
                && isUpcoming(c);
    }

    /**
     * Upcoming classes that accept bookings, earliest first.
     *
     * @param keyword    matches class ID, name, discipline or instructor (blank = all)
     * @param discipline exact discipline, or null / "All" for every discipline
     */
    public List<ClassView> searchAvailableClasses(String keyword, String discipline) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        Map<String, String> names = userNames();
        List<Booking> allBookings = bookings.findAll();
        List<ClassView> result = new ArrayList<>();
        for (TrainingClass c : allClasses()) {
            if (!isBookable(c)) {
                continue;
            }
            if (discipline != null && !discipline.equals("All") && !discipline.equalsIgnoreCase(c.getDiscipline())) {
                continue;
            }
            String instructor = names.getOrDefault(c.getInstructorId(), c.getInstructorId());
            boolean match = k.isEmpty()
                    || c.getClassId().toLowerCase().contains(k)
                    || c.getClassName().toLowerCase().contains(k)
                    || c.getDiscipline().toLowerCase().contains(k)
                    || instructor.toLowerCase().contains(k);
            if (!match) {
                continue;
            }
            int left = Math.max(0, c.getCapacity() - seatsTaken(c.getClassId(), allBookings));
            result.add(new ClassView(c, instructor, left, hasActiveBooking(c.getClassId(), allBookings)));
        }
        result.sort(Comparator.comparing((ClassView v) -> v.getTrainingClass().getDate())
                .thenComparing(v -> v.getTrainingClass().getStartTime()));
        return result;
    }

    /** Disciplines that currently have bookable classes, for the filter box. */
    public List<String> availableDisciplines() {
        List<String> list = new ArrayList<>();
        for (TrainingClass c : allClasses()) {
            if (isBookable(c) && !list.contains(c.getDiscipline())) {
                list.add(c.getDiscipline());
            }
        }
        Collections.sort(list);
        return list;
    }

    private boolean hasActiveBooking(String classId, List<Booking> all) {
        for (Booking b : all) {
            if (b.getStudentId().equals(studentId) && b.getClassId().equalsIgnoreCase(classId)
                    && b.getStatus().isActive()) {
                return true;
            }
        }
        return false;
    }

    /** True if the new class overlaps in time with another class the student has booked. */
    private TrainingClass findClash(TrainingClass target, String ignoreBookingId) {
        for (Booking b : myBookingsRaw()) {
            if (!b.getStatus().isActive() || b.getId().equals(ignoreBookingId)) {
                continue;
            }
            TrainingClass other = findClass(b.getClassId()).orElse(null);
            if (other == null || !other.getDate().equals(target.getDate())) {
                continue;
            }
            // same day: overlap if start < otherEnd and otherStart < end (HH:mm compares as text)
            if (target.getStartTime().compareTo(other.getEndTime()) < 0
                    && other.getStartTime().compareTo(target.getEndTime()) < 0) {
                return other;
            }
        }
        return null;
    }

    // =====================================================================
    // Bookings
    // =====================================================================

    /** One booking as the student sees it. */
    public static class BookingView {
        private final Booking booking;
        private final TrainingClass trainingClass;   // null if the class no longer exists
        private final String instructorName;

        BookingView(Booking booking, TrainingClass trainingClass, String instructorName) {
            this.booking = booking;
            this.trainingClass = trainingClass;
            this.instructorName = instructorName;
        }

        public Booking getBooking() { return booking; }
        public TrainingClass getTrainingClass() { return trainingClass; }
        public String getInstructorName() { return instructorName; }
    }

    private List<Booking> myBookingsRaw() {
        List<Booking> mine = new ArrayList<>();
        for (Booking b : bookings.findAll()) {
            if (b.getStudentId().equals(studentId)) {
                mine.add(b);
            }
        }
        return mine;
    }

    /** The student's bookings, newest first. */
    public List<BookingView> getMyBookings() {
        Map<String, String> names = userNames();
        List<BookingView> result = new ArrayList<>();
        for (Booking b : myBookingsRaw()) {
            TrainingClass c = findClass(b.getClassId()).orElse(null);
            String instructor = c == null ? "-" : names.getOrDefault(c.getInstructorId(), c.getInstructorId());
            result.add(new BookingView(b, c, instructor));
        }
        result.sort((a, b) -> b.getBooking().getBookingDate().compareTo(a.getBooking().getBookingDate()));
        return result;
    }

    private Booking myBooking(String bookingId) {
        Booking b = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking " + bookingId + " was not found."));
        if (!b.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("You can only manage your own bookings.");
        }
        return b;
    }

    /**
     * Books a seat and raises an UNPAID payment for the class fee.
     *
     * @return the new booking
     */
    public Booking bookClass(String classId) {
        TrainingClass c = findClass(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class " + classId + " was not found."));
        if (!isBookable(c)) {
            throw new IllegalStateException("This class is not open for booking (it may be full, past or cancelled).");
        }
        List<Booking> all = bookings.findAll();
        if (hasActiveBooking(c.getClassId(), all)) {
            throw new IllegalStateException("You have already booked this class.");
        }
        if (seatsTaken(c.getClassId(), all) >= c.getCapacity()) {
            throw new IllegalStateException("Sorry, this class is full.");
        }
        TrainingClass clash = findClash(c, null);
        if (clash != null) {
            throw new IllegalStateException("This class clashes with " + clash.getClassName()
                    + " (" + clash.getDate() + " " + clash.getStartTime() + "-" + clash.getEndTime() + ").");
        }

        Booking booking = new Booking(IdGenerator.nextId(DataFiles.BOOKINGS, "B"), studentId, c.getClassId(),
                today(), BookingStatus.BOOKED, PaymentStatus.UNPAID);
        Payment payment = new Payment(IdGenerator.nextId(DataFiles.PAYMENTS, "P"), studentId, booking.getId(),
                c.getClassId(), fee(c), today(), null, PaymentStatus.UNPAID);

        if (!bookings.add(booking)) {
            throw new IllegalStateException("Could not save the booking. Please try again.");
        }
        if (!payments.add(payment)) {
            bookings.delete(booking.getId());   // roll back so no booking exists without a payment
            throw new IllegalStateException("Could not create the payment. The booking was not made.");
        }
        ActivityLog.log(studentId, "BOOK_CLASS", booking.getId() + " for " + c.getClassId());
        return booking;
    }

    private static BigDecimal fee(TrainingClass c) {
        return BigDecimal.valueOf(c.getFee()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Bookings can be cancelled or rescheduled until the day before the class. */
    private void requireChangeable(Booking b) {
        if (!b.getStatus().isActive()) {
            throw new IllegalStateException("Only BOOKED or RESCHEDULED bookings can be changed.");
        }
        TrainingClass c = findClass(b.getClassId()).orElse(null);
        if (c == null) {
            throw new IllegalStateException("The class for this booking no longer exists. "
                    + "Please contact the Administrator.");
        }
        if (!isUpcoming(c)) {
            throw new IllegalStateException("Bookings can only be changed up to the day before the class.");
        }
    }

    /**
     * Cancels a booking. A paid booking is marked REFUNDED; an unpaid one has
     * its pending payment removed.
     */
    public void cancelBooking(String bookingId) {
        Booking b = myBooking(bookingId);
        requireChangeable(b);
        Payment p = paymentForBooking(b.getId());

        b.setStatus(BookingStatus.CANCELLED);
        if (b.getPaymentStatus() == PaymentStatus.PAID) {
            b.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        if (!bookings.update(b)) {
            throw new IllegalStateException("Could not cancel the booking. Please try again.");
        }
        if (p != null) {
            if (p.getStatus() == PaymentStatus.PAID) {
                p.setStatus(PaymentStatus.REFUNDED);
                payments.update(p);
            } else if (p.getStatus() == PaymentStatus.UNPAID) {
                payments.delete(p.getId());
            }
        }
        ActivityLog.log(studentId, "CANCEL_BOOKING", b.getId());
    }

    /** Upcoming classes of the same discipline the booking could move to. */
    public List<ClassView> rescheduleOptions(String bookingId) {
        Booking b = myBooking(bookingId);
        TrainingClass current = findClass(b.getClassId()).orElse(null);
        List<ClassView> options = new ArrayList<>();
        for (ClassView v : searchAvailableClasses("", current == null ? null : current.getDiscipline())) {
            if (!v.getTrainingClass().getClassId().equalsIgnoreCase(b.getClassId())
                    && !v.isAlreadyBooked() && v.getSeatsLeft() > 0) {
                options.add(v);
            }
        }
        return options;
    }

    /**
     * Moves a booking to another upcoming class of the same discipline.
     * A paid booking can only move to a class with the same fee; an unpaid
     * booking's payment is updated to the new class fee.
     */
    public void rescheduleBooking(String bookingId, String newClassId) {
        Booking b = myBooking(bookingId);
        requireChangeable(b);
        TrainingClass target = findClass(newClassId)
                .orElseThrow(() -> new IllegalArgumentException("Class " + newClassId + " was not found."));
        TrainingClass current = findClass(b.getClassId()).orElse(null);

        if (target.getClassId().equalsIgnoreCase(b.getClassId())) {
            throw new IllegalArgumentException("Please choose a different class.");
        }
        if (!isBookable(target)) {
            throw new IllegalStateException("The new class is not open for booking.");
        }
        if (current != null && !current.getDiscipline().equalsIgnoreCase(target.getDiscipline())) {
            throw new IllegalArgumentException("You can only reschedule to a class of the same discipline.");
        }
        List<Booking> all = bookings.findAll();
        if (hasActiveBooking(target.getClassId(), all)) {
            throw new IllegalStateException("You have already booked the new class.");
        }
        if (seatsTaken(target.getClassId(), all) >= target.getCapacity()) {
            throw new IllegalStateException("The new class is full.");
        }
        TrainingClass clash = findClash(target, b.getId());
        if (clash != null) {
            throw new IllegalStateException("The new class clashes with " + clash.getClassName() + ".");
        }
        Payment p = paymentForBooking(b.getId());
        if (b.getPaymentStatus() == PaymentStatus.PAID && current != null
                && fee(current).compareTo(fee(target)) != 0) {
            throw new IllegalStateException("This booking is already paid, so it can only move to a class with "
                    + "the same fee (RM " + fee(current) + ").");
        }

        b.setClassId(target.getClassId());
        b.setStatus(BookingStatus.RESCHEDULED);
        if (!bookings.update(b)) {
            throw new IllegalStateException("Could not reschedule the booking. Please try again.");
        }
        if (p != null) {
            p.setClassId(target.getClassId());
            if (p.getStatus() == PaymentStatus.UNPAID) {
                p.setAmount(fee(target));
            }
            payments.update(p);
        }
        ActivityLog.log(studentId, "RESCHEDULE_BOOKING", b.getId() + " to " + target.getClassId());
    }

    // =====================================================================
    // Payments
    // =====================================================================

    private Payment paymentForBooking(String bookingId) {
        for (Payment p : payments.findAll()) {
            if (p.getBookingId().equals(bookingId) && p.getStudentId().equals(studentId)) {
                return p;
            }
        }
        return null;
    }

    /** The student's payments, newest first. */
    public List<Payment> getMyPayments() {
        List<Payment> mine = new ArrayList<>();
        for (Payment p : payments.findAll()) {
            if (p.getStudentId().equals(studentId)) {
                mine.add(p);
            }
        }
        mine.sort((a, b) -> b.getPaymentDate().compareTo(a.getPaymentDate()));
        return mine;
    }

    /** Total still owed on unpaid payments for active bookings. */
    public BigDecimal outstandingBalance() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Payment p : getMyPayments()) {
            if (p.getStatus() == PaymentStatus.UNPAID) {
                sum = sum.add(p.getAmount());
            }
        }
        return sum;
    }

    public BigDecimal totalPaid() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Payment p : getMyPayments()) {
            if (p.getStatus() == PaymentStatus.PAID) {
                sum = sum.add(p.getAmount());
            }
        }
        return sum;
    }

    /** Pays an UNPAID payment and marks its booking as PAID. */
    public void makePayment(String paymentId, PaymentMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("Please choose a payment method.");
        }
        Payment p = payments.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment " + paymentId + " was not found."));
        if (!p.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("You can only pay your own payments.");
        }
        if (p.getStatus() != PaymentStatus.UNPAID) {
            throw new IllegalStateException("This payment is already " + p.getStatus() + ".");
        }
        Booking b = bookings.findById(p.getBookingId()).orElse(null);
        if (b != null && !b.getStatus().isActive()) {
            throw new IllegalStateException("The booking for this payment is " + b.getStatus() + ".");
        }
        p.setStatus(PaymentStatus.PAID);
        p.setMethod(method.name());
        p.setPaymentDate(today());
        if (!payments.update(p)) {
            throw new IllegalStateException("Could not save the payment. Please try again.");
        }
        if (b != null) {
            b.setPaymentStatus(PaymentStatus.PAID);
            bookings.update(b);
        }
        ActivityLog.log(studentId, "MAKE_PAYMENT", p.getId() + " RM " + p.getAmount() + " by " + method.name());
    }

    // =====================================================================
    // Attendance
    // =====================================================================

    /** One attendance line with the class name filled in. */
    public static class AttendanceView {
        private final AttendanceRecord record;
        private final String className;

        AttendanceView(AttendanceRecord record, String className) {
            this.record = record;
            this.className = className;
        }

        public AttendanceRecord getRecord() { return record; }
        public String getClassName() { return className; }
    }

    public List<AttendanceView> getMyAttendance() {
        List<AttendanceView> result = new ArrayList<>();
        try {
            for (AttendanceRecord r : attendanceService.searchByStudentId(studentId)) {
                String name = findClass(r.getClassId()).map(TrainingClass::getClassName).orElse("-");
                result.add(new AttendanceView(r, name));
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not read attendance: " + e.getMessage());
        }
        result.sort((a, b) -> b.getRecord().getDate().compareTo(a.getRecord().getDate()));
        return result;
    }

    /** Percentage of attendance records marked PRESENT (or LATE), 0 if none. */
    public int attendanceRate() {
        List<AttendanceView> all = getMyAttendance();
        if (all.isEmpty()) {
            return 0;
        }
        int present = 0;
        for (AttendanceView v : all) {
            String s = v.getRecord().getStatus().toUpperCase();
            if (s.equals("PRESENT") || s.equals("LATE")) {
                present++;
            }
        }
        return Math.round(present * 100f / all.size());
    }

    // =====================================================================
    // Ratings
    // =====================================================================

    /** Classes the student took (attended, or booked and already held) and has not rated yet. */
    public List<TrainingClass> classesToRate() {
        List<String> taken = new ArrayList<>();
        try {
            for (AttendanceRecord r : attendanceService.searchByStudentId(studentId)) {
                if (!r.getStatus().equalsIgnoreCase("ABSENT")) {
                    taken.add(r.getClassId().toUpperCase());
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not read attendance: " + e.getMessage());
        }
        for (Booking b : myBookingsRaw()) {
            if (b.getStatus() == BookingStatus.ATTENDED) {
                taken.add(b.getClassId().toUpperCase());
            } else if (b.getStatus().isActive()) {
                findClass(b.getClassId()).ifPresent(c -> {
                    LocalDate d = dateOf(c);
                    if (d != null && d.isBefore(today())) {
                        taken.add(c.getClassId().toUpperCase());
                    }
                });
            }
        }
        List<String> rated = new ArrayList<>();
        for (Rating r : getMyRatings()) {
            rated.add(r.getClassId().toUpperCase());
        }
        List<TrainingClass> result = new ArrayList<>();
        for (TrainingClass c : allClasses()) {
            String id = c.getClassId().toUpperCase();
            if (taken.contains(id) && !rated.contains(id)) {
                result.add(c);
            }
        }
        return result;
    }

    public List<Rating> getMyRatings() {
        List<Rating> mine = new ArrayList<>();
        for (Rating r : ratings.findAll()) {
            if (r.getStudentId().equals(studentId)) {
                mine.add(r);
            }
        }
        mine.sort((a, b) -> b.getRatingDate().compareTo(a.getRatingDate()));
        return mine;
    }

    public Rating rateClass(String classId, int stars, String comment) {
        boolean allowed = false;
        for (TrainingClass c : classesToRate()) {
            if (c.getClassId().equalsIgnoreCase(classId)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new IllegalStateException("You can only rate a class you have taken, once.");
        }
        TrainingClass c = findClass(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class " + classId + " was not found."));
        Rating rating = new Rating(IdGenerator.nextId(DataFiles.RATINGS, "R"), studentId, c.getClassId(),
                c.getInstructorId(), stars, comment, today());
        if (!ratings.add(rating)) {
            throw new IllegalStateException("Could not save your rating. Please try again.");
        }
        ActivityLog.log(studentId, "RATE_CLASS", rating.getId() + " " + stars + " stars for " + c.getClassId());
        return rating;
    }

    public String className(String classId) {
        return findClass(classId).map(TrainingClass::getClassName).orElse("-");
    }
}
