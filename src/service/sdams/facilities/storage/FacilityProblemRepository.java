package sdams.facilities.storage;

import sdams.facilities.model.FacilityProblem;

/**
 * Stores FacilityProblem records (PROVISIONAL format: the group must confirm it).
 * File line format:
 *   problemId|description|location|dateReported|reportedBy|status|remarks|dateUpdated
 * Example:
 *   FP001|Mat in Studio A has a torn edge|Studio A|2026-10-20|INS001|Reported||
 * The Instructor's code creates these lines; the Facilities Officer only updates
 * status, remarks and dateUpdated.
 */
public class FacilityProblemRepository extends TextFileRepository<FacilityProblem> {

    public FacilityProblemRepository(String filePath) {
        super(filePath);
    }

    @Override
    protected FacilityProblem parseLine(String line) {
        String[] f = splitFields(line);
        if (f.length != 8) {
            throw new IllegalArgumentException("Facility problem line must have 8 fields: " + line);
        }
        return new FacilityProblem(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7]);
    }

    @Override
    protected String formatRecord(FacilityProblem p) {
        return String.join(SEPARATOR, p.getProblemId(), p.getDescription(), p.getLocation(),
                p.getDateReported(), p.getReportedBy(), p.getStatus(), p.getRemarks(),
                p.getDateUpdated());
    }
}
