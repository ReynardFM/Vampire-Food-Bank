package com.project.BloodBank.exception;

import com.project.BloodBank.controller.CurrentUserAdvice;
import com.project.BloodBank.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Catches exceptions that escape any controller and turns them into something a user can read.
//
// @ControllerAdvice applies across the whole application, so an exception only has to be handled
// here once rather than in every method that might raise it.
@ControllerAdvice
public class GlobalExceptionHandler {

    private final CurrentUserAdvice currentUserAdvice;

    public GlobalExceptionHandler(CurrentUserAdvice currentUserAdvice) {
        this.currentUserAdvice = currentUserAdvice;
    }

    // @ResponseStatus sets the real HTTP status. Without it this would render the 404 page while
    // telling the browser 200, which misleads search engines and monitoring alike.
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());

        // The header's "user" has to be supplied by hand here. Spring does not run @ModelAttribute
        // methods for @ExceptionHandler ones, so CurrentUserAdvice never reaches this view and the
        // page would render as though nobody were signed in.
        //
        // CurrentUserAdvice is called rather than the principal being used directly, because the
        // principal is a snapshot from sign-in: after a profile edit, the 404 page showed the old
        // name while every other page showed the new one.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User principal = auth != null && auth.getPrincipal() instanceof User user ? user : null;
        model.addAttribute("user", currentUserAdvice.currentUser(principal));

        return "error/404";
    }

    // A safety net rather than the usual path. AuthController normally catches this itself so it
    // can attach the message to the email field; this only fires if some other route raises it.
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExists(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/register";
    }

    // IllegalStateException is deliberately not handled here, even though several services raise
    // it. Approve and reject already catch their own in AdminController, so a global handler would
    // only pick up the unintended ones - and send those callers to an admin page they get a 403 on,
    // turning a clear failure into a confusing one.
}
