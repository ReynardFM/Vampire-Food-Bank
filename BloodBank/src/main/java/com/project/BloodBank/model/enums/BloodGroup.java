package com.project.BloodBank.model.enums;

import java.util.EnumSet;
import java.util.Set;

// The eight ABO/Rh blood groups, and who can safely give to whom.
// Constant names are what the database and URLs hold; the display name is the short form shown
// on screen.
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

    // Which groups can safely give red cells to a patient of this group.
    //
    // Two rules produce the table below. A patient's immune system attacks any A or B antigen their
    // own blood lacks, so A cannot take B or vice versa; and an Rh-negative patient must never
    // receive Rh-positive blood. Hence O- gives to everyone and AB+ receives from everyone.
    //
    // Donor search needs this because matching on the exact group throws away most of the people
    // who could help: someone needing A+ can also take A-, O+ and O-.
    //
    // A switch expression rather than a map, so the compiler refuses to build if a group is ever
    // added without a rule.
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

    // The same question from the donor's side: who could this person give to?
    //
    // Both directions are needed - search starts from a patient, recording a donation starts from a
    // donor. This walks compatibleDonors() rather than repeating it as a second table, so the two
    // can never disagree and offer a donor that the next screen refuses.
    public Set<BloodGroup> compatibleRecipients() {
        // EnumSet stores enum members as bits in a long, and iterates in declaration order.
        EnumSet<BloodGroup> recipients = EnumSet.noneOf(BloodGroup.class);

        for (BloodGroup recipient : values()) {
            if (recipient.compatibleDonors().contains(this)) {
                recipients.add(recipient);
            }
        }

        return recipients;
    }
}
