package com.project.BloodBank.controller;

import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.User;
import com.project.BloodBank.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Puts the signed-in user in the model for every page.
//
// @ControllerAdvice applies to all controllers at once, and a @ModelAttribute method inside one
// runs before every handler. Without it, the header's header(user=${user}) fragment only showed
// its signed-in branch on the few pages whose controller happened to add the attribute, so the
// header flipped between signed-in and guest as you navigated.
//
// The row is reloaded rather than taken from the security context, because the principal there is
// a snapshot from sign-in and never changes. A donor who edited their profile would keep seeing
// their old name in the header until they signed out and back in.
//
// One thing it does not cover: @ExceptionHandler methods, which Spring runs without the
// @ModelAttribute pass. GlobalExceptionHandler calls this class directly for that reason.
@ControllerAdvice
public class CurrentUserAdvice {

    private final UserService userService;

    public CurrentUserAdvice(UserService userService) {
        this.userService = userService;
    }

    // @AuthenticationPrincipal hands over the signed-in User, or null for a visitor who is not
    // signed in - which is exactly what the header's guest branch checks for.
    @ModelAttribute("user")
    public User currentUser(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return null;
        }

        try {
            return userService.getUserById(principal.getId());
        } catch (ResourceNotFoundException e) {
            // Deactivated part-way through a session, since getUserById ignores inactive rows.
            // Swallowed on purpose: this method runs before every page, so letting it escape would
            // turn the entire application into the 404 view.
            return null;
        }
    }
}
