package sdams.facilities.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import sdams.facilities.model.FacilityProblem;
import sdams.facilities.storage.FacilityProblemRepository;
import sdams.facilities.storage.StorageException;
import sdams.facilities.util.ValidationException;
import sdams.facilities.util.Validator;

/**
 * The rules for Facility Problem Management (view, search, update).
 * There is deliberately no add and no delete method: the Instructor creates problems.
 */
public class FacilityProblemService {

    private static final int MAX_REMARKS = 150;

    private final FacilityProblemRepository repository;

    public FacilityProblemService(FacilityProblemRepository repository) {
        this.repository = repository;
    }

    /** VIEW: all reported problems. */
    public List<FacilityProblem> getAll() throws StorageException {
        return repository.loadAll();
    }

    public int getSkippedLineCount() {
        return repository.getSkippedLineCount();
    }

    /** SEARCH: case-insensitive match on ID, description, location, reporter, status or remarks. */
    public List<FacilityProblem> search(String keyword) throws StorageException {
        String k = (keyword == null) ? "" : keyword.trim().toLowerCase();
        List<FacilityProblem> matches = new ArrayList<>();
        for (FacilityProblem p : repository.loadAll()) {
            if (k.isEmpty()
                    || p.getProblemId().toLowerCase().contains(k)
                    || p.getDescription().toLowerCase().contains(k)
                    || p.getLocation().toLowerCase().contains(k)
                    || p.getReportedBy().toLowerCase().contains(k)
                    || p.getStatus().toLowerCase().contains(k)
                    || p.getRemarks().toLowerCase().contains(k)) {
                matches.add(p);
            }
        }
        return matches;
    }

    /**
     * UPDATE: the only changes the Facilities Officer may make are status and remarks.
     * Rule (recommendation): a problem cannot be marked Resolved without remarks
     * explaining what was done.
     */
    public FacilityProblem update(String problemId, String newStatus, String remarks)
            throws ValidationException, StorageException {
        String status = Validator.requireChoice(newStatus, "Status", FacilityProblem.STATUSES);
        String cleanRemarks = Validator.optionalText(remarks, "Remarks", MAX_REMARKS);
        if (status.equals(FacilityProblem.RESOLVED) && cleanRemarks.isEmpty()) {
            throw new ValidationException("Remarks are required when a problem is marked Resolved "
                    + "(describe what was done).");
        }

        List<FacilityProblem> all = repository.loadAll();
        FacilityProblem target = null;
        for (FacilityProblem p : all) {
            if (p.getProblemId().equalsIgnoreCase(problemId == null ? "" : problemId.trim())) {
                target = p;
                break;
            }
        }
        if (target == null) {
            throw new ValidationException("Problem ID '" + problemId + "' was not found. "
                    + "It may have been removed by the Instructor. Refresh and try again.");
        }
        target.setStatus(status);
        target.setRemarks(cleanRemarks);
        target.setDateUpdated(LocalDate.now().toString());
        repository.saveAll(all);
        return target;
    }
}
