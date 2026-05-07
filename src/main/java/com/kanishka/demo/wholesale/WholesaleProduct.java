package com.kanishka.demo.wholesale;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "wholesale_products")
public class WholesaleProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    @Column(name = "minimum_quantity", nullable = false)
    private Integer minimumQuantity;

    private boolean active = true;
}
