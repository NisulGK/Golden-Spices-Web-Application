package com.kanishka.demo.wholesale;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WholesaleProductRepository
        extends JpaRepository<WholesaleProduct, Long> {

    List<WholesaleProduct> findByActiveTrue();
}
