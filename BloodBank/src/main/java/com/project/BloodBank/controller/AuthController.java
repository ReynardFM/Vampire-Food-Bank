package com.project.BloodBank.controller;

import com.project.BloodBank.dto.UserRegistrationDto;
import com.project.BloodBank.exception.EmailAlreadyExistsException;
import com.project.BloodBank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Signing in and registering.
//
// Note there is no method that handles the login form's POST. Spring Security intercepts /login
// before it ever reaches a controller, checks the password, and redirects. This class only renders
// the page and reports what happened.
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ?error and ?logout are appended by Spring Security itself on a failed sign-in and a
    // successful sign-out, configured in SecurityConfig. required = false because the ordinary
    // case is neither being present.
    //
    // The error message stays vague on purpose. Saying which of the two was wrong would confirm
    // to a stranger that an email address has an account.
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully.");
        }
        return "login";
    }

    // Asks before signing out, rather than doing it the moment the header link is clicked.
    //
    // This works because Spring Security's logout only matches POST /logout while CSRF is enabled,
    // so a GET falls through to here. The page it renders holds the actual POST form, which is the
    // request Spring Security acts on - this method never signs anybody out itself.
    //
    // A page rather than a JavaScript confirm(): nothing else in this application needs JavaScript,
    // and a dialog that silently does nothing when scripting is off is worse than no dialog.
    @GetMapping("/logout")
    public String confirmLogout() {
        return "logout-confirm";
    }

    // The containsAttribute check matters: a rejected submission redirects back here with the
    // filled-in form already in the model, and overwriting it with a blank one would wipe out
    // everything the user typed.
    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new UserRegistrationDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationDto") UserRegistrationDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        // @Valid runs the DTO's constraints and collects failures in BindingResult rather than
        // throwing. BindingResult must be the parameter immediately after the object it belongs to,
        // or Spring will not connect the two.
        //
        // Returning the view rather than redirecting is deliberate: it keeps the submitted values
        // and the error messages, so the form comes back filled in.
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.register(dto);

            // A flash attribute survives exactly one redirect and is then discarded, which is what
            // lets the success message appear on the login page without sticking around on refresh.
            redirectAttributes.addFlashAttribute("success", "Account created! Please log in.");
            return "redirect:/login";
        } catch (EmailAlreadyExistsException e) {

            // Attached to the email field rather than shown as a general error, so it appears
            // beside the input that caused it.
            result.rejectValue("email", "error.registrationDto", e.getMessage());
            return "register";
        }
    }
}
