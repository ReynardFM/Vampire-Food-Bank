package com.project.BloodBank.model.enums;

// What an account may do. A USER manages their own profile and requests; an ADMIN also reviews the
// queue, manages accounts, and records donations.
//
// USER rather than DONOR, which is what this was called. The name was wrong: an account holder is
// somebody who may donate, may raise a request, and quite often does both - and a request cannot be
// fulfilled by the person who raised it, so "donor" was actively misleading about half the time.
// Donating is something an account does, not what it is.
//
// Spring Security expects authority names to start with "ROLE_", which User.getAuthorities() adds -
// hence hasAuthority("ROLE_ADMIN") in SecurityConfig.
//
// Registration only creates USER accounts and nothing promotes anyone, so the first admin must come
// from DatabaseSeeder. That is also why UserService refuses to deactivate an ADMIN: it would be a
// one-way door out of the admin area.
public enum Role {
    USER, ADMIN
}
