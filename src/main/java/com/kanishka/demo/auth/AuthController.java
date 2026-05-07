package com.kanishka.demo.auth;

import com.kanishka.demo.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =========================================================
    // LOGIN PAGE
    // =========================================================
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String expired,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String verified,
            Model model) {

        if (error != null) {
            model.addAttribute("loginError",
                    "Invalid email or password. Your account may not be verified yet.");
        }
        if (expired != null) {
            model.addAttribute("loginError",
                    "Your session has expired. Please log in again.");
        }
        if (logout != null) {
            model.addAttribute("loginSuccess",
                    "You have been logged out successfully.");
        }
        if (verified != null) {
            model.addAttribute("loginSuccess",
                    "Email verified! You can now log in.");
        }
        return "auth/login";
    }

    // =========================================================
    // REGISTER PAGE
    // =========================================================
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    // =========================================================
    // REGISTER SUBMIT
    // =========================================================
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("registerRequest", request);
            return "auth/register";
        }

        try {
            authService.register(request);
            redirectAttributes.addFlashAttribute("registeredEmail", request.getEmail());
            return "redirect:/auth/check-email";

        } catch (IllegalArgumentException e) {
            // Business rule errors (duplicate email, weak password, etc.)
            model.addAttribute("registerRequest", request);
            model.addAttribute("error", e.getMessage());
            return "auth/register";

        } catch (RuntimeException e) {
            // Mail sending failed — show friendly message, keep form data
            model.addAttribute("registerRequest", request);

            String msg = e.getMessage() != null ? e.getMessage() : "";

            if (msg.contains("Authentication") || msg.contains("authentication")
                    || msg.contains("Username and Password") || msg.contains("535")) {
                model.addAttribute("error",
                        "Email configuration error: Gmail credentials are wrong. " +
                                "Please set up a Gmail App Password in application.properties.");

            } else if (msg.contains("connect") || msg.contains("timeout")
                    || msg.contains("Connection")) {
                model.addAttribute("error",
                        "Cannot connect to mail server. Check your internet connection " +
                                "and mail settings in application.properties.");

            } else {
                model.addAttribute("error",
                        "Registration saved but verification email failed to send. " +
                                "Technical detail: " + getRootCause(e));
            }
            return "auth/register";
        }
    }

    // =========================================================
    // CHECK EMAIL PAGE
    // =========================================================
    @GetMapping("/check-email")
    public String checkEmailPage() {
        return "auth/check-email";
    }

    // =========================================================
    // VERIFY EMAIL
    // =========================================================
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam(required = false) String token,
                              RedirectAttributes ra) {
        String result = authService.verifyEmail(token);
        switch (result) {
            case "success":
                ra.addFlashAttribute("loginSuccess",
                        "Email verified! Your account is now active. Please log in.");
                return "redirect:/auth/login?verified=true";
            case "already":
                ra.addFlashAttribute("loginSuccess",
                        "Your email is already verified. Please log in.");
                return "redirect:/auth/login";
            case "expired":
                ra.addFlashAttribute("tokenExpired", true);
                return "redirect:/auth/resend";
            default:
                ra.addFlashAttribute("invalidToken", true);
                return "redirect:/auth/resend";
        }
    }

    // =========================================================
    // RESEND PAGE
    // =========================================================
    @GetMapping("/resend")
    public String resendPage() {
        return "auth/resend";
    }

    // =========================================================
    // RESEND SUBMIT
    // =========================================================
    @PostMapping("/resend")
    public String resendVerification(@RequestParam String email,
                                     RedirectAttributes ra) {
        try {
            authService.resendVerification(email);
            ra.addFlashAttribute("success",
                    "Verification email sent! Please check your inbox.");
            return "redirect:/auth/check-email";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/resend";
        }
    }

    // =========================================================
    // HELPER
    // =========================================================
    private String getRootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}