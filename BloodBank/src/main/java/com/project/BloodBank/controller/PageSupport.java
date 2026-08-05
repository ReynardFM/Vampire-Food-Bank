package com.project.BloodBank.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

// One page size for every listing, and a guard on the page number.
//
// A bean rather than a constant so the size can come from a property and be injected. @Value reads
// bloodbank.page-size, and the ":10" after the colon is the default when it is not set.
@Component
class PageSupport {

    private final int pageSize;

    // Clamped because PageRequest.of rejects a size of zero or less, and a typo in the property
    // would otherwise break every listing at once.
    PageSupport(@Value("${bloodbank.page-size:10}") int pageSize) {
        this.pageSize = Math.max(pageSize, 1);
    }

    // Math.max on the page number for the same reason: PageRequest.of throws on a negative page, so
    // ?page=-1 would be a 500 rather than simply the first page.
    Pageable of(int page, Sort sort) {
        return PageRequest.of(Math.max(page, 0), pageSize, sort);
    }
}
