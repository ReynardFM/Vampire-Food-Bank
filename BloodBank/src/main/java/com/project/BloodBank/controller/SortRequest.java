package com.project.BloodBank.controller;

import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.util.Map;
import java.util.Set;

/**
 * A ?sort= and ?dir= pair that has been checked against a whitelist.
 *
 * Every sortable listing repeated the same three steps: fall back to a default when the requested
 * field is not on the whitelist, turn "asc"/"desc" into a Sort.Direction, then echo both back to
 * the view so the column headers know which one is active and which way to flip. Six listings
 * spread over three controllers each had their own copy, and they had already started to drift.
 *
 * The whitelist is not decoration: Sort.by() accepts any string, and an unknown property raises
 * PropertyReferenceException, so a hand-edited query string would otherwise be a 500.
 */
final class SortRequest {

    /** The name as it appears in the URL. Kept separate from the property so links stay readable. */
    private final String name;
    private final String property;
    private final Sort.Direction direction;

    private SortRequest(String name, String property, Sort.Direction direction) {
        this.name = name;
        this.property = property;
        this.direction = direction;
    }

    /** For listings where the URL name is the property, which is most of them. */
    static SortRequest of(Set<String> allowed, String sort, String dir,
                          String defaultSort, Sort.Direction defaultDirection) {
        String name = allowed.contains(sort) ? sort : defaultSort;
        return new SortRequest(name, name, resolveDirection(dir, defaultDirection));
    }

    /**
     * For listings that order on a different property than the one named in the URL. Urgency is the
     * reason this overload exists: the enum column sorts alphabetically, so it orders on the
     * mirrored urgencySeverity while the link stays ?sort=urgencyLevel.
     */
    static SortRequest of(Map<String, String> allowed, String sort, String dir,
                          String defaultSort, Sort.Direction defaultDirection) {
        String name = allowed.containsKey(sort) ? sort : defaultSort;
        return new SortRequest(name, allowed.get(name), resolveDirection(dir, defaultDirection));
    }

    /** An unrecognised direction falls back rather than failing, same as an unrecognised field. */
    private static Sort.Direction resolveDirection(String dir, Sort.Direction fallback) {
        if ("asc".equalsIgnoreCase(dir)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(dir)) {
            return Sort.Direction.DESC;
        }
        return fallback;
    }

    Sort toSort() {
        return Sort.by(direction, property);
    }

    /** Puts the URL-facing name and direction in the model for the sortable column headers. */
    void applyTo(Model model) {
        model.addAttribute("sort", name);
        model.addAttribute("dir", direction.isAscending() ? "asc" : "desc");
    }
}
