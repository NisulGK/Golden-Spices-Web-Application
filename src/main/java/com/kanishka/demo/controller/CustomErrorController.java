package com.kanishka.demo.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request,
                              HttpServletResponse response,
                              Model model) {

        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorAttr = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object errorMsg = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object uriAttr = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        String requestUri = uriAttr != null ? uriAttr.toString() : "Unknown";
        int statusCode = 500;

        if (statusAttr != null) {
            try {
                statusCode = Integer.parseInt(statusAttr.toString());
            } catch (Exception ignored) {
                statusCode = 500;
            }
        }

        if (errorAttr instanceof Throwable t) {
            String msg = t.getMessage() != null ? t.getMessage() : "";
            if (isAuthRelated(t, msg)) {
                clearRememberMeCookie(response);
                return "redirect:/auth/login?expired=true";
            }
        }

        if (errorMsg instanceof String s && isUserNotFound(s)) {
            clearRememberMeCookie(response);
            return "redirect:/auth/login?expired=true";
        }

        if (statusCode == HttpStatus.UNAUTHORIZED.value()
                || statusCode == HttpStatus.FORBIDDEN.value()) {
            return "redirect:/auth/login";
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("requestUri", requestUri);
        model.addAttribute("errorMessage",
                errorMsg != null ? errorMsg.toString() : "Something went wrong.");

        return "error";
    }

    private boolean isAuthRelated(Throwable t, String msg) {
        return t instanceof org.springframework.security.core.userdetails.UsernameNotFoundException
                || t instanceof org.springframework.security.core.AuthenticationException
                || isUserNotFound(msg);
    }

    private boolean isUserNotFound(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("user not found")
                || lower.contains("no account found")
                || lower.contains("usernamenotfoundexception");
    }

    private void clearRememberMeCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("remember-me", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        Cookie sessionCookie = new Cookie("JSESSIONID", null);
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
    }
}