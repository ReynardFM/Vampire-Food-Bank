package com.project.BloodBank.controller;

import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.User;
import com.project.BloodBank.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Every page renders the shared header fragment as header(user=${user}), but "user" is a plain
 * model attribute, so the header only showed its signed-in branch on the few pages whose
 * controller happened to add it. Supplying it for every controller keeps the header consistent
 * as you navigate.
 *
 * The row is reloaded rather than taken straight from the security context: the principal is a
 * snapshot from sign-in time, so a donor who edits their profile would keep seeing their old name
 * in the header until they logged out and back in.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    private final UserService userService;

    public CurrentUserAdvice(UserService userService) {
        this.userService = userService;
    }

    /** Null for anonymous visitors, which is what the header's guest branch checks for. */
    @ModelAttribute("user")
    public User currentUser(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return null;
        }

        try {
            return userService.getUserById(principal.getId());
        } catch (ResourceNotFoundException e) {
            // Deactivated part-way through a session. Must be swallowed: letting this escape a
            // @ModelAttribute method would turn every page into the 404 view.
            return null;
        }
    }
}
