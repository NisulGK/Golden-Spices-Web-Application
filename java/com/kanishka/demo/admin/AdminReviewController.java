package com.kanishka.demo.admin;

import com.kanishka.demo.Review.Review;
import com.kanishka.demo.Review.ReviewRepository;
import com.kanishka.demo.Review.ReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewRepository       reviewRepository;
    private final ReviewReportRepository reportRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "pending") String filter, Model model) {

        List<Review> reviews = switch (filter) {
            case "approved" -> reviewRepository.findByApprovedTrueAndDeletedFalseOrderByCreatedAtDesc();
            case "all"      -> reviewRepository.findByDeletedFalseOrderByCreatedAtDesc();
            default         -> reviewRepository.findByApprovedFalseAndDeletedFalseOrderByCreatedAtAsc();
        };

        model.addAttribute("reviews",            reviews);
        model.addAttribute("filter",             filter);
        model.addAttribute("pendingCount",       reviewRepository.countByApprovedFalseAndDeletedFalse());
        model.addAttribute("approvedCount",      reviewRepository.countByApprovedTrueAndDeletedFalse());
        model.addAttribute("pendingReportCount", reportRepository.countByResolvedFalse());

        return "admin/reviews/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(defaultValue = "pending") String filter,
                          RedirectAttributes ra) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setApproved(true);
        review.setDeleted(false);
        reviewRepository.save(review);
        ra.addFlashAttribute("success", "Review approved.");
        return "redirect:/admin/reviews?filter=" + filter;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "pending") String filter,
                         RedirectAttributes ra) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setApproved(false);
        review.setDeleted(true);
        reviewRepository.save(review);
        ra.addFlashAttribute("success", "Review rejected and hidden.");
        return "redirect:/admin/reviews?filter=" + filter;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(defaultValue = "pending") String filter,
                         RedirectAttributes ra) {
        if (!reviewRepository.existsById(id)) {
            ra.addFlashAttribute("error", "Review not found.");
            return "redirect:/admin/reviews?filter=" + filter;
        }
        reviewRepository.deleteById(id);
        ra.addFlashAttribute("success", "Review permanently deleted.");
        return "redirect:/admin/reviews?filter=" + filter;
    }
}