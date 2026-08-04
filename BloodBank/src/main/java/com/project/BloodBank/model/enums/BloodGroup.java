package com.project.BloodBank.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String displayName;

    BloodGroup(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * The groups whose red cells may be given to a patient of this group.
     *
     * Two rules combined: a patient must not receive an antigen they lack, so A cannot take B and
     * vice versa, and an Rh-negative patient must not receive Rh-positive blood. That is what makes
     * O- the universal donor and AB+ the universal recipient.
     *
     * Matching on an exact group hides most of the people who could actually help - someone needing
     * A+ can also receive A-, O+ and O-.
     */
    public Set<BloodGroup> compatibleDonors() {
        return switch (this) {
            case O_NEGATIVE -> EnumSet.of(O_NEGATIVE);
            case O_POSITIVE -> EnumSet.of(O_NEGATIVE, O_POSITIVE);
            case A_NEGATIVE -> EnumSet.of(O_NEGATIVE, A_NEGATIVE);
            case A_POSITIVE -> EnumSet.of(O_NEGATIVE, O_POSITIVE, A_NEGATIVE, A_POSITIVE);
            case B_NEGATIVE -> EnumSet.of(O_NEGATIVE, B_NEGATIVE);
            case B_POSITIVE -> EnumSet.of(O_NEGATIVE, O_POSITIVE, B_NEGATIVE, B_POSITIVE);
            case AB_NEGATIVE -> EnumSet.of(O_NEGATIVE, A_NEGATIVE, B_NEGATIVE, AB_NEGATIVE);
            case AB_POSITIVE -> EnumSet.allOf(BloodGroup.class);
        };
    }
}
