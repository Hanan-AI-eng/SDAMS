package model;

/**
 * Base class for records stored in the SDAMS system.
 * All records must have a unique ID.
 */
public abstract class Record {
    private String id;

    public Record(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
