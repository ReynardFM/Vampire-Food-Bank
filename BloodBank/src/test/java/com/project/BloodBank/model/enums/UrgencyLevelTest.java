package com.project.BloodBank.model.enums;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UrgencyLevelTest {

    @Test
    void severityIncreasesWithUrgency() {
        assertThat(UrgencyLevel.LOW.getSeverity())
                .isLessThan(UrgencyLevel.MEDIUM.getSeverity());
        assertThat(UrgencyLevel.MEDIUM.getSeverity())
                .isLessThan(UrgencyLevel.HIGH.getSeverity());
        assertThat(UrgencyLevel.HIGH.getSeverity())
                .isLessThan(UrgencyLevel.CRITICAL.getSeverity());
    }

    /**
     * The whole reason the severity rank exists: the enum is stored as a string, and sorting those
     * alphabetically puts LOW between HIGH and MEDIUM.
     */
    @Test
    void severityOrderDiffersFromAlphabeticalOrder() {
        List<UrgencyLevel> alphabetical = List.of(UrgencyLevel.values()).stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        List<UrgencyLevel> bySeverity = List.of(UrgencyLevel.values()).stream()
                .sorted(Comparator.comparingInt(UrgencyLevel::getSeverity))
                .toList();

        assertThat(alphabetical).containsExactly(
                UrgencyLevel.CRITICAL, UrgencyLevel.HIGH, UrgencyLevel.LOW, UrgencyLevel.MEDIUM);
        assertThat(bySeverity).containsExactly(
                UrgencyLevel.LOW, UrgencyLevel.MEDIUM, UrgencyLevel.HIGH, UrgencyLevel.CRITICAL);
        assertThat(bySeverity).isNotEqualTo(alphabetical);
    }
}
