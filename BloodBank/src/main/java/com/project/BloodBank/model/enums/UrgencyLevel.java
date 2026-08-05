package com.project.BloodBank.model.enums;

// How urgently a request needs filling.
// Declared in increasing order of urgency, which is also the enum's natural ordering - reordering
// these constants would silently change comparisons.
public enum UrgencyLevel {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    CRITICAL(3);

    // A number for sorting, because the level itself is stored as text and sorting text gives
    // CRITICAL, HIGH, LOW, MEDIUM - alphabetical, with LOW in the middle. DonationRequest keeps a
    // copy of this in a numeric column and the admin queue orders by that instead.
    private final int severity;

    UrgencyLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}
