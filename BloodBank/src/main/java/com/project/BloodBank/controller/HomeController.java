package com.project.BloodBank.controller;

import com.project.BloodBank.model.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If the user is authenticated and NOT an anonymous guest, pass them to the view
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            model.addAttribute("user", authenticatedUser(auth));
        } else {
            model.addAttribute("user", null);
        }

        return "home";
    }

    @GetMapping("/about-us")
    public String aboutUs(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            model.addAttribute("user", authenticatedUser(auth));
        } else {
            model.addAttribute("user", null);
        }

        return "about-us";
    }

    private User authenticatedUser(Authentication authentication) {
        return authentication.getPrincipal() instanceof User user ? user : null;
    }
}
