package sdams.facilities.model;

/**
 * One piece (or one type) of martial arts equipment, for example "Training Mat".
 * This class only HOLDS data. It does not read files or draw screens.
 */
public class Equipment {

    public static final String[] CATEGORIES = {
        "Mats", "Punching Bags", "Gloves", "Uniforms", "Protective Gear", "Training Aids", "Other"
    };
    public static final String[] CONDITIONS = {"New", "Good", "Fair", "Poor", "Damaged"};

    public static final String IN_STOCK = "In Stock";
    public static final String LOW_STOCK = "Low Stock";
    public static final String OUT_OF_STOCK = "Out of Stock";

    // private = only code inside this class can touch the fields (encapsulation).
    private final String id;            // final: an ID never changes after creation
    private String name;
    private String category;
    private int quantity;
    private int minimumStockLevel;
    private String condition;

    public Equipment(String id, String name, String category, int quantity,
                     int minimumStockLevel, String condition) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.minimumStockLevel = minimumStockLevel;
        this.condition = condition;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public int getMinimumStockLevel() { return minimumStockLevel; }
    public String getCondition() { return condition; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setMinimumStockLevel(int minimumStockLevel) { this.minimumStockLevel = minimumStockLevel; }
    public void setCondition(String condition) { this.condition = condition; }

    /**
     * Stock status is CALCULATED from the numbers every time it is asked for.
     * It is never stored in the file, so it can never disagree with the quantity.
     */
    public String getStockStatus() {
        if (quantity == 0) {
            return OUT_OF_STOCK;
        }
        if (quantity <= minimumStockLevel) {
            return LOW_STOCK;
        }
        return IN_STOCK;
    }
}
