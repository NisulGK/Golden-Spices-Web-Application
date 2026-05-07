package com.kanishka.demo.Review;

import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReviewReportController {

    private final ReviewRepository       reviewRepository;
    private final ReviewReportRepository reportRepository;
    private final UserRepository         userRepository;

    /* ── Customer: report a review ── */
    @PostMapping("/reviews/{reviewId}/report")
    @Transactional
    public String report(
            @PathVariable Long reviewId,
            @RequestParam ReviewReport.ReportReason reason,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {

        if (principal == null) return "redirect:/auth/login";

        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (reportRepository.existsByReviewAndReportedBy(review, user)) {
            ra.addFlashAttribute("error", "You have already reported this review.");
            return "redirect:/products/" + review.getProduct().getId() + "/reviews";
        }

        reportRepository.save(ReviewReport.builder()
                .review(review)
                .reportedBy(user)
                .reason(reason)
                .note(note != null ? note.trim() : null)
                .resolved(false)
                .build());

        ra.addFlashAttribute("success", "Review reported. Our team will review it.");
        return "redirect:/products/" + review.getProduct().getId() + "/reviews";
    }

    /* ── Admin: list unresolved reports ── */
    @GetMapping("/admin/reviews/reports")
    @Transactional(readOnly = true)
    public String listReports(Model model) {
        model.addAttribute("reports",      reportRepository.findByResolvedFalseOrderByCreatedAtDesc());
        model.addAttribute("pendingCount", reportRepository.countByResolvedFalse());
        return "admin/reviews/reports";
    }

    /* ── Admin: resolve report ── */
    @PostMapping("/admin/reviews/reports/{id}/resolve")
    @Transactional
    public String resolve(@PathVariable Long id, RedirectAttributes ra) {
        ReviewReport rpt = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        rpt.setResolved(true);
        reportRepository.save(rpt);
        ra.addFlashAttribute("success", "Report resolved.");
        return "redirect:/admin/reviews/reports";
    }

    /* ── Admin: hide review ── */
    @PostMapping("/admin/reviews/reports/{id}/hide")
    @Transactional
    public String hideReview(@PathVariable Long id, RedirectAttributes ra) {
        ReviewReport rpt = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        rpt.getReview().setApproved(false);  // hide from public
        reviewRepository.save(rpt.getReview());
        rpt.setResolved(true);
        reportRepository.save(rpt);
        ra.addFlashAttribute("success", "Review hidden and report resolved.");
        return "redirect:/admin/reviews/reports";
    }

    /* ── Admin: delete review ── */
    @PostMapping("/admin/reviews/reports/{id}/delete")
    @Transactional
    public String deleteReview(@PathVariable Long id, RedirectAttributes ra) {
        ReviewReport rpt = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        rpt.getReview().setDeleted(true);
        reviewRepository.save(rpt.getReview());
        rpt.setResolved(true);
        reportRepository.save(rpt);
        ra.addFlashAttribute("success", "Review deleted.");
        return "redirect:/admin/reviews/reports";
    }
}