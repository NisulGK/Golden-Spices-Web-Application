package com.kanishka.demo.wholesale;

import com.kanishka.demo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WholesaleAccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private String businessRegistrationNo;

    private boolean approved = false;

    private LocalDateTime requestedAt;

    @ManyToOne
    private User user;

    @PrePersist
    void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}
