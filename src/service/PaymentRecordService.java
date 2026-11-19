package service;

import model.User;
import model.UserFactory;
import util.ActivityLog;
import util.DataFiles;
import util.FileHandler;
import util.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View-only access to payments.txt, joined with student names from users.txt.
 * The Administrator can filter and export payments but never edit them.
 *
 * payments.txt: paymentId|studentId|bookingId|classId|amount|paymentDate|method|status
 */
public class PaymentRecordService {

    public static final String ALL_STATUSES = "All";
    public static final String[] STATUS_OPTIONS = {ALL_STATUSES, "PAID", "UNPAID", "REFUNDED"};

    private final Repository<User> users = new Repository<>(DataFiles.USERS, UserFactory::fromFileLine);
    private final String actingUserId;

    public PaymentRecordService(String actingUserId) {
        this.actingUserId = actingUserId;
    }

    /** One row of the payment table (read-only). */
    public static class PaymentRow {
        private final String paymentId, studentId, studentName, bookingId, classId, method, status;
        private final BigDecimal amount;
        private final LocalDate paymentDate;

        PaymentRow(String paymentId, String studentId, String studentName, String bookingId,
                   String classId, BigDecimal amount, LocalDate paymentDate, String method, String status) {
            this.paymentId = paymentId;
            this.studentId = studentId;
            this.studentName = studentName;
            this.bookingId = bookingId;
            this.classId = classId;
            this.amount = amount;
            this.paymentDate = paymentDate;
            this.method = method;
            this.status = status;
        }

        public String getPaymentId() { return paymentId; }
        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getBookingId() { return bookingId; }
        public String getClassId() { return classId; }
        public BigDecimal getAmount() { return amount; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public String getMethod() { return method; }
        public String getStatus() { return status; }
    }

    public List<PaymentRow> getAllPayments() {
        Map<String, String> names = new HashMap<>();
        for (User u : users.findAll()) {
            names.put(u.getId(), u.getName());
        }
        List<PaymentRow> rows = new ArrayList<>();
        for (String line : FileHandler.readAllLines(DataFiles.PAYMENTS)) {
            String[] f = FileHandler.split(line);
            if (f.length < 8) {
                System.err.println("Skipping bad payment line: " + line);
                continue;
            }
            try {
                BigDecimal amount = new BigDecimal(f[4].trim()).setScale(2, RoundingMode.HALF_UP);
                LocalDate date = LocalDate.parse(f[5].trim());
                String studentId = f[1].trim();
                rows.add(new PaymentRow(f[0].trim(), studentId,
                        names.getOrDefault(studentId, "(unknown student)"),
                        f[2].trim(), f[3].trim(), amount, date, f[6].trim(), f[7].trim().toUpperCase()));
            } catch (RuntimeException e) {
                System.err.println("Skipping bad payment line: " + line);
            }
        }
        return rows;
    }

    /**
     * Filters payments. Any argument may be null/blank to skip that filter.
     *
     * @throws IllegalArgumentException if "from" is after "to"
     */
    public List<PaymentRow> filter(String studentKeyword, LocalDate from, LocalDate to, String status) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The \"From\" date must not be after the \"To\" date.");
        }
        String keyword = studentKeyword == null ? "" : studentKeyword.trim().toLowerCase();
        List<PaymentRow> result = new ArrayList<>();
        for (PaymentRow r : getAllPayments()) {
            boolean studentOk = keyword.isEmpty()
                    || r.getStudentId().toLowerCase().contains(keyword)
                    || r.getStudentName().toLowerCase().contains(keyword);
            boolean fromOk = from == null || !r.getPaymentDate().isBefore(from);
            boolean toOk = to == null || !r.getPaymentDate().isAfter(to);
            boolean statusOk = status == null || status.equals(ALL_STATUSES) || r.getStatus().equals(status);
            if (studentOk && fromOk && toOk && statusOk) {
                result.add(r);
            }
        }
        return result;
    }

    public BigDecimal total(List<PaymentRow> rows) {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (PaymentRow r : rows) {
            sum = sum.add(r.getAmount());
        }
        return sum;
    }

    /** Exports the given rows to a readable text report (extra feature). */
    public Path exportToFile(List<PaymentRow> rows, String filterSummary) throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path dir = java.nio.file.Paths.get(FileHandler.DATA_DIR, "exports");
        Files.createDirectories(dir);
        Path file = dir.resolve("payment_records_" + stamp + ".txt");

        List<String> out = new ArrayList<>();
        out.add("SDAMS Payment Records");
        out.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " by " + actingUserId);
        out.add("Filters: " + filterSummary);
        out.add("");
        out.add(String.format("%-8s %-6s %-22s %-8s %-7s %10s  %-10s %-6s %-8s",
                "Payment", "Stud.", "Student name", "Booking", "Class", "Amount", "Date", "Method", "Status"));
        for (PaymentRow r : rows) {
            out.add(String.format("%-8s %-6s %-22.22s %-8s %-7s %10s  %-10s %-6s %-8s",
                    r.getPaymentId(), r.getStudentId(), r.getStudentName(), r.getBookingId(),
                    r.getClassId(), r.getAmount(), r.getPaymentDate(), r.getMethod(), r.getStatus()));
        }
        out.add("");
        out.add("Records: " + rows.size() + "    Total: RM " + total(rows));
        Files.write(file, out, StandardCharsets.UTF_8);
        ActivityLog.log(actingUserId, "EXPORT_PAYMENTS", file.getFileName() + " (" + rows.size() + " rows)");
        return file;
    }
}
