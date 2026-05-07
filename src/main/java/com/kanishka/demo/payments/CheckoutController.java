package com.kanishka.demo.payments;

import com.kanishka.demo.Order.Order;
import com.kanishka.demo.Order.OrderService;
import com.kanishka.demo.cart.Cart;
import com.kanishka.demo.cart.CartItem;
import com.kanishka.demo.cart.CartItemEntity;
import com.kanishka.demo.cart.CartService;
import com.kanishka.demo.user.User;
import com.kanishka.demo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final MockPaymentValidator mockPaymentValidator;
    private final InvoicePdfService invoicePdfService;
    private final EmailService emailService;

    @GetMapping
    public String checkoutPage(@AuthenticationPrincipal UserDetails principal,
                               Model model) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        Cart cart = cartService.getCartByUser(principal.getUsername());

        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal total = cart.getItems().stream()
                .map(CartItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        userRepository.findByEmail(principal.getUsername()).ifPresent(u -> {
            model.addAttribute("userEmail", u.getEmail());
            model.addAttribute("userName", u.getFullName());
            model.addAttribute("userPhone", u.getPhone());
            model.addAttribute("userAddress", u.getAddress());
        });

        model.addAttribute("cart", cart);
        model.addAttribute("items", cart.getItems());
        model.addAttribute("total", total);

        return "checkout/checkout";
    }

    @PostMapping("/create-order")
    public String createOrder(@RequestParam String customerName,
                              @RequestParam String customerEmail,
                              @RequestParam String customerPhone,
                              @RequestParam String deliveryAddress,
                              @RequestParam(required = false) String notes,
                              @RequestParam String paymentMethod,

                              @RequestParam(required = false) String cardName,
                              @RequestParam(required = false) String cardNumber,
                              @RequestParam(required = false) String expiryMonth,
                              @RequestParam(required = false) String expiryYear,
                              @RequestParam(required = false) String cvv,

                              @AuthenticationPrincipal UserDetails principal,
                              RedirectAttributes ra) {

        if (principal == null) {
            return "redirect:/auth/login";
        }

        if (customerName == null || customerName.trim().isBlank()) {
            ra.addFlashAttribute("error", "Full name is required.");
            return "redirect:/checkout";
        }

        if (customerEmail == null
                || !customerEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            ra.addFlashAttribute("error", "A valid email address is required.");
            return "redirect:/checkout";
        }

        if (customerPhone == null || customerPhone.trim().isBlank()) {
            ra.addFlashAttribute("error", "Phone number is required.");
            return "redirect:/checkout";
        }

        if (deliveryAddress == null || deliveryAddress.trim().isBlank()) {
            ra.addFlashAttribute("error", "Delivery address is required.");
            return "redirect:/checkout";
        }

        if (!"cod".equalsIgnoreCase(paymentMethod) && !"card".equalsIgnoreCase(paymentMethod)) {
            ra.addFlashAttribute("error", "Invalid payment method.");
            return "redirect:/checkout";
        }

        try {
            Cart cart = cartService.getCartByUser(principal.getUsername());

            if (cart.getItems().isEmpty()) {
                ra.addFlashAttribute("error", "Your cart is empty.");
                return "redirect:/cart";
            }

            if ("card".equalsIgnoreCase(paymentMethod)) {
                mockPaymentValidator.validateCardPayment(
                        cardName, cardNumber, expiryMonth, expiryYear, cvv
                );
            }

            List<CartItem> cartItems = cart.getItems().stream()
                    .map(e -> new CartItem(
                            e.getProduct().getId(),
                            e.getProduct().getName(),
                            e.getProduct().getBrand(),
                            e.getProduct().getSizeLabel(),
                            e.getProduct().getPriceLkr(),
                            e.getQuantity(),
                            e.getProduct().getStockQty(),
                            e.getProduct().getImageUrl()
                    ))
                    .collect(Collectors.toList());

            User user = userRepository.findByEmail(principal.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Order order = orderService.createOrder(
                    cartItems,
                    user,
                    customerName.trim(),
                    customerEmail.trim().toLowerCase(),
                    customerPhone.trim(),
                    deliveryAddress.trim(),
                    notes != null ? notes.trim() : null
            );

            if ("card".equalsIgnoreCase(paymentMethod)) {
                String paymentRef = "MOCK-CARD-" + System.currentTimeMillis();

                orderService.updatePaymentStatus(
                        order.getOrderNumber(),
                        PaymentStatus.PAID,
                        paymentRef
                );

                Order paidOrder = orderService.getOrderByNumber(order.getOrderNumber());

                // Attempt to send invoice email. If mail is not configured,
                // log the error and continue — the order is already saved.
                String successMsg = "Card payment successful! Your order has been placed.";
                try {
                    byte[] pdfBytes = invoicePdfService.generateInvoice(paidOrder);
                    emailService.sendInvoiceEmail(
                            paidOrder.getCustomerEmail(),
                            paidOrder.getCustomerName(),
                            paidOrder.getOrderNumber(),
                            pdfBytes
                    );
                    successMsg = "Card payment successful. Receipt has been sent to your email.";
                } catch (Exception mailEx) {
                    System.err.println("[WARN] Invoice email could not be sent for order "
                            + paidOrder.getOrderNumber() + ": " + mailEx.getMessage());
                }

                cartService.clear(principal.getUsername());

                ra.addFlashAttribute("success", successMsg);

                return "redirect:/checkout/success?orderNumber=" + paidOrder.getOrderNumber();
            }

            cartService.clear(principal.getUsername());

            ra.addFlashAttribute("success",
                    "Order placed! Order number: " + order.getOrderNumber());

            return "redirect:/checkout/success?orderNumber=" + order.getOrderNumber();

        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Unexpected error. Please try again.");
            return "redirect:/checkout";
        }
    }

    @GetMapping("/success")
    public String success(@RequestParam String orderNumber, Model model) {
        model.addAttribute("orderNumber", orderNumber);
        return "checkout/success";
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "checkout/cancel";
    }
}