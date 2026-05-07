package com.kanishka.demo.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    List<Product> findByBrandAndActiveTrue(String brand);

    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);
}