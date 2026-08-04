package com.project.BloodBank.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * One page size for every listing, and a guard on the page number.
 *
 * PageRequest.of throws IllegalArgumentException for a negative page, so ?page=-1 would otherwise
 * be a 500 rather than simply the first page.
 *
 * The size is a property so it can be tuned per deployment without a rebuild; a bean rather than a
 * constant because that is what lets it be injected. It is clamped to at least 1, since a size of
 * zero is also an IllegalArgumentException.
 */
@Component
class PageSupport {

    private final int pageSize;

    PageSupport(@Value("${bloodbank.page-size:10}") int pageSize) {
        this.pageSize = Math.max(pageSize, 1);
    }

    Pageable of(int page, Sort sort) {
        return PageRequest.of(Math.max(page, 0), pageSize, sort);
    }
}
