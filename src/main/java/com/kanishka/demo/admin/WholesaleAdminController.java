package com.kanishka.demo.admin;

import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import com.kanishka.demo.wholesale.WholesaleOrder;
import com.kanishka.demo.wholesale.WholesaleOrderRepository;
import com.kanishka.demo.wholesale.WholesaleProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/wholesale")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // 🔐 ADMIN ONLY — users are blocked at Spring Security level
public class WholesaleAdminController {

    private final UserRepository         userRepository;
    private final WholesaleOrderRepository   orderRepository;
    private final WholesaleProductRepository productRepository;

    // =========================================================
    // ADMIN WHOLESALE DASHBOARD  ← NEW
    // =========================================================
    @GetMapping("/dashboard")
    public String adminWholesaleDashboard(Model model) {

        // All wholesale orders — use JOIN FETCH to eagerly load user,
        // preventing LazyInitializationException when Thymeleaf accesses o.user.fullName
        // JOIN FETCH user AND product — avoids LazyInitializationException in Thymeleaf
        List<WholesaleOrder> allOrders = orderRepository.findAllWithUserAndProduct();

        BigDecimal totalRevenue = allOrders.stream()
                .map(o -> Optional.ofNullable(o.getTotalAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount  = countByStatus(allOrders, "PENDING");
        long approvedCount = countByStatus(allOrders, "APPROVED");
        long shippedCount  = countByStatus(allOrders, "SHIPPED");

        // Pending wholesale account requests
        List<User> pendingRequests =
                userRepository.findByWholesaleRequestedTrueAndWholesaleApprovedFalse();

        // All approved wholesale users
        List<User> approvedUsers =
                userRepository.findByWholesaleApprovedTrue();

        // All wholesale products
        model.addAttribute("allOrders",       allOrders);
        model.addAttribute("totalRevenue",    totalRevenue);
        model.addAttribute("totalOrders",     allOrders.size());
        model.addAttribute("pendingCount",    pendingCount);
        model.addAttribute("approvedCount",   approvedCount);
        model.addAttribute("shippedCount",    shippedCount);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("pendingRequestCount", pendingRequests.size());
        model.addAttribute("approvedUsers",   approvedUsers);
        model.addAttribute("products",        productRepository.findByActiveTrue());

        return "admin/wholesale/dashboard";   // ← admin-only template
    }

    // =========================================================
    // VIEW PENDING WHOLESALE REQUESTS
    // =========================================================
    @GetMapping("/requests")
    public String viewRequests(Model model) {
        List<User> pendingUsers =
                userRepository.findByWholesaleRequestedTrueAndWholesaleApprovedFalse();
        model.addAttribute("requests", pendingUsers);
        return "admin/wholesale/requests";
    }

    // =========================================================
    // APPROVE WHOLESALE ACCESS
    // =========================================================
    @PostMapping("/{id}/approve")
    public String approveUser(@PathVariable Long id, RedirectAttributes ra) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getWholesaleApproved())) {
            ra.addFlashAttribute("error", "User is already approved.");
            return "redirect:/admin/wholesale/requests";
        }

        user.setWholesaleApproved(true);
        user.setWholesaleRequested(false);
        userRepository.save(user);

        ra.addFlashAttribute("success",
                "Wholesale access approved for " + user.getFullName());
        return "redirect:/admin/wholesale/requests";
    }

    // =========================================================
    // REJECT WHOLESALE REQUEST
    // =========================================================
    @PostMapping("/{id}/reject")
    public String rejectUser(@PathVariable Long id, RedirectAttributes ra) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setWholesaleRequested(false);
        user.setWholesaleApproved(false);
        userRepository.save(user);

        ra.addFlashAttribute("error",
                "Wholesale request rejected for " + user.getFullName());
        return "redirect:/admin/wholesale/requests";
    }

    // =========================================================
    // UPDATE ORDER STATUS
    // =========================================================
    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    RedirectAttributes ra) {
        WholesaleOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
        ra.addFlashAttribute("success", "Order status updated to " + status);
        return "redirect:/admin/wholesale/dashboard";
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private long countByStatus(List<WholesaleOrder> orders, String status) {
        return orders.stream()
                .filter(o -> status.equalsIgnoreCase(o.getStatus()))
                .count();
    }
}