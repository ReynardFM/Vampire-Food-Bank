package com.project.BloodBank.model.enums;

public enum UrgencyLevel {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    CRITICAL(3);

    /**
     * Rank used for ordering. The enum is persisted as a string, so sorting the column itself is
     * alphabetical (CRITICAL, HIGH, LOW, MEDIUM), which puts LOW between HIGH and MEDIUM.
     * DonationRequest mirrors this value into a numeric column that sorts correctly.
     */
    private final int severity;

    UrgencyLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}
