package com.kanishka.demo.Review;

import com.kanishka.demo.catalog.Product;
import com.kanishka.demo.catalog.ProductRepository;
import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ReviewController {

        private final ReviewRepository       reviewRepository;
        private final ProductRepository      productRepository;
        private final UserRepository         userRepository;
        private final ReviewReportRepository reviewReportRepository;

        /* ═══════════════════════════════════════
           GET /products/{id}/reviews
        ═══════════════════════════════════════ */
        @GetMapping("/products/{id}/reviews")
        @Transactional
        public String productReviews(
                @PathVariable Long id,
                @RequestParam(defaultValue = "0") int page,
                @AuthenticationPrincipal UserDetails principal,
                Model model) {

                Product product = productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found: " + id));

                Page<Review> reviewPage = reviewRepository
                        .findByProductAndApprovedTrueAndDeletedFalse(
                                product, PageRequest.of(page, 5));

                List<Review> reviews = reviewPage.getContent();

                double avgRating = reviews.stream()
                        .mapToInt(Review::getRating)
                        .average().orElse(0.0);

                long[] breakdown = new long[5];
                for (int i = 1; i <= 5; i++) {
                        breakdown[i - 1] = reviewRepository
                                .countByProductAndRatingAndApprovedTrueAndDeletedFalse(product, i);
                }

                Review  userReview = null;
                boolean canReview  = false;
                boolean hasOrdered = false;

                if (principal != null) {
                        User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
                        if (user != null) {
                                Review found = reviewRepository.findByUserAndProduct(user, product).orElse(null);
                                // Auto-clean orphaned unapproved rows left over from old soft-delete logic
                                if (found != null && !found.isApproved() && !found.isDeleted()) {
                                        reviewRepository.delete(found);
                                        found = null;
                                }
                                userReview = found;
                                hasOrdered = reviewRepository
                                        .hasUserOrderedProductNative(user.getId(), id) > 0;
                                // Allow writing a review only if they have never reviewed this product
                                canReview  = (userReview == null);
                        }
                }

                model.addAttribute("product",     product);
                model.addAttribute("reviews",     reviews);
                model.addAttribute("avgRating",   avgRating);
                model.addAttribute("reviewCount", reviewPage.getTotalElements());
                model.addAttribute("breakdown",   breakdown);
                model.addAttribute("userReview",  userReview);
                model.addAttribute("hasMore",     reviewPage.hasNext());
                model.addAttribute("nextPage",    page + 1);
                model.addAttribute("canReview",   canReview);
                model.addAttribute("hasOrdered",  hasOrdered);

                return "reviews/product-reviews";
        }

        /* ═══════════════════════════════════════
           POST /products/{id}/reviews  (NEW review)
        ═══════════════════════════════════════ */
        @PostMapping("/products/{id}/reviews")
        @Transactional
        public String saveReview(
                @PathVariable Long id,
                @RequestParam int rating,
                @RequestParam String comment,
                @AuthenticationPrincipal UserDetails principal,
                RedirectAttributes ra) {

                if (principal == null) {
                        ra.addFlashAttribute("error", "Please log in to leave a review.");
                        return "redirect:/auth/login";
                }
                String validationError = validateReviewInput(rating, comment);
                if (validationError != null) {
                        ra.addFlashAttribute("error", validationError);
                        return "redirect:/products/" + id + "/reviews";
                }
                String trimmed = comment.trim();

                User user = userRepository.findByEmail(principal.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                Product product = productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                // Block if user already has a review for this product
                // (submit button should be hidden, but guard server-side too)
                Optional<Review> existing = reviewRepository.findByUserAndProduct(user, product);
                if (existing.isPresent()) {
                        Review ex = existing.get();
                        // Auto-clean orphaned unapproved rows from old soft-delete logic
                        if (!ex.isApproved() && !ex.isDeleted()) {
                                reviewRepository.delete(ex);
                        } else {
                                ra.addFlashAttribute("error", "You have already reviewed this product. Use the edit option below.");
                                return "redirect:/products/" + id + "/reviews";
                        }
                }

                boolean hasOrdered = reviewRepository
                        .hasUserOrderedProductNative(user.getId(), id) > 0;

                Review review = Review.builder()
                        .user(user)
                        .product(product)
                        .rating(rating)
                        .comment(trimmed)
                        .approved(true)    // auto-approved — visible immediately
                        .deleted(false)
                        .verifiedPurchase(hasOrdered)
                        .build();
                reviewRepository.save(review);

                ra.addFlashAttribute("success", "Thank you! Your review has been published.");
                return "redirect:/products/" + id + "/reviews";
        }

        /* ═══════════════════════════════════════
           GET /products/{id}/reviews/edit
        ═══════════════════════════════════════ */
        @GetMapping("/products/{id}/reviews/edit")
        @Transactional(readOnly = true)
        public String editReviewForm(
                @PathVariable Long id,
                @AuthenticationPrincipal UserDetails principal,
                RedirectAttributes ra) {

                if (principal == null) return "redirect:/auth/login";

                User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
                Product product = productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                Review review = reviewRepository.findByUserAndProduct(user, product).orElse(null);

                if (review == null) {
                        ra.addFlashAttribute("error", "No review found to edit.");
                        return "redirect:/products/" + id + "/reviews";
                }
                // Redirect back to the reviews page — the edit form is embedded there
                return "redirect:/products/" + id + "/reviews";
        }

        /* ═══════════════════════════════════════
           POST /products/{id}/reviews/edit
        ═══════════════════════════════════════ */
        @PostMapping("/products/{id}/reviews/edit")
        @Transactional
        public String updateReview(
                @PathVariable Long id,
                @RequestParam int rating,
                @RequestParam String comment,
                @AuthenticationPrincipal UserDetails principal,
                RedirectAttributes ra) {

                if (principal == null) {
                        ra.addFlashAttribute("error", "Please log in.");
                        return "redirect:/auth/login";
                }
                String validationError = validateReviewInput(rating, comment);
                if (validationError != null) {
                        ra.addFlashAttribute("error", validationError);
                        return "redirect:/products/" + id + "/reviews";
                }
                String trimmed = comment.trim();

                User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
                Product product = productRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                Review review = reviewRepository.findByUserAndProduct(user, product)
                        .orElseThrow(() -> new RuntimeException("Review not found"));

                boolean changed = review.getRating() != rating || !review.getComment().equals(trimmed);
                if (!changed) {
                        ra.addFlashAttribute("error", "No changes detected.");
                        return "redirect:/products/" + id + "/reviews";
                }

                review.setRating(rating);
                review.setComment(trimmed);
                // Keep approved — edits are live immediately
                review.setApproved(true);
                reviewRepository.save(review);

                ra.addFlashAttribute("success", "Your review has been updated.");
                return "redirect:/products/" + id + "/reviews";
        }

        /* ── Shared validation ── */
        private String validateReviewInput(int rating, String comment) {
                if (rating < 1 || rating > 5) return "Rating must be between 1 and 5 stars.";
                String trimmed = (comment != null) ? comment.trim() : "";
                if (trimmed.isEmpty())          return "Review comment cannot be empty.";
                if (trimmed.length() < 10)      return "Review must be at least 10 characters.";
                if (trimmed.length() > 1000)    return "Review must not exceed 1000 characters.";
                // Basic profanity / all-whitespace guard
                if (trimmed.matches("[\\s\\p{Punct}]+"))
                        return "Review must contain meaningful text.";
                // Block reviews that are only repeated single characters e.g. "aaaaaaa"
                if (trimmed.chars().distinct().count() < 3 && trimmed.length() > 5)
                        return "Please write a more descriptive review.";
                return null;
        }

        /* ═══════════════════════════════════════
           POST /products/{id}/reviews/{reviewId}/delete
        ═══════════════════════════════════════ */
        @PostMapping("/products/{id}/reviews/{reviewId}/delete")
        @Transactional
        public String deleteReview(
                @PathVariable Long id,
                @PathVariable Long reviewId,
                @AuthenticationPrincipal UserDetails principal,
                RedirectAttributes ra) {

                if (principal == null) return "redirect:/auth/login";

                User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
                Review review = reviewRepository.findById(reviewId)
                        .orElseThrow(() -> new RuntimeException("Review not found"));

                boolean isAdmin = user.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                if (!review.getUser().getId().equals(user.getId()) && !isAdmin) {
                        ra.addFlashAttribute("error", "You cannot delete this review.");
                        return "redirect:/products/" + id + "/reviews";
                }

                // Delete associated reports first to avoid FK constraint violation
                reviewReportRepository.findByReviewOrderByCreatedAtDesc(review)
                        .forEach(reviewReportRepository::delete);

                reviewRepository.delete(review);

                ra.addFlashAttribute("success", "Review removed. You can now write a new one.");
                return "redirect:/products/" + id + "/reviews";
        }
}