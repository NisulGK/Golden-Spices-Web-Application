package com.kanishka.demo.wholesale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WholesaleOrderRepository extends JpaRepository<WholesaleOrder, Long> {

    // Eagerly fetch product AND user to avoid LazyInitializationException on payment/receipt/detail pages
    @Query("SELECT o FROM WholesaleOrder o LEFT JOIN FETCH o.product LEFT JOIN FETCH o.user WHERE o.id = :id")
    Optional<WholesaleOrder> findByIdWithProduct(Long id);

    // Eagerly join-fetch user AND product to avoid LazyInitializationException in Thymeleaf
    @Query("SELECT o FROM WholesaleOrder o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.product")
    List<WholesaleOrder> findAllWithUserAndProduct();

    // Legacy — kept for compatibility
    @Query("SELECT o FROM WholesaleOrder o LEFT JOIN FETCH o.user")
    List<WholesaleOrder> findAllWithUser();

    List<WholesaleOrder> findByUserEmail(String email);

    @Query("SELECT o FROM WholesaleOrder o LEFT JOIN FETCH o.product WHERE o.user = :user ORDER BY o.createdAt DESC")
    List<WholesaleOrder> findByUserOrderByCreatedAtDesc(com.kanishka.demo.user.User user);
}