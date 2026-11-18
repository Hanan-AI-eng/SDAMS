package sdams.facilities.service;

import java.util.ArrayList;
import java.util.List;
import sdams.facilities.model.Equipment;
import sdams.facilities.storage.EquipmentRepository;
import sdams.facilities.storage.StorageException;
import sdams.facilities.util.ValidationException;
import sdams.facilities.util.Validator;

/**
 * The rules for Equipment Inventory Management (add, view, search, update, delete).
 * The screens call this class; this class validates and uses the repository.
 * Every operation reloads the file first, so it always works on the latest saved data.
 */
public class EquipmentService {

    private static final int MAX_TEXT = 60;
    private static final int MAX_NUMBER = 100000;

    private final EquipmentRepository repository;

    public EquipmentService(EquipmentRepository repository) {
        this.repository = repository;
    }

    /** VIEW: all equipment. */
    public List<Equipment> getAll() throws StorageException {
        return repository.loadAll();
    }

    /** Number of damaged lines skipped during the most recent load (0 when the file is healthy). */
    public int getSkippedLineCount() {
        return repository.getSkippedLineCount();
    }

    /** SEARCH: case-insensitive match on ID, name or category. An empty keyword returns everything. */
    public List<Equipment> search(String keyword) throws StorageException {
        String k = (keyword == null) ? "" : keyword.trim().toLowerCase();
        List<Equipment> matches = new ArrayList<>();
        for (Equipment e : repository.loadAll()) {
            if (k.isEmpty()
                    || e.getId().toLowerCase().contains(k)
                    || e.getName().toLowerCase().contains(k)
                    || e.getCategory().toLowerCase().contains(k)) {
                matches.add(e);
            }
        }
        return matches;
    }

    /** ADD: validates, generates the next ID, saves, and returns the new record. */
    public Equipment add(String name, String category, String quantityText,
                         String minimumText, String condition)
            throws ValidationException, StorageException {
        String cleanName = Validator.requireText(name, "Name", MAX_TEXT);
        String cleanCategory = Validator.requireText(category, "Category", MAX_TEXT);
        int quantity = Validator.requireNonNegativeInt(quantityText, "Quantity", MAX_NUMBER);
        int minimum = Validator.requireNonNegativeInt(minimumText, "Minimum stock level", MAX_NUMBER);
        String cleanCondition = Validator.requireChoice(condition, "Condition", Equipment.CONDITIONS);

        List<Equipment> all = repository.loadAll();
        rejectDuplicate(all, cleanName, cleanCategory, null);

        Equipment created = new Equipment(nextId(all), cleanName, cleanCategory,
                quantity, minimum, cleanCondition);
        all.add(created);
        repository.saveAll(all);
        return created;
    }

    /** UPDATE: changes every editable field of the record with the given ID. */
    public Equipment update(String id, String name, String category, String quantityText,
                            String minimumText, String condition)
            throws ValidationException, StorageException {
        String cleanName = Validator.requireText(name, "Name", MAX_TEXT);
        String cleanCategory = Validator.requireText(category, "Category", MAX_TEXT);
        int quantity = Validator.requireNonNegativeInt(quantityText, "Quantity", MAX_NUMBER);
        int minimum = Validator.requireNonNegativeInt(minimumText, "Minimum stock level", MAX_NUMBER);
        String cleanCondition = Validator.requireChoice(condition, "Condition", Equipment.CONDITIONS);

        List<Equipment> all = repository.loadAll();
        Equipment target = findById(all, id);
        rejectDuplicate(all, cleanName, cleanCategory, target.getId());

        target.setName(cleanName);
        target.setCategory(cleanCategory);
        target.setQuantity(quantity);
        target.setMinimumStockLevel(minimum);
        target.setCondition(cleanCondition);
        repository.saveAll(all);
        return target;
    }

    /** DELETE: removes the record with the given ID. */
    public void delete(String id) throws ValidationException, StorageException {
        List<Equipment> all = repository.loadAll();
        Equipment target = findById(all, id);
        all.remove(target);
        repository.saveAll(all);
    }

    // ---------- private helpers ----------

    private Equipment findById(List<Equipment> all, String id) throws ValidationException {
        if (id != null) {
            for (Equipment e : all) {
                if (e.getId().equalsIgnoreCase(id.trim())) {
                    return e;
                }
            }
        }
        throw new ValidationException("Equipment ID '" + id + "' was not found. "
                + "It may have been deleted. Refresh the table and try again.");
    }

    /** Two records may not share the same name AND category. ignoreId lets a record keep its own name. */
    private void rejectDuplicate(List<Equipment> all, String name, String category, String ignoreId)
            throws ValidationException {
        for (Equipment e : all) {
            boolean sameRecord = ignoreId != null && e.getId().equalsIgnoreCase(ignoreId);
            if (!sameRecord && e.getName().equalsIgnoreCase(name)
                    && e.getCategory().equalsIgnoreCase(category)) {
                throw new ValidationException("'" + name + "' already exists in category '" + category
                        + "' (ID " + e.getId() + "). Use Update to change its quantity instead.");
            }
        }
    }

    /** Next ID = highest existing EQ number + 1, written as EQ001, EQ002, ... */
    private String nextId(List<Equipment> all) {
        int highest = 0;
        for (Equipment e : all) {
            String id = e.getId().toUpperCase();
            if (id.startsWith("EQ")) {
                try {
                    highest = Math.max(highest, Integer.parseInt(id.substring(2)));
                } catch (NumberFormatException ignored) {
                    // an ID in some other style: ignore it when counting
                }
            }
        }
        return String.format("EQ%03d", highest + 1);
    }
}
