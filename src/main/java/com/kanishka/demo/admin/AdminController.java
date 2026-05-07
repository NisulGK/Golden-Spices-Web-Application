package com.kanishka.demo.admin;

import com.kanishka.demo.Order.Order;
import com.kanishka.demo.Order.OrderRepository;
import com.kanishka.demo.catalog.ProductRepository;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {

        List<Order> allOrders = orderRepository.findAll();

        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalRevenue", totalRevenue);

        model.addAttribute("recentOrders",
                orderRepository.findAllByOrderByCreatedAtDesc()
                        .stream().limit(5).toList());

        return "admin/dashboard";
    }
}
