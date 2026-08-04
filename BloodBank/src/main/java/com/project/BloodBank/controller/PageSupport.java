package com.project.BloodBank.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * One page size for every listing, and a guard on the page number.
 *
 * PageRequest.of throws IllegalArgumentException for a negative page, so ?page=-1 would otherwise
 * be a 500 rather than simply the first page.
 */
final class PageSupport {

    static final int PAGE_SIZE = 10;

    private PageSupport() {
    }

    static Pageable of(int page, Sort sort) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort);
    }
}
