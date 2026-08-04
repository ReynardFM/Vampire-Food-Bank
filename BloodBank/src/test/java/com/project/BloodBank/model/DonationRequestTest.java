package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.UrgencyLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DonationRequestTest {

    @Test
    void settingUrgencyMirrorsItsSeverity() {
        DonationRequest request = new DonationRequest();

        request.setUrgencyLevel(UrgencyLevel.CRITICAL);
        assertThat(request.getUrgencySeverity()).isEqualTo(UrgencyLevel.CRITICAL.getSeverity());

        // The mirror has to follow later changes too, or the sort silently uses a stale rank.
        request.setUrgencyLevel(UrgencyLevel.LOW);
        assertThat(request.getUrgencySeverity()).isEqualTo(UrgencyLevel.LOW.getSeverity());
    }

    @Test
    void lifecycleHookFillsDefaultsWithoutOverwritingWhatWasSet() {
        DonationRequest blank = new DonationRequest();
        blank.setUrgencyLevel(UrgencyLevel.HIGH);
        blank.setDefaultValues();

        assertThat(blank.getStatus()).isNotNull();
        assertThat(blank.getRequestDate()).isNotNull();
        assertThat(blank.getUrgencySeverity()).isEqualTo(UrgencyLevel.HIGH.getSeverity());
    }
}
