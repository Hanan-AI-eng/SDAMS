package model;

/** Ways a student can pay (owner: S3, Student). */
public enum PaymentMethod {
    CARD("Card"), ONLINE_BANKING("Online banking"), E_WALLET("E-wallet"), CASH("Cash");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;   // shown in combo boxes
    }
}
