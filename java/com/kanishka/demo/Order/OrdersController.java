package com.kanishka.demo.Order;

import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;

    // =========================================================
    // MY ORDERS LIST
    // =========================================================
    @GetMapping
    public String myOrders(
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);

        // ✅ FIX: sum quantities, not just number of order rows
        Map<Long, Integer> itemCounts = orders.stream()
                .collect(Collectors.toMap(Order::getId, Order::getTotalItemCount));

        long totalCount = orders.size();

        long deliveredCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();

        long activeCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.PROCESSING
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .count();

        long pendingCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

        model.addAttribute("orders", orders);
        model.addAttribute("itemCounts", itemCounts);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("pendingCount", pendingCount);

        return "orders/list";
    }

    // =========================================================
    // ORDER DETAIL  GET /orders/{id}
    // =========================================================
    @GetMapping("/{id}")
    public String orderDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow();

        Order order = orderService.getOrderById(id);

        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return "redirect:/orders";
            }
        }

        List<OrderTracking> trackingEvents = orderService.getTrackingForOrder(id);

        model.addAttribute("order", order);
        model.addAttribute("trackingEvents", trackingEvents);

        return "orders/detail";
    }

    // =========================================================
    // TRACK PAGE  GET /orders/{id}/track
    // =========================================================
    @GetMapping("/{id}/track")
    public String trackOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow();

        Order order = orderService.getOrderById(id);

        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return "redirect:/orders";
            }
        }

        List<OrderTracking> trackingEvents = orderService.getTrackingForOrder(id);

        model.addAttribute("order", order);
        model.addAttribute("trackingEvents", trackingEvents);

        return "orders/track";
    }
}