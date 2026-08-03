package com.project.BloodBank.controller;

import com.project.BloodBank.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Every page renders the shared header fragment as header(user=${user}), but "user" is a plain
 * model attribute, so the header only showed its signed-in branch on the few pages whose
 * controller happened to add it. Supplying it for every controller keeps the header consistent
 * as you navigate.
 *
 * The principal is read straight from the security context rather than reloaded from the
 * database, which is what HomeController already did, and keeps this off the request's data path.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    /** Null for anonymous visitors, which is what the header's guest branch checks for. */
    @ModelAttribute("user")
    public User currentUser(@AuthenticationPrincipal User principal) {
        return principal;
    }
}
