package com.kanishka.demo.feedback;

import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository     userRepository;

    @PostMapping
    public String submit(
            @AuthenticationPrincipal User currentUser,
            @RequestParam int rating,
            @RequestParam String category,
            @RequestParam String message,
            RedirectAttributes ra) {

        if (message == null || message.trim().length() < 5) {
            ra.addFlashAttribute("error", "Please write at least 5 characters in your feedback.");
            return "redirect:/profile";
        }
        if (rating < 1 || rating > 5) {
            ra.addFlashAttribute("error", "Please select a star rating.");
            return "redirect:/profile";
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Feedback fb = Feedback.builder()
                .user(user)
                .rating(rating)
                .category(category.trim())
                .message(message.trim())
                .build();

        feedbackRepository.save(fb);

        ra.addFlashAttribute("success", "Thank you! Your feedback has been submitted.");
        ra.addFlashAttribute("openTab", "feedback");
        return "redirect:/profile";
    }
}