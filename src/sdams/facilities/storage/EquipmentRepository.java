package sdams.facilities.storage;

import sdams.facilities.model.Equipment;

/**
 * Stores Equipment records.
 * File line format:  id|name|category|quantity|minimumStockLevel|condition
 * Example:           EQ001|Training Mat|Mats|20|5|Good
 */
public class EquipmentRepository extends TextFileRepository<Equipment> {

    public EquipmentRepository(String filePath) {
        super(filePath);
    }

    @Override
    protected Equipment parseLine(String line) {
        String[] f = splitFields(line);
        if (f.length != 6) {
            throw new IllegalArgumentException("Equipment line must have 6 fields: " + line);
        }
        // Integer.parseInt throws NumberFormatException (a kind of IllegalArgumentException) if bad.
        return new Equipment(f[0], f[1], f[2],
                Integer.parseInt(f[3].trim()), Integer.parseInt(f[4].trim()), f[5]);
    }

    @Override
    protected String formatRecord(Equipment e) {
        return String.join(SEPARATOR, e.getId(), e.getName(), e.getCategory(),
                String.valueOf(e.getQuantity()), String.valueOf(e.getMinimumStockLevel()),
                e.getCondition());
    }
}
