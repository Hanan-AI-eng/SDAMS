package sdams.facilities.model;

/**
 * A facility problem reported by an Instructor (damaged mat, broken light, etc.).
 *
 * Notice there are NO setters for id, description, location, dateReported or reportedBy.
 * Those belong to the Instructor's report. The Facilities Officer may only change
 * status, remarks and dateUpdated, so the class itself enforces the permission rule.
 */
public class FacilityProblem {

    public static final String REPORTED = "Reported";
    public static final String IN_PROGRESS = "In Progress";
    public static final String RESOLVED = "Resolved";
    public static final String[] STATUSES = {REPORTED, IN_PROGRESS, RESOLVED};

    private final String problemId;
    private final String description;
    private final String location;
    private final String dateReported;   // stored as yyyy-MM-dd text, e.g. 2026-10-20
    private final String reportedBy;     // the Instructor's ID or name
    private String status;
    private String remarks;
    private String dateUpdated;

    public FacilityProblem(String problemId, String description, String location,
                           String dateReported, String reportedBy,
                           String status, String remarks, String dateUpdated) {
        this.problemId = problemId;
        this.description = description;
        this.location = location;
        this.dateReported = dateReported;
        this.reportedBy = reportedBy;
        this.status = status;
        this.remarks = remarks;
        this.dateUpdated = dateUpdated;
    }

    public String getProblemId() { return problemId; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getDateReported() { return dateReported; }
    public String getReportedBy() { return reportedBy; }
    public String getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public String getDateUpdated() { return dateUpdated; }

    public void setStatus(String status) { this.status = status; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setDateUpdated(String dateUpdated) { this.dateUpdated = dateUpdated; }
}
