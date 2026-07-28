package com.project.BloodBank.controller;

import com.project.BloodBank.model.User;
import com.project.BloodBank.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If the user is authenticated and NOT an anonymous guest, pass them to the view
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            User currentUser = userService.getCurrentUser();
            model.addAttribute("user", currentUser);
        } else {
            model.addAttribute("user", null);
        }

        return "home";
    }

    @GetMapping("/about-us")
    public String aboutUs(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            User currentUser = userService.getCurrentUser();
            model.addAttribute("user", currentUser);
        } else {
            model.addAttribute("user", null);
        }

        return "about-us";
    }
}