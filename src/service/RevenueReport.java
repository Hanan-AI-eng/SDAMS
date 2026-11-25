package service;

import model.User;
import model.UserFactory;
import util.DataFiles;
import util.FileHandler;
import util.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Revenue Report (owner: S1). Kept separate from the GUI.
 *
 * Rules (assumptions):
 *  - Only payments with status PAID count. UNPAID and REFUNDED are excluded.
 *  - One PAID payment = one class registration.
 *  - Month and year come from the payment date (LocalDate, never string compare).
 *
 * payments.txt: paymentId|studentId|bookingId|classId|amount|paymentDate|method|status
 */
public class RevenueReport implements Reportable {

    public enum Period { MONTHLY, YEARLY }

    /** One table row: either one payment (monthly) or one month (yearly). */
    public static class Row {
        private final String[] cells;
        private final int registrations;
        private final BigDecimal amount;

        Row(String[] cells, int registrations, BigDecimal amount) {
            this.cells = cells;
            this.registrations = registrations;
            this.amount = amount;
        }

        public String[] getCells() { return cells.clone(); }
        public int getRegistrations() { return registrations; }
        public BigDecimal getAmount() { return amount; }
    }

    /** A PAID payment read from the file. */
    private static class Paid {
        String paymentId, studentId, classId;
        LocalDate date;
        BigDecimal amount;
    }

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Period period = Period.MONTHLY;
    private int year;
    private int month;
    private final List<Row> rows = new ArrayList<>();
    private int totalRegistrations;
    private BigDecimal totalRevenue = ZERO;

    // ---------------- generate ----------------

    /** Monthly view: one row per PAID payment in that month. */
    public List<Row> generateMonthly(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be 1-12.");
        }
        reset(Period.MONTHLY, year, month);
        Map<String, String> names = studentNames();
        for (Paid p : loadPaid()) {
            if (p.date.getYear() == year && p.date.getMonthValue() == month) {
                rows.add(new Row(new String[]{p.paymentId, p.date.toString(), p.studentId,
                        names.getOrDefault(p.studentId, "(unknown)"), p.classId, money(p.amount)},
                        1, p.amount));
                totalRegistrations++;
                totalRevenue = totalRevenue.add(p.amount);
            }
        }
        rows.sort((a, b) -> a.cells[1].compareTo(b.cells[1])); // ISO dates sort correctly
        return rows;
    }

    /** Yearly view: one row per month (all 12 months, zero where there were no payments). */
    public List<Row> generateYearly(int year) {
        reset(Period.YEARLY, year, 0);
        int[] count = new int[13];
        BigDecimal[] sum = new BigDecimal[13];
        for (int m = 1; m <= 12; m++) {
            sum[m] = ZERO;
        }
        for (Paid p : loadPaid()) {
            if (p.date.getYear() == year) {
                int m = p.date.getMonthValue();
                count[m]++;
                sum[m] = sum[m].add(p.amount);
            }
        }
        for (int m = 1; m <= 12; m++) {
            rows.add(new Row(new String[]{monthName(m), String.valueOf(count[m]), money(sum[m])},
                    count[m], sum[m]));
            totalRegistrations += count[m];
            totalRevenue = totalRevenue.add(sum[m]);
        }
        return rows;
    }

    // ---------------- results ----------------

    public String[] getColumns() {
        return period == Period.MONTHLY
                ? new String[]{"Payment ID", "Date", "Student ID", "Student name", "Class ID", "Amount (RM)"}
                : new String[]{"Month", "Registrations", "Revenue (RM)"};
    }

    /** The TOTAL row shown at the bottom of the table. */
    public String[] getTotalRow() {
        return period == Period.MONTHLY
                ? new String[]{"TOTAL", "", "", totalRegistrations + " registration(s)", "", money(totalRevenue)}
                : new String[]{"TOTAL", String.valueOf(totalRegistrations), money(totalRevenue)};
    }

    public List<Row> getRows() { return rows; }
    public int getTotalRegistrations() { return totalRegistrations; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public boolean hasRecords() { return totalRegistrations > 0; }
    public Period getPeriod() { return period; }

    /** Labels and values for the bar chart: months (yearly) or classes (monthly). */
    public Map<String, BigDecimal> chartData() {
        Map<String, BigDecimal> data = new java.util.LinkedHashMap<>();
        if (period == Period.YEARLY) {
            for (int m = 1; m <= 12; m++) {
                data.put(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), rows.get(m - 1).amount);
            }
        } else {
            for (Row r : rows) {
                data.merge(r.cells[4], r.amount, BigDecimal::add);   // revenue per class
            }
        }
        return data;
    }

    /** Years that appear in payments.txt, plus the current year, newest first. */
    public static List<Integer> availableYears() {
        TreeSet<Integer> years = new TreeSet<>();
        years.add(LocalDate.now().getYear());
        for (Paid p : loadAllPayments(false)) {
            years.add(p.date.getYear());
        }
        List<Integer> list = new ArrayList<>(years.descendingSet());
        return list;
    }

    public static String monthName(int m) {
        return Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ---------------- Reportable ----------------

    @Override
    public String getTitle() {
        return period == Period.MONTHLY
                ? "Revenue Report - " + monthName(month) + " " + year
                : "Revenue Report - Year " + year;
    }

    @Override
    public List<String> toReportLines() {
        List<String> out = new ArrayList<>();
        out.add("SDAMS " + getTitle() + "   (generated " + LocalDate.now() + ")");
        out.add("Only PAID payments are counted. One payment = one class registration.");
        out.add("");
        String[] cols = getColumns();
        String fmt = period == Period.MONTHLY ? "%-10s %-11s %-10s %-22.22s %-8s %12s" : "%-12s %13s %14s";
        out.add(String.format(fmt, (Object[]) cols));
        for (Row r : rows) {
            out.add(String.format(fmt, (Object[]) r.cells));
        }
        if (!hasRecords()) {
            out.add("No records for this period.");
        }
        out.add(String.format(fmt, (Object[]) getTotalRow()));
        out.add("");
        out.add("Class registrations: " + totalRegistrations);
        out.add("Total revenue: RM " + money(totalRevenue));
        return out;
    }

    // ---------------- helpers ----------------

    private void reset(Period period, int year, int month) {
        this.period = period;
        this.year = year;
        this.month = month;
        rows.clear();
        totalRegistrations = 0;
        totalRevenue = ZERO;
    }

    private static List<Paid> loadPaid() {
        return loadAllPayments(true);
    }

    private static List<Paid> loadAllPayments(boolean paidOnly) {
        List<Paid> list = new ArrayList<>();
        for (String line : FileHandler.readAllLines(DataFiles.PAYMENTS)) {
            String[] f = FileHandler.split(line);
            if (f.length < 8) {
                continue;
            }
            if (paidOnly && !f[7].trim().equalsIgnoreCase("PAID")) {
                continue;
            }
            try {
                Paid p = new Paid();
                p.paymentId = f[0].trim();
                p.studentId = f[1].trim();
                p.classId = f[3].trim();
                p.amount = new BigDecimal(f[4].trim()).setScale(2, RoundingMode.HALF_UP);
                p.date = LocalDate.parse(f[5].trim());
                list.add(p);
            } catch (RuntimeException e) {
                System.err.println("Skipping bad payment line: " + line);
            }
        }
        return list;
    }

    private static Map<String, String> studentNames() {
        Map<String, String> names = new HashMap<>();
        for (User u : new Repository<>(DataFiles.USERS, UserFactory::fromFileLine).findAll()) {
            names.put(u.getId(), u.getName());
        }
        return names;
    }
}
