package com.kanishka.demo.wholesale;

import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Month;
import java.util.*;

@Controller
@RequestMapping("/wholesale")
@RequiredArgsConstructor
public class WholesaleController {

    private final UserRepository             userRepository;
    private final WholesaleOrderRepository   orderRepository;
    private final WholesaleProductRepository productRepository;
    private final WholesaleOrderService      service;
    private final WholesaleReceiptPdfService receiptPdfService;

    // =====================================================
    // DASHBOARD — Users only. Admins are redirected away.
    // =====================================================
    @GetMapping
    public String dashboard(Model model, Authentication auth) {

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return "redirect:/admin/wholesale/dashboard";
        }

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();

        List<WholesaleOrder> orders =
                orderRepository.findByUserOrderByCreatedAtDesc(user);

        BigDecimal totalAmount = orders.stream()
                .map(o -> Optional.ofNullable(o.getTotalAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount  = countByStatus(orders, "PENDING");
        long approvedCount = countByStatus(orders, "APPROVED");
        long shippedCount  = countByStatus(orders, "SHIPPED");

        Map<String, BigDecimal> monthlyTotals = generateMonthlyTotals(orders);

        model.addAttribute("orders",        orders);
        model.addAttribute("products",      productRepository.findByActiveTrue());
        model.addAttribute("totalAmount",   totalAmount);
        model.addAttribute("pendingCount",  pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("shippedCount",  shippedCount);
        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("approved",      Boolean.TRUE.equals(user.getWholesaleApproved()));
        model.addAttribute("requested",     Boolean.TRUE.equals(user.getWholesaleRequested()));
        model.addAttribute("user",          user);

        return "wholesale/dashboard";
    }

    // =====================================================
    // PRODUCTS PAGE
    // =====================================================
    @GetMapping("/products")
    public String products(Model model, Authentication auth, RedirectAttributes ra) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (!Boolean.TRUE.equals(user.getWholesaleApproved())) {
            ra.addFlashAttribute("error", "You need an approved wholesale account to view products.");
            return "redirect:/wholesale";
        }
        model.addAttribute("products",     productRepository.findByActiveTrue());
        model.addAttribute("minQty",       WholesaleOrderService.GLOBAL_MIN_QUANTITY);
        return "wholesale/products";
    }

    // =====================================================
    // ORDER DETAIL
    // =====================================================
    @GetMapping("/order/{id}/detail")
    public String orderDetail(@PathVariable Long id,
                              Authentication auth,
                              Model model,
                              RedirectAttributes ra) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        WholesaleOrder order = orderRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/wholesale";
        }
        model.addAttribute("order", order);
        return "wholesale/detail";
    }

    // =====================================================
    // REQUEST WHOLESALE ACCESS
    // =====================================================
    @PostMapping("/request-access")
    public String requestAccess(Authentication auth, RedirectAttributes ra) {
        return handleRequestAccess(auth, ra);
    }

    @PostMapping("/request")
    public String requestAccessAlias(Authentication auth, RedirectAttributes ra) {
        return handleRequestAccess(auth, ra);
    }

    private String handleRequestAccess(Authentication auth, RedirectAttributes ra) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();

        if (Boolean.TRUE.equals(user.getWholesaleApproved())) {
            ra.addFlashAttribute("success", "Your wholesale account is already approved.");
            return "redirect:/wholesale";
        }
        if (Boolean.TRUE.equals(user.getWholesaleRequested())) {
            ra.addFlashAttribute("success",
                    "Wholesale request already submitted. Please wait for admin approval.");
            return "redirect:/wholesale";
        }

        user.setWholesaleRequested(true);
        userRepository.save(user);
        ra.addFlashAttribute("success", "Wholesale access request submitted successfully.");
        return "redirect:/wholesale";
    }

    // =====================================================
    // CREATE WHOLESALE ORDER
    // =====================================================
    @PostMapping("/order")
    public String createOrder(Authentication auth,
                              @RequestParam Long productId,
                              @RequestParam int quantity,
                              @RequestParam(required = false, defaultValue = "PAY_LATER")
                              String paymentStatus,
                              RedirectAttributes ra) {

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();

        if (!Boolean.TRUE.equals(user.getWholesaleApproved())) {
            ra.addFlashAttribute("error", "Your wholesale account is not approved yet.");
            return "redirect:/wholesale";
        }

        // Whitelist payment statuses
        if (!"PAID".equals(paymentStatus) && !"PAY_LATER".equals(paymentStatus)) {
            paymentStatus = "PAY_LATER";
        }

        // ── Validate minimum 50 upfront so the error is shown before saving ──
        if (quantity < WholesaleOrderService.GLOBAL_MIN_QUANTITY) {
            ra.addFlashAttribute("error",
                    "Minimum wholesale order quantity is "
                            + WholesaleOrderService.GLOBAL_MIN_QUANTITY
                            + " units. You entered " + quantity + ".");
            return "redirect:/wholesale/products";
        }

        try {
            WholesaleOrder savedOrder =
                    service.createOrder(user, productId, quantity, paymentStatus);

            // ── If user chose to pay now, redirect to payment page ──
            if ("PAID".equals(paymentStatus)) {
                return "redirect:/wholesale/payment/" + savedOrder.getId();
            }

            ra.addFlashAttribute("success",
                    "Wholesale order #" + savedOrder.getId() + " placed successfully! "
                            + "We will send you an invoice shortly.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to place order: " + e.getMessage());
            return "redirect:/wholesale/products";
        }

        return "redirect:/wholesale";
    }

    // =====================================================
    // CARD PAYMENT — Demo
    // =====================================================

    /** Show the card payment form for a PAID order. */
    @GetMapping("/payment/{id}")
    public String paymentPage(@PathVariable Long id,
                              Authentication auth,
                              Model model,
                              RedirectAttributes ra) {

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        WholesaleOrder order = orderRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/wholesale";
        }

        model.addAttribute("order", order);
        model.addAttribute("user",  user);
        return "wholesale/payment";
    }

    /** Process the demo card payment and redirect to receipt. */
    @PostMapping("/payment/{id}/process")
    public String processPayment(@PathVariable Long id,
                                 @RequestParam(required = false) String cardName,
                                 @RequestParam(required = false) String cardNumber,
                                 Authentication auth,
                                 RedirectAttributes ra) {

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        WholesaleOrder order = orderRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/wholesale";
        }

        // Demo: always mark as paid successfully
        order.setPaymentStatus("PAID");
        orderRepository.save(order);

        return "redirect:/wholesale/receipt/" + id;
    }

    // =====================================================
    // PAYMENT RECEIPT
    // =====================================================
    @GetMapping("/receipt/{id}")
    public String receipt(@PathVariable Long id,
                          Authentication auth,
                          Model model,
                          RedirectAttributes ra) {

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        WholesaleOrder order = orderRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/wholesale";
        }

        model.addAttribute("order", order);
        model.addAttribute("user",  user);
        return "wholesale/receipt";
    }

    // =====================================================
    // EDIT WHOLESALE ORDER (PENDING only)
    // =====================================================
    @PostMapping("/order/{id}/edit")
    public String editOrder(@PathVariable Long id,
                            @RequestParam int quantity,
                            Authentication auth,
                            RedirectAttributes ra) {

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        WholesaleOrder order = orderRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/wholesale";
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            ra.addFlashAttribute("error", "Only PENDING orders can be edited.");
            return "redirect:/wholesale";
        }

        try {
            service.updateOrder(order, quantity);
            ra.addFlashAttribute("success", "Order #" + id + " updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update order: " + e.getMessage());
        }

        return "redirect:/wholesale";
    }

    // =====================================================
    // DELETE WHOLESALE ORDER (PENDING only)
    // =====================================================
    @PostMapping("/order/{id}/delete")
    public String deleteOrder(@PathVariable Long id,
                              Authentication auth,
                              RedirectAttributes ra) {

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        WholesaleOrder order = orderRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/wholesale";
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            ra.addFlashAttribute("error", "Only PENDING orders can be deleted.");
            return "redirect:/wholesale";
        }

        orderRepository.deleteById(id);
        ra.addFlashAttribute("success", "Order #" + id + " deleted successfully.");
        return "redirect:/wholesale";
    }

    // =====================================================

    // =====================================================
    // DOWNLOAD PDF RECEIPT
    // =====================================================
    @GetMapping("/receipt/{id}/download")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id,
                                                  Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        WholesaleOrder order = orderRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] pdf = receiptPdfService.generate(order, user);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "receipt-order-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // HELPERS
    // =====================================================
    private long countByStatus(List<WholesaleOrder> orders, String status) {
        return orders.stream()
                .filter(o -> status.equalsIgnoreCase(o.getStatus()))
                .count();
    }

    private Map<String, BigDecimal> generateMonthlyTotals(List<WholesaleOrder> orders) {
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        for (Month month : Month.values()) {
            monthly.put(month.name(), BigDecimal.ZERO);
        }
        for (WholesaleOrder order : orders) {
            if (order.getCreatedAt() == null) continue;
            String month = order.getCreatedAt().getMonth().name();
            monthly.put(month, monthly.get(month)
                    .add(Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO)));
        }
        return monthly;
    }
}