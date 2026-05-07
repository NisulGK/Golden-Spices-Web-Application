package com.kanishka.demo.wholesale;

import com.kanishka.demo.payments.EmailService;
import com.kanishka.demo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WholesaleOrderService {

    private final WholesaleOrderRepository   orderRepository;
    private final WholesaleProductRepository productRepository;
    private final EmailService              emailService;

    /** Global minimum quantity enforced across ALL wholesale products. */
    public static final int GLOBAL_MIN_QUANTITY = 50;

    // =========================================================
    // CREATE ORDER — now returns the saved WholesaleOrder
    // =========================================================
    public WholesaleOrder createOrder(User user,
                                      Long productId,
                                      int quantity,
                                      String paymentStatus) {

        if (!Boolean.TRUE.equals(user.getWholesaleApproved())) {
            throw new RuntimeException("Your wholesale account has not been approved yet.");
        }

        WholesaleProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found. It may have been removed."));

        if (!product.isActive()) {
            throw new RuntimeException("This product is no longer available.");
        }

        // ── GLOBAL minimum: 50 units ────────────────────────────
        if (quantity < GLOBAL_MIN_QUANTITY) {
            throw new RuntimeException(
                    "Minimum wholesale order quantity is " + GLOBAL_MIN_QUANTITY
                            + " units. You entered " + quantity + ". "
                            + "Please increase your quantity to continue.");
        }

        // ── Product-specific minimum (may be higher than global) ─
        int minQty = product.getMinimumQuantity();
        if (quantity < minQty) {
            throw new RuntimeException(
                    "Minimum order quantity for \"" + product.getName()
                            + "\" is " + minQty + " units. You entered " + quantity + ".");
        }

        BigDecimal unitPrice = product.getPrice();
        BigDecimal total     = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // ── Bulk discount: > 100 units → 10% off ────────────────
        if (quantity > 100) {
            total = total.multiply(new BigDecimal("0.90"));
        }

        // ── Sanitise paymentStatus ──────────────────────────────
        if (!"PAID".equals(paymentStatus) && !"PAY_LATER".equals(paymentStatus)) {
            paymentStatus = "PAY_LATER";
        }

        WholesaleOrder order = WholesaleOrder.builder()
                .user(user)
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(total)
                .status("PENDING")
                .paymentStatus(paymentStatus)
                .build();

        WholesaleOrder savedOrder = orderRepository.save(order);
        
        // ── Send confirmation email to user ──────────────────────
        try {
            emailService.sendWholesaleOrderConfirmation(
                    user.getEmail(),
                    user.getFullName(),
                    savedOrder.getId(),
                    product.getName(),
                    quantity,
                    total.toString()
            );
        } catch (Exception e) {
            // Log the error but don't fail the order creation
            System.err.println("⚠️ Warning: Failed to send confirmation email: " + e.getMessage());
        }
        
        return savedOrder;
    }

    // =========================================================
    // UPDATE ORDER  (PENDING orders only)
    // =========================================================
    public void updateOrder(WholesaleOrder order, int quantity) {

        WholesaleProduct product = order.getProduct();
        if (product == null) {
            throw new RuntimeException("Order product is missing — cannot update.");
        }

        // Global minimum check
        if (quantity < GLOBAL_MIN_QUANTITY) {
            throw new RuntimeException(
                    "Minimum wholesale order quantity is " + GLOBAL_MIN_QUANTITY
                            + " units. You entered " + quantity + ".");
        }

        int minQty = product.getMinimumQuantity();
        if (quantity < minQty) {
            throw new RuntimeException(
                    "Minimum order quantity for \"" + product.getName()
                            + "\" is " + minQty + " units. You entered " + quantity + ".");
        }

        BigDecimal unitPrice = product.getPrice();
        BigDecimal total     = unitPrice.multiply(BigDecimal.valueOf(quantity));

        if (quantity > 100) {
            total = total.multiply(new BigDecimal("0.90"));
        }

        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(total);

        orderRepository.save(order);
    }
}