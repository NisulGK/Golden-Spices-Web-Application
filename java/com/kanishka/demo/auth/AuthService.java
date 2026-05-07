package com.kanishka.demo.auth;

import com.kanishka.demo.auth.dto.RegisterRequest;
import com.kanishka.demo.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender  mailSender;

    @Value("${app.baseUrl}")
    private String baseUrl;

    @Value("${app.mail.from}")
    private String fromEmail;

    // =========================================================
    // REGISTER — saves user then sends email
    // NOTE: @Transactional only wraps DB save.
    //       Mail is sent AFTER commit so a mail failure
    //       does NOT roll back the saved user.
    // =========================================================
    public void register(RegisterRequest req) {

        // ── Validation ──
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (!isStrongPassword(req.getPassword())) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain " +
                            "an uppercase letter, a lowercase letter, a number, " +
                            "and a special character.");
        }
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email address is already registered.");
        }
        if (req.getFullName() == null || req.getFullName().trim().length() < 3) {
            throw new IllegalArgumentException("Full name must be at least 3 characters.");
        }
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            String phone = req.getPhone().replaceAll("[\\s\\-()]", "");
            if (!phone.matches("^[+]?[0-9]{9,15}$")) {
                throw new IllegalArgumentException("Please enter a valid phone number.");
            }
        }

        // ── Generate token ──
        String token = UUID.randomUUID().toString();

        // ── Save user (DB commit happens here) ──
        saveUser(req, email, token);

        // ── Send email AFTER DB commit ──
        // If this throws, user is already saved — they can use Resend link
        sendVerificationEmail(email, req.getFullName().trim(), token);
    }

    @Transactional
    public void saveUser(RegisterRequest req, String email, String token) {
        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone() != null && !req.getPhone().isBlank()
                        ? req.getPhone().trim() : null)
                .address(req.getAddress() != null && !req.getAddress().isBlank()
                        ? req.getAddress().trim() : null)
                .role(Role.ROLE_USER)
                .enabled(false)
                .emailVerified(false)
                .verificationToken(token)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .build();
        userRepository.save(user);
    }

    // =========================================================
    // VERIFY EMAIL
    // =========================================================
    @Transactional
    public String verifyEmail(String token) {
        if (token == null || token.isBlank()) return "invalid";

        User user = userRepository.findByVerificationToken(token).orElse(null);
        if (user == null)            return "invalid";
        if (user.getEmailVerified()) return "already";
        if (user.getVerificationTokenExpiry() != null &&
                LocalDateTime.now().isAfter(user.getVerificationTokenExpiry())) {
            return "expired";
        }

        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        return "success";
    }

    // =========================================================
    // RESEND VERIFICATION
    // =========================================================
    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found with that email."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException(
                    "This account is already verified. Please log in.");
        }

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        sendVerificationEmail(user.getEmail(), user.getFullName(), token);
    }

    // =========================================================
    // SEND VERIFICATION EMAIL
    // =========================================================
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        String verifyUrl = baseUrl + "/auth/verify?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(message, true, "UTF-8");

            h.setFrom(fromEmail);
            h.setTo(toEmail);
            h.setSubject("Verify your GOLDEN account email");

            String html =
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                            "</head><body style='margin:0;padding:0;background:#faf7f2;" +
                            "font-family:Arial,Helvetica,sans-serif;'>" +

                            "<div style='max-width:580px;margin:30px auto;background:#ffffff;" +
                            "border-radius:16px;overflow:hidden;" +
                            "box-shadow:0 8px 32px rgba(0,0,0,.10);'>" +

                            // Header
                            "<div style='background:linear-gradient(135deg,#8a5c0c,#c8841f,#e8a94a);" +
                            "padding:40px 40px 36px;text-align:center;'>" +
                            "<div style='font-size:2.8rem;margin-bottom:10px;'>&#128142;</div>" +
                            "<h1 style='margin:0 0 6px;font-size:1.7rem;color:#fff;" +
                            "font-family:Georgia,serif;letter-spacing:4px;'>GOLDEN</h1>" +
                            "<p style='margin:0;color:rgba(255,255,255,.80);font-size:.78rem;" +
                            "letter-spacing:2px;text-transform:uppercase;'>Dissanayaka Distributors</p>" +
                            "</div>" +

                            // Body
                            "<div style='padding:40px;'>" +
                            "<h2 style='margin:0 0 18px;color:#1a1208;font-size:1.25rem;" +
                            "font-family:Georgia,serif;'>Verify your email address</h2>" +

                            "<p style='margin:0 0 10px;color:#3d2e14;line-height:1.75;font-size:.93rem;'>" +
                            "Hi <strong style='color:#1a1208;'>" + escapeHtml(fullName) + "</strong>,</p>" +

                            "<p style='margin:0 0 30px;color:#7a6444;line-height:1.75;font-size:.91rem;'>" +
                            "Thank you for registering with GOLDEN! " +
                            "Click the button below to verify your email and activate your account." +
                            "</p>" +

                            // Big button
                            "<div style='text-align:center;margin:0 0 32px;'>" +
                            "<a href='" + verifyUrl + "' " +
                            "style='display:inline-block;padding:16px 44px;" +
                            "background:linear-gradient(135deg,#8a5c0c,#c8841f);" +
                            "color:#ffffff;text-decoration:none;border-radius:8px;" +
                            "font-weight:700;font-size:1rem;letter-spacing:.5px;" +
                            "box-shadow:0 4px 18px rgba(200,132,31,.40);'>" +
                            "&#10003;&nbsp;&nbsp;Verify My Email Address</a>" +
                            "</div>" +

                            // Expiry note
                            "<div style='background:#fdf4e3;border:1px solid #e8dfc9;" +
                            "border-radius:10px;padding:14px 18px;margin-bottom:26px;'>" +
                            "<p style='margin:0;font-size:.82rem;color:#7a6444;line-height:1.6;'>" +
                            "&#9203; This link expires in <strong>24 hours</strong>. " +
                            "If you did not create this account, you can safely ignore this email." +
                            "</p></div>" +

                            // Fallback URL
                            "<p style='margin:0 0 4px;color:#aaa;font-size:.76rem;'>" +
                            "Or copy this link into your browser:</p>" +
                            "<p style='margin:0 0 24px;word-break:break-all;" +
                            "font-size:.75rem;color:#c8841f;'>" + verifyUrl + "</p>" +

                            "<p style='margin:0;font-size:.82rem;color:#ccc;'>" +
                            "— The GOLDEN Team</p>" +
                            "</div>" +

                            // Footer
                            "<div style='background:#f5f0e8;border-top:1px solid #e8dfc9;" +
                            "padding:18px 40px;text-align:center;'>" +
                            "<p style='margin:0;font-size:.71rem;color:#bbb;line-height:1.7;'>" +
                            "&copy; 2026 GOLDEN &middot; Dissanayaka Distributors<br>" +
                            "96B/2, Old Kesbewa Road, Nugegoda, Sri Lanka</p>" +
                            "</div></div></body></html>";

            h.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            // Wrap and rethrow so AuthController can show a friendly message
            throw new RuntimeException(
                    "Failed to send email to " + toEmail + ": " + e.getMessage(), e);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;");
    }

    private boolean isStrongPassword(String pw) {
        if (pw == null || pw.length() < 8) return false;
        return pw.chars().anyMatch(Character::isUpperCase)
                && pw.chars().anyMatch(Character::isLowerCase)
                && pw.chars().anyMatch(Character::isDigit)
                && pw.chars().anyMatch(c ->
                "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
    }
}