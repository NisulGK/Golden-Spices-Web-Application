package com.kanishka.demo.admin;

import com.kanishka.demo.Order.Order;
import com.kanishka.demo.Order.OrderRepository;
import com.kanishka.demo.Order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final OrderRepository orderRepository;

    @GetMapping
    public String reports(Model model) {

        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();

        // ── Total Revenue ──
        BigDecimal totalRevenue = allOrders.stream()
                .map(o -> Optional.ofNullable(o.getTotalAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Counts by status ──
        long pending   = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long confirmed = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long delivered = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();

        // ── Monthly revenue (current year) ──
        int currentYear = java.time.LocalDate.now().getYear();
        Map<String, BigDecimal> monthlyRevenue = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            monthlyRevenue.put(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), BigDecimal.ZERO);
        }
        allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getYear() == currentYear)
                .forEach(o -> {
                    String month = o.getCreatedAt().getMonth()
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    monthlyRevenue.merge(month,
                            Optional.ofNullable(o.getTotalAmount()).orElse(BigDecimal.ZERO),
                            BigDecimal::add);
                });

        // ── Monthly order count (current year) ──
        Map<String, Long> monthlyOrderCount = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            monthlyOrderCount.put(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0L);
        }
        allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getYear() == currentYear)
                .forEach(o -> {
                    String month = o.getCreatedAt().getMonth()
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    monthlyOrderCount.merge(month, 1L, Long::sum);
                });

        // ── Average order value ──
        BigDecimal avgOrderValue = allOrders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(allOrders.size()), 2, java.math.RoundingMode.HALF_UP);

        // ── Paid vs unpaid ──
        long paidOrders   = allOrders.stream().filter(o -> o.getPaymentStatus() != null &&
                o.getPaymentStatus().name().equals("PAID")).count();
        long unpaidOrders = allOrders.size() - paidOrders;

        model.addAttribute("totalRevenue",      totalRevenue);
        model.addAttribute("totalOrders",       allOrders.size());
        model.addAttribute("pendingCount",      pending);
        model.addAttribute("confirmedCount",    confirmed);
        model.addAttribute("deliveredCount",    delivered);
        model.addAttribute("avgOrderValue",     avgOrderValue);
        model.addAttribute("paidOrders",        paidOrders);
        model.addAttribute("unpaidOrders",      unpaidOrders);
        model.addAttribute("monthlyRevenue",    monthlyRevenue);
        model.addAttribute("monthlyOrderCount", monthlyOrderCount);
        model.addAttribute("recentOrders",      allOrders.stream().limit(10).collect(Collectors.toList()));
        model.addAttribute("currentYear",       currentYear);

        return "admin/reports";
    }
}