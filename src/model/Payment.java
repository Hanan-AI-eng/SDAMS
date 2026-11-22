package model;

import util.FileHandler;
import util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * One payment for one booking (owner: S3, Student).
 *
 * payments.txt: paymentId | studentId | bookingId | classId | amount | paymentDate | method | status
 * While a payment is UNPAID, paymentDate is the date it was raised and method is "-".
 */
public class Payment extends Record {

    private final String studentId;
    private final String bookingId;
    private String classId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String method;
    private PaymentStatus status;

    public Payment(String paymentId, String studentId, String bookingId, String classId,
                   BigDecimal amount, LocalDate paymentDate, String method, PaymentStatus status) {
        super(paymentId);
        this.studentId = Validator.requireText(studentId, "Student ID", 10);
        this.bookingId = Validator.requireText(bookingId, "Booking ID", 10);
        setClassId(classId);
        setAmount(amount);
        setPaymentDate(paymentDate);
        setMethod(method);
        setStatus(status);
    }

    public static Payment fromFileLine(String line) {
        String[] f = FileHandler.split(line);
        if (f.length < 8) {
            throw new IllegalArgumentException("payments.txt line needs 8 fields");
        }
        LocalDate date = Validator.parseDate(f[5]);
        if (date == null) {
            throw new IllegalArgumentException("Bad date: " + f[5]);
        }
        return new Payment(f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim(),
                new BigDecimal(f[4].trim()), date, f[6].trim(), PaymentStatus.valueOf(f[7].trim()));
    }

    @Override
    public String toFileLine() {
        return FileHandler.join(getId(), studentId, bookingId, classId, amount.toPlainString(),
                paymentDate, method, status.name());
    }

    public String getStudentId() { return studentId; }
    public String getBookingId() { return bookingId; }
    public String getClassId() { return classId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }

    public final void setClassId(String classId) {
        this.classId = Validator.requireText(classId, "Class ID", 10);
    }

    public final void setAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public final void setPaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date is required.");
        }
        this.paymentDate = paymentDate;
    }

    public final void setMethod(String method) {
        if (Validator.hasPipe(method)) {
            throw new IllegalArgumentException("Payment method must not contain the | character.");
        }
        this.method = Validator.isEmpty(method) ? FileHandler.EMPTY_FIELD : method.trim();
    }

    public final void setStatus(PaymentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Payment status is required.");
        }
        this.status = status;
    }
}
