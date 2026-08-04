package com.project.BloodBank.exception;

import com.project.BloodBank.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        // @ModelAttribute methods on a @ControllerAdvice do not run for @ExceptionHandler methods,
        // so CurrentUserAdvice never reaches this view and the header would render as a guest.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("user",
                auth != null && auth.getPrincipal() instanceof User user ? user : null);
        return "error/404";
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExists(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/register";
    }

    // IllegalStateException is deliberately not handled here. The approve and reject paths that
    // raise it already catch it in AdminController, so a global handler only caught the
    // unintended ones - and redirected those callers to an admin page they get a 403 on.
}
