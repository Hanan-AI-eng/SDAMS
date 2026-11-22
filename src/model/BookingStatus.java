package model;

/** Status of a class booking (owner: S3, Student). The enum name is what goes into bookings.txt. */
public enum BookingStatus {
    BOOKED, RESCHEDULED, CANCELLED, ATTENDED;

    /** A booking that still holds a seat in its class. */
    public boolean isActive() {
        return this == BOOKED || this == RESCHEDULED;
    }
}
