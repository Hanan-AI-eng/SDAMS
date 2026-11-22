package model;

import util.FileHandler;
import util.Validator;

import java.time.LocalDate;

/**
 * A student's booking for one training class (owner: S3, Student).
 *
 * bookings.txt: bookingId | studentId | classId | bookingDate | status | paymentStatus
 */
public class Booking extends Record {

    private final String studentId;
    private String classId;
    private final LocalDate bookingDate;
    private BookingStatus status;
    private PaymentStatus paymentStatus;

    public Booking(String bookingId, String studentId, String classId, LocalDate bookingDate,
                   BookingStatus status, PaymentStatus paymentStatus) {
        super(bookingId);
        if (studentId == null || !studentId.startsWith(Role.STUDENT.getIdPrefix())) {
            throw new IllegalArgumentException("A booking needs a student ID (S###).");
        }
        if (bookingDate == null) {
            throw new IllegalArgumentException("Booking date is required.");
        }
        this.studentId = studentId;
        this.bookingDate = bookingDate;
        setClassId(classId);
        setStatus(status);
        setPaymentStatus(paymentStatus);
    }

    public static Booking fromFileLine(String line) {
        String[] f = FileHandler.split(line);
        if (f.length < 6) {
            throw new IllegalArgumentException("bookings.txt line needs 6 fields");
        }
        LocalDate date = Validator.parseDate(f[3]);
        if (date == null) {
            throw new IllegalArgumentException("Bad date: " + f[3]);
        }
        return new Booking(f[0].trim(), f[1].trim(), f[2].trim(), date,
                BookingStatus.valueOf(f[4].trim()), PaymentStatus.valueOf(f[5].trim()));
    }

    @Override
    public String toFileLine() {
        return FileHandler.join(getId(), studentId, classId, bookingDate, status.name(), paymentStatus.name());
    }

    public String getStudentId() { return studentId; }
    public String getClassId() { return classId; }
    public LocalDate getBookingDate() { return bookingDate; }
    public BookingStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    public final void setClassId(String classId) {
        this.classId = Validator.requireText(classId, "Class ID", 10);
    }

    public final void setStatus(BookingStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Booking status is required.");
        }
        this.status = status;
    }

    public final void setPaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            throw new IllegalArgumentException("Payment status is required.");
        }
        this.paymentStatus = paymentStatus;
    }
}
