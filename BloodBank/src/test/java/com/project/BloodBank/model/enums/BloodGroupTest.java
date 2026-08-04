package com.project.BloodBank.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BloodGroupTest {

    @Test
    void oNegativeCanGiveToEveryone() {
        for (BloodGroup recipient : BloodGroup.values()) {
            assertThat(recipient.compatibleDonors())
                    .as("%s should be able to receive O-", recipient)
                    .contains(BloodGroup.O_NEGATIVE);
        }
    }

    @Test
    void abPositiveCanReceiveFromEveryone() {
        assertThat(BloodGroup.AB_POSITIVE.compatibleDonors())
                .containsExactlyInAnyOrder(BloodGroup.values());
    }

    @Test
    void oNegativeCanOnlyReceiveFromItself() {
        assertThat(BloodGroup.O_NEGATIVE.compatibleDonors())
                .containsExactly(BloodGroup.O_NEGATIVE);
    }

    @Test
    void aPositiveAcceptsBothAAndOInEitherRhesus() {
        assertThat(BloodGroup.A_POSITIVE.compatibleDonors())
                .containsExactlyInAnyOrder(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE,
                        BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE);
    }

    /** A rhesus-negative patient must never be offered rhesus-positive blood. */
    @Test
    void negativeRecipientsAreNeverOfferedPositiveBlood() {
        for (BloodGroup recipient : BloodGroup.values()) {
            if (recipient.name().endsWith("_NEGATIVE")) {
                assertThat(recipient.compatibleDonors())
                        .as("donors offered to %s", recipient)
                        .allMatch(donor -> donor.name().endsWith("_NEGATIVE"));
            }
        }
    }

    /** A and B antigens must not cross; only AB recipients may take both. */
    @Test
    void aAndBNeverCrossExceptForAbRecipients() {
        assertThat(BloodGroup.A_POSITIVE.compatibleDonors())
                .doesNotContain(BloodGroup.B_POSITIVE, BloodGroup.B_NEGATIVE);
        assertThat(BloodGroup.B_POSITIVE.compatibleDonors())
                .doesNotContain(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE);
        assertThat(BloodGroup.AB_NEGATIVE.compatibleDonors())
                .contains(BloodGroup.A_NEGATIVE, BloodGroup.B_NEGATIVE);
    }

    @Test
    void everyGroupCanAlwaysReceiveItsOwnBlood() {
        for (BloodGroup group : BloodGroup.values()) {
            assertThat(group.compatibleDonors())
                    .as("%s should accept its own group", group)
                    .contains(group);
        }
    }
}
