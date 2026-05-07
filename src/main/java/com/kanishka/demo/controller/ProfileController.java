package com.kanishka.demo.controller;

import com.kanishka.demo.feedback.Feedback;
import com.kanishka.demo.feedback.FeedbackRepository;
import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final FeedbackRepository feedbackRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    // ─────────────────────────────────────────────────────────────────────────
    // GET /profile
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal User currentUser, Model model) {
        User user = userRepository.findById(currentUser.getId()).orElse(currentUser);
        model.addAttribute("user", user);
        model.addAttribute("memberSince",
                user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FMT) : "—");
        model.addAttribute("roleLabel",
                user.getRole() != null ? user.getRole().name().replace("ROLE_", "") : "USER");
        String initials = (user.getFullName() != null && !user.getFullName().isBlank())
                ? String.valueOf(user.getFullName().trim().charAt(0)).toUpperCase() : "U";
        model.addAttribute("initials", initials);
        model.addAttribute("hasPhoto",
                user.getProfilePhoto() != null && user.getProfilePhoto().length > 0);
        List<Feedback> feedbackList = feedbackRepository.findByUserOrderByCreatedAtDesc(user);
        model.addAttribute("feedbackList", feedbackList);
        return "profile";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /profile
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal User currentUser,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                RedirectAttributes ra) {
        if (fullName == null || fullName.trim().length() < 3) {
            ra.addFlashAttribute("error", "Full name must be at least 3 characters.");
            return "redirect:/profile";
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFullName(fullName.trim());
        user.setPhone((phone != null && !phone.isBlank()) ? phone.trim() : null);
        user.setAddress((address != null && !address.isBlank()) ? address.trim() : null);
        userRepository.save(user);
        ra.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/profile";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /profile/change-password
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal User currentUser,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            ra.addFlashAttribute("error", "Current password is incorrect.");
            ra.addFlashAttribute("pwdError", true);
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            ra.addFlashAttribute("pwdError", true);
            return "redirect:/profile";
        }
        if (!isStrongPassword(newPassword)) {
            ra.addFlashAttribute("error",
                    "Password must be 8+ characters with uppercase, lowercase, digit and symbol.");
            ra.addFlashAttribute("pwdError", true);
            return "redirect:/profile";
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("success", "Password changed. Please sign in again.");
        return "redirect:/auth/login";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /profile/photo
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/profile/photo")
    public String uploadPhoto(@AuthenticationPrincipal User currentUser,
                              @RequestParam("photo") MultipartFile file,
                              RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Please select a photo to upload.");
            return "redirect:/profile";
        }
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("image/jpeg") && !ct.equals("image/png") && !ct.equals("image/webp"))) {
            ra.addFlashAttribute("error", "Only JPEG, PNG or WebP images are allowed.");
            return "redirect:/profile";
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            ra.addFlashAttribute("error", "Photo must be smaller than 5 MB.");
            return "redirect:/profile";
        }
        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setProfilePhoto(file.getBytes());
            userRepository.save(user);
            ra.addFlashAttribute("success", "Profile photo updated.");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Failed to save photo. Please try again.");
        }
        return "redirect:/profile";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /profile/photo/remove
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/profile/photo/remove")
    public String removePhoto(@AuthenticationPrincipal User currentUser, RedirectAttributes ra) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setProfilePhoto(null);
        userRepository.save(user);
        ra.addFlashAttribute("success", "Profile photo removed.");
        return "redirect:/profile";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /profile/picture/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/profile/picture/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> servePicture(@PathVariable Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getProfilePhoto() != null && u.getProfilePhoto().length > 0)
                .map(u -> {
                    HttpHeaders h = new HttpHeaders();
                    h.setContentType(MediaType.IMAGE_JPEG);
                    return new ResponseEntity<>(u.getProfilePhoto(), h, HttpStatus.OK);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    private boolean isStrongPassword(String pw) {
        if (pw == null || pw.length() < 8) return false;
        return pw.chars().anyMatch(Character::isUpperCase)
                && pw.chars().anyMatch(Character::isLowerCase)
                && pw.chars().anyMatch(Character::isDigit)
                && pw.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
    }
}