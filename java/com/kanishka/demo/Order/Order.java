package com.kanishka.demo.Order;

import com.kanishka.demo.payments.PaymentStatus;
import com.kanishka.demo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String paymentIntentId;

    private LocalDateTime estimatedDelivery;
    private String currentLocation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();

        if (orderNumber == null) {
            orderNumber = "ORD" + System.currentTimeMillis();
        }

        if (estimatedDelivery == null) {
            estimatedDelivery = LocalDateTime.now().plusDays(3);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setStripeSessionId(String id) {
        // optional placeholder if you want to store Stripe session id later
    }

    // ✅ FIX: total quantity across all order lines
    public int getTotalItemCount() {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        return items.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();
    }
}