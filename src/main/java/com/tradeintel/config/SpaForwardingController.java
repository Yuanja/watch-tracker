package com.tradeintel.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SPA support: serves index.html for client-side routes that don't match
 * a backend endpoint. Implements ErrorController to catch 404s and forward
 * them to the SPA instead of returning a whitelabel error page.
 */
@Controller
public class SpaForwardingController implements ErrorController {

    /** SPA login route — serves index.html so React Router handles /login */
    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String login() {
        return "forward:/index.html";
    }

    /** Catch-all for unknown routes — forward to SPA */
    @GetMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public String handleError(HttpServletRequest request) {
        return "forward:/index.html";
    }
}
