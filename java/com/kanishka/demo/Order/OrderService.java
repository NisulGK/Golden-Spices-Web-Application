package com.kanishka.demo.Order;

import com.kanishka.demo.cart.CartItem;
import com.kanishka.demo.catalog.Product;
import com.kanishka.demo.catalog.ProductRepository;
import com.kanishka.demo.payments.PaymentStatus;
import com.kanishka.demo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderTrackingRepository trackingRepository;

    /* ═══════════════════════════════════════
       CREATE ORDER
    ═══════════════════════════════════════ */
    @Transactional
    public Order createOrder(List<CartItem> cartItems, User user,
                             String customerName, String customerEmail,
                             String customerPhone, String deliveryAddress,
                             String notes) {

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        BigDecimal total = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .deliveryAddress(deliveryAddress)
                .totalAmount(total)
                .notes(notes)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .estimatedDelivery(LocalDateTime.now().plusDays(3))
                .build();

        for (CartItem ci : cartItems) {
            Product p = productRepository.findById(ci.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + ci.productId()));

            OrderItem oi = OrderItem.builder()
                    .product(p)
                    .productName(ci.name())
                    .productSize(ci.size())
                    .unitPrice(ci.price())
                    .quantity(ci.qty())
                    .build();

            order.addItem(oi);

            if (p.getStockQty() != null && p.getStockQty() >= ci.qty()) {
                p.setStockQty(p.getStockQty() - ci.qty());
                productRepository.save(p);
            }
        }

        Order saved = orderRepository.save(order);

        trackingRepository.save(OrderTracking.builder()
                .order(saved)
                .status(OrderStatus.PENDING)
                .title("Order Placed")
                .description("Your order has been received and is awaiting payment.")
                .build());

        return saved;
    }

    /* ═══════════════════════════════════════
       UPDATE ORDER STATUS (admin / staff)
    ═══════════════════════════════════════ */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus,
                                  String note, String updatedBy) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.CONFIRMED && order.getEstimatedDelivery() == null) {
            order.setEstimatedDelivery(LocalDateTime.now().plusDays(3));
        }

        orderRepository.save(order);

        String title = statusTitle(newStatus);
        String desc = (note != null && !note.isBlank())
                ? note
                : statusDescription(newStatus);

        if (updatedBy != null && !updatedBy.isBlank()) {
            desc = desc + " — " + updatedBy;
        }

        trackingRepository.save(OrderTracking.builder()
                .order(order)
                .status(newStatus)
                .title(title)
                .description(desc)
                .build());
    }

    /* ✅ FIX: admin controller calls this method */
    @Transactional
    public void updateOrderStatus(Long id, OrderStatus newStatus) {
        updateOrderStatus(id, newStatus, null, "Admin");
    }

    /* ═══════════════════════════════════════
       UPDATE LOCATION (staff)
    ═══════════════════════════════════════ */
    @Transactional
    public void updateLocation(Long orderId, String location) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setCurrentLocation(location);
        orderRepository.save(order);
    }

    /* ═══════════════════════════════════════
       PAYMENT
    ═══════════════════════════════════════ */
    @Transactional
    public void updatePaymentStatus(String orderNumber, PaymentStatus newStatus,
                                    String paymentIntentId) {

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        order.setPaymentStatus(newStatus);
        order.setPaymentIntentId(paymentIntentId);

        if (newStatus == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.CONFIRMED);

            trackingRepository.save(OrderTracking.builder()
                    .order(order)
                    .status(OrderStatus.CONFIRMED)
                    .title("Payment Confirmed")
                    .description("Payment received. Your order is now confirmed.")
                    .build());
        }

        orderRepository.save(order);
    }

    /* ═══════════════════════════════════════
       TRACKING HISTORY
    ═══════════════════════════════════════ */
    public List<OrderTracking> getTrackingForOrder(Long orderId) {
        return trackingRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /* ═══════════════════════════════════════
       FINDERS
    ═══════════════════════════════════════ */
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    /* ═══════════════════════════════════════
       HELPERS
    ═══════════════════════════════════════ */
    private String statusTitle(OrderStatus s) {
        return switch (s) {
            case PENDING -> "Order Placed";
            case CONFIRMED -> "Order Confirmed";
            case PROCESSING -> "Processing";
            case SHIPPED -> "Shipped";
            case OUT_FOR_DELIVERY -> "Out for Delivery";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
        };
    }

    private String statusDescription(OrderStatus s) {
        return switch (s) {
            case PENDING -> "Your order has been received.";
            case CONFIRMED -> "Your order has been confirmed and will be prepared soon.";
            case PROCESSING -> "Your order is being packed and prepared for dispatch.";
            case SHIPPED -> "Your order has been dispatched and is on its way.";
            case OUT_FOR_DELIVERY -> "Your order is out for delivery today.";
            case DELIVERED -> "Your order has been delivered successfully.";
            case CANCELLED -> "Your order has been cancelled.";
        };
    }
}