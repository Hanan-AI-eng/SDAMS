package sdams.facilities.model;

/** Totals shown at the top of the Stock Reports screen. Calculated, never stored. */
public class StockSummary {
    private final int itemTypes;
    private final int totalUnits;
    private final int lowStockItems;
    private final int outOfStockItems;

    public StockSummary(int itemTypes, int totalUnits, int lowStockItems, int outOfStockItems) {
        this.itemTypes = itemTypes;
        this.totalUnits = totalUnits;
        this.lowStockItems = lowStockItems;
        this.outOfStockItems = outOfStockItems;
    }

    public int getItemTypes() { return itemTypes; }
    public int getTotalUnits() { return totalUnits; }
    public int getLowStockItems() { return lowStockItems; }
    public int getOutOfStockItems() { return outOfStockItems; }
}
