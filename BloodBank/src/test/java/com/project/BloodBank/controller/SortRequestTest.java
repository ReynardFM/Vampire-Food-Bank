package com.project.BloodBank.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SortRequest is the one place a query string turns into an ORDER BY, so it is also the one place
 * a hand-edited ?sort= could reach the database. These are plain unit tests - no Spring context,
 * because none of this touches one.
 */
class SortRequestTest {

    private static final Set<String> ALLOWED = Set.of("fullName", "email", "lastDonationDate");

    private static final Map<String, String> MAPPED = Map.of(
            "requestDate", "requestDate",
            "urgencyLevel", "urgencySeverity",
            "status", "status");

    @Test
    void anAllowedFieldIsUsedAsAsked() {
        SortRequest sorting = SortRequest.of(ALLOWED, "email", "desc", "fullName", Sort.Direction.ASC);

        assertThat(sorting.toSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "email"));
    }

    /** Anything off the list would raise PropertyReferenceException and turn a URL into a 500. */
    @Test
    void anUnknownFieldFallsBackToTheDefault() {
        SortRequest sorting = SortRequest.of(ALLOWED, "password", "asc", "fullName", Sort.Direction.ASC);

        assertThat(sorting.toSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "fullName"));
    }

    /** An unrecognised direction falls back rather than meaning the opposite of the default. */
    @Test
    void anUnknownDirectionFallsBackToTheDefault() {
        SortRequest sorting = SortRequest.of(ALLOWED, "email", "sideways", "fullName", Sort.Direction.DESC);

        assertThat(sorting.toSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "email"));
    }

    /** Urgency is asked for by name but ordered on the mirrored numeric column. */
    @Test
    void aMappedFieldOrdersOnItsRealProperty() {
        SortRequest sorting = SortRequest.of(MAPPED, "urgencyLevel", "desc", "requestDate", Sort.Direction.DESC);

        assertThat(sorting.toSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "urgencySeverity"));
    }

    /**
     * The pending queue's real ordering: most urgent first, and among equally urgent requests the
     * one that has waited longest.
     */
    @Test
    void theTieBreakIsAppendedAfterTheChosenColumn() {
        SortRequest sorting = SortRequest.of(MAPPED, "urgencyLevel", "desc", "requestDate", Sort.Direction.DESC);

        assertThat(sorting.toSortThenBy("requestDate", Sort.Direction.ASC))
                .isEqualTo(Sort.by(Sort.Direction.DESC, "urgencySeverity")
                        .and(Sort.by(Sort.Direction.ASC, "requestDate")));
    }

    /** Otherwise this would emit "request_date DESC, request_date ASC" - harmless but nonsense. */
    @Test
    void theTieBreakIsSkippedWhenItRepeatsTheChosenColumn() {
        SortRequest sorting = SortRequest.of(MAPPED, "requestDate", "desc", "requestDate", Sort.Direction.DESC);

        assertThat(sorting.toSortThenBy("requestDate", Sort.Direction.ASC))
                .isEqualTo(Sort.by(Sort.Direction.DESC, "requestDate"));
    }
}
