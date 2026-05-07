package com.kanishka.demo.Order;

import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Delivery Staff dashboard — accessible to ROLE_ADMIN and ROLE_STAFF.
 * URL prefix: /staff
 */
@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class DeliveryStaffController {

    private final OrderRepository  orderRepository;
    private final OrderService     orderService;
    private final UserRepository   userRepository;

    private User getUser(UserDetails p) {
        return userRepository.findByEmail(p.getUsername()).orElseThrow();
    }

    /* ── Dashboard: all active orders ── */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        List<Order> active = orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED
                        && o.getStatus() != OrderStatus.CANCELLED)
                .toList();
        List<Order> all = orderRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("activeOrders", active);
        model.addAttribute("allOrders",    all);
        model.addAttribute("statuses",     OrderStatus.values());
        return "staff/dashboard";
    }

    /* ── Update order status ── */
    @PostMapping("/orders/{id}/status")
    @Transactional
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               @RequestParam(required = false) String note,
                               @RequestParam(required = false) String location,
                               @AuthenticationPrincipal UserDetails principal,
                               RedirectAttributes ra) {
        User user = getUser(principal);
        orderService.updateOrderStatus(id, status, note, user.getFullName());
        if (location != null && !location.isBlank())
            orderService.updateLocation(id, location);
        ra.addFlashAttribute("success", "Order status updated to " + status + ".");
        return "redirect:/staff/dashboard";
    }

    /* ── Update location only ── */
    @PostMapping("/orders/{id}/location")
    @Transactional
    public String updateLocation(@PathVariable Long id,
                                 @RequestParam @NotBlank String location,
                                 RedirectAttributes ra) {
        orderService.updateLocation(id, location);
        ra.addFlashAttribute("success", "Location updated.");
        return "redirect:/staff/dashboard";
    }
}