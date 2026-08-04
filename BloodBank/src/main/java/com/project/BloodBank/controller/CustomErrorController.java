package com.project.BloodBank.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Replaces Spring's BasicErrorController so error pages render like the rest of the site.
 *
 * The reason it exists is the header: BasicErrorController is not one of this application's
 * controllers, so CurrentUserAdvice never contributed the "user" attribute to it and a signed-in
 * visitor who mistyped a URL was shown the logged-out header. Being an ordinary @Controller, this
 * one does receive the advice.
 *
 * Spring Boot backs its own error controller off as soon as an ErrorController bean is present.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        int status = statusOf(request);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        model.addAttribute("status", status);
        // error/404.html falls back to its own wording when this is blank, which it usually is:
        // server.error.include-message defaults to "never".
        model.addAttribute("message", message != null ? message.toString() : "");

        // Anything that is not a 404 gets the generic page. Routing everything to error/404 would
        // report genuine server failures as "not found".
        return status == 404 ? "error/404" : "error/error";
    }

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
