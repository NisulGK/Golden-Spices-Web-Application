package com.kanishka.demo.wholesale;

import com.kanishka.demo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wholesale_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WholesaleOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===============================
    // RELATIONS
    // ===============================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private WholesaleProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ===============================
    // ORDER DETAILS
    // ===============================

    private int quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    /**
     * PENDING
     * APPROVED
     * SHIPPED
     */
    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING";

    /**
     * PAY_LATER
     * PAID
     */
    @Builder.Default
    @Column(nullable = false)
    private String paymentStatus = "PAY_LATER";

    private LocalDateTime createdAt;

    // ===============================
    // AUTO TIMESTAMP
    // ===============================

    @PrePersist
    public void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "PENDING";
        }

        if (paymentStatus == null) {
            paymentStatus = "PAY_LATER";
        }
    }
}
