package sdams.facilities.ui;

/**
 * An interface is a promise: "any class that implements me has a refresh() method".
 * The tabbed window can then refresh ANY screen without knowing which one it is (polymorphism).
 */
public interface Refreshable {
    void refresh();
}
