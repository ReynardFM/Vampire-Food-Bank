package com.project.BloodBank.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

// Renders error pages so they look like the rest of the site.
//
// Spring Boot ships its own BasicErrorController, but it is not one of this application's
// controllers, so CurrentUserAdvice never gave it the "user" attribute - a signed-in visitor who
// mistyped a URL saw the logged-out header. Being an ordinary @Controller, this one does get it.
//
// Boot switches its own error controller off as soon as any ErrorController bean exists, so simply
// declaring this class replaces it.
@Controller
public class CustomErrorController implements ErrorController {

    // The servlet container forwards failed requests here, carrying the details as request
    // attributes rather than as parameters. @RequestMapping with no method covers every verb, since
    // a POST can fail as easily as a GET.
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        int status = statusOf(request);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        model.addAttribute("status", status);

        // Usually blank, because server.error.include-message defaults to "never" - Boot withholds
        // exception text from users, since it can leak internals. The view has its own wording.
        model.addAttribute("message", message != null ? message.toString() : "");

        // Anything that is not a 404 gets the generic page. Sending everything to error/404 would
        // report genuine server failures as "not found".
        return status == 404 ? "error/404" : "error/error";
    }

    // Defensive on both paths: the attribute can be absent, and it is typed as Object rather than
    // int. A failure while rendering the error page would be an unrecoverable loop, so anything
    // unreadable is treated as a server error.
    private int statusOf(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status == null) {
            return 500;
        }
        try {
            return Integer.parseInt(status.toString());
        } catch (NumberFormatException e) {
            return 500;
        }
    }
}
