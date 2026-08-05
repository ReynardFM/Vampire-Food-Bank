package com.project.BloodBank.model.enums;

// What an account may do. A DONOR manages their own profile and requests; an ADMIN also reviews
// the queue, manages accounts, and records donations.
//
// Spring Security expects authority names to start with "ROLE_", which User.getAuthorities() adds -
// hence hasAuthority("ROLE_ADMIN") in SecurityConfig.
//
// Registration only creates DONOR accounts and nothing promotes anyone, so the first admin must
// come from DatabaseSeeder. That is also why UserService refuses to deactivate an ADMIN: it would
// be a one-way door out of the admin area.
public enum Role {
    DONOR, ADMIN
}
