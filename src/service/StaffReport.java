package service;

import model.Role;
import model.User;
import model.UserFactory;
import util.DataFiles;
import util.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Staff Report (owner: S1): Administrators, Instructors and Facilities
 * Officers, the roles the brief lists. Filter by role and name; totals per role.
 */
public class StaffReport implements Reportable {

    public static final List<Role> REPORT_ROLES =
            Arrays.asList(Role.ADMINISTRATOR, Role.INSTRUCTOR, Role.FACILITIES_OFFICER);

    private final Repository<User> users = new Repository<>(DataFiles.USERS, UserFactory::fromFileLine);

    private Role roleFilter;          // null = all report roles
    private String nameFilter = "";
    private List<User> rows = new ArrayList<>();

    /** Runs the report. roleFilter null means all three roles. */
    public List<User> generate(Role roleFilter, String nameFilter) {
        if (roleFilter != null && !REPORT_ROLES.contains(roleFilter)) {
            throw new IllegalArgumentException("Role " + roleFilter + " is not part of the staff report.");
        }
        this.roleFilter = roleFilter;
        this.nameFilter = nameFilter == null ? "" : nameFilter.trim();
        String k = this.nameFilter.toLowerCase();
        rows = new ArrayList<>();
        for (User u : users.findAll()) {
            boolean roleOk = roleFilter == null ? REPORT_ROLES.contains(u.getRole()) : u.getRole() == roleFilter;
            boolean nameOk = k.isEmpty() || u.getName().toLowerCase().contains(k)
                    || u.getId().toLowerCase().contains(k);
            if (roleOk && nameOk) {
                rows.add(u);
            }
        }
        return rows;
    }

    /** Count of the current rows for each role (0 for roles with none). */
    public Map<Role, Integer> totalsByRole() {
        Map<Role, Integer> totals = new EnumMap<>(Role.class);
        for (Role r : REPORT_ROLES) {
            totals.put(r, 0);
        }
        for (User u : rows) {
            totals.merge(u.getRole(), 1, Integer::sum);
        }
        return totals;
    }

    public int total() {
        return rows.size();
    }

    @Override
    public String getTitle() {
        return "Staff Report";
    }

    @Override
    public List<String> toReportLines() {
        List<String> out = new ArrayList<>();
        out.add("SDAMS " + getTitle() + "   (generated " + LocalDate.now() + ")");
        out.add("Role: " + (roleFilter == null ? "All" : roleFilter.getDisplayName())
                + "    Name/ID contains: " + (nameFilter.isEmpty() ? "(any)" : nameFilter));
        out.add("");
        out.add(String.format("%-6s %-24s %-20s %-12s %s", "ID", "Name", "Role", "Phone", "Email"));
        for (User u : rows) {
            out.add(String.format("%-6s %-24.24s %-20s %-12s %s",
                    u.getId(), u.getName(), u.getRole().getDisplayName(), u.getPhone(), u.getEmail()));
        }
        out.add("");
        for (Map.Entry<Role, Integer> e : totalsByRole().entrySet()) {
            out.add(String.format("%-20s %d", e.getKey().getDisplayName() + "s:", e.getValue()));
        }
        out.add(String.format("%-20s %d", "Total staff:", total()));
        return out;
    }
}
