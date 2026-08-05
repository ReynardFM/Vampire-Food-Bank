package com.project.BloodBank.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// The public pages. Both are permitted without signing in, per SecurityConfig.
//
// These methods look empty because they are: returning a view name is all a page with no data
// needs. The header's "user" comes from CurrentUserAdvice, which runs for every controller.
//
// This class once built that attribute itself from the security context. Handler methods run after
// @ModelAttribute ones, so it overwrote the advice's freshly loaded value with the sign-in
// snapshot, and the header showed a stale name after a profile edit. Doing nothing is the fix.
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
