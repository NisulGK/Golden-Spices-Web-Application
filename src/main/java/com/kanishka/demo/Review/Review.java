package com.kanishka.demo.Review;

import com.kanishka.demo.catalog.Product;
import com.kanishka.demo.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rating;

    @Column(length = 1000)
    private String comment;

    // approved=true  → visible publicly
    // approved=false → hidden / pending moderation
    @Builder.Default
    private boolean approved = false;

    @Builder.Default
    private boolean deleted = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verifiedPurchase = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (verifiedPurchase == null) verifiedPurchase = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (verifiedPurchase == null) verifiedPurchase = false;
    }
}