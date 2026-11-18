package sdams.facilities.service;

import java.util.List;
import sdams.facilities.model.Equipment;
import sdams.facilities.model.StockSummary;
import sdams.facilities.storage.StorageException;

/**
 * Stock reports (view and search). This class has NO add/update/delete methods and no
 * file of its own: it asks EquipmentService, which reads equipment.txt. So the report can
 * never disagree with the inventory, and inventory changes appear the next time it loads.
 */
public class StockReportService {

    private final EquipmentService equipmentService;

    public StockReportService(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    /** VIEW + SEARCH: report rows filtered by equipment ID, name or category (empty = all). */
    public List<Equipment> getReport(String keyword) throws StorageException {
        return equipmentService.search(keyword);
    }

    /** Totals across ALL equipment (OPTIONAL enhancement, not required by the brief). */
    public StockSummary getSummary() throws StorageException {
        List<Equipment> all = equipmentService.getAll();
        int units = 0;
        int low = 0;
        int out = 0;
        for (Equipment e : all) {
            units += e.getQuantity();
            if (Equipment.LOW_STOCK.equals(e.getStockStatus())) {
                low++;
            } else if (Equipment.OUT_OF_STOCK.equals(e.getStockStatus())) {
                out++;
            }
        }
        return new StockSummary(all.size(), units, low, out);
    }
}
