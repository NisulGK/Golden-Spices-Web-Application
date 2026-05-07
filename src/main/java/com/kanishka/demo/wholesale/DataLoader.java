package com.kanishka.demo.wholesale;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader {

    private final WholesaleProductRepository repository;

    @PostConstruct
    public void load() {

        if (repository.count() == 0) {

            repository.save(
                    WholesaleProduct.builder()
                            .name("Bulk Cinnamon 1kg")
                            .price(new BigDecimal("1200"))
                            .minimumQuantity(50)   // ✅ REQUIRED FIELD
                            .active(true)
                            .build()
            );

            repository.save(
                    WholesaleProduct.builder()
                            .name("Bulk Pepper 1kg")
                            .price(new BigDecimal("1500"))
                            .minimumQuantity(30)   // ✅ REQUIRED FIELD
                            .active(true)
                            .build()
            );

            repository.save(
                    WholesaleProduct.builder()
                            .name("Bulk Turmeric 1kg")
                            .price(new BigDecimal("1000"))
                            .minimumQuantity(40)
                            .active(true)
                            .build()
            );
        }
    }
}
