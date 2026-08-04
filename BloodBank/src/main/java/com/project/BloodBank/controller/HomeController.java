package com.project.BloodBank.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The "user" model attribute these views need comes from CurrentUserAdvice. This controller used
 * to build it itself from the security context, which overwrote the advice's value with the
 * sign-in snapshot and left the header showing a stale name after a profile edit.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "home";
    }

    @GetMapping("/about-us")
    public String aboutUs() {
        return "about-us";
    }
}
