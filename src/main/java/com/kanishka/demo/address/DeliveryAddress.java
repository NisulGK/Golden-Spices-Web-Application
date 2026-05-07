package com.kanishka.demo.address;

import com.kanishka.demo.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String label;           // e.g. "Home", "Office"

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String addressLine1;

    private String addressLine2;

    @Column(nullable = false)
    private String city;

    private String district;

    @Column(nullable = false)
    private String postalCode;

    @Builder.Default
    private Boolean isDefault = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    /** Formatted one-line address for display */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder(addressLine1);
        if (addressLine2 != null && !addressLine2.isBlank()) sb.append(", ").append(addressLine2);
        sb.append(", ").append(city);
        if (district != null && !district.isBlank()) sb.append(", ").append(district);
        sb.append(" ").append(postalCode);
        return sb.toString();
    }
}