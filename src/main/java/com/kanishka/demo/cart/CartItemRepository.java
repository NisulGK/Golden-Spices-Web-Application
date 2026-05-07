package com.kanishka.demo.cart;

import com.kanishka.demo.catalog.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCartId(Long cartId);

    @Query("SELECT ci FROM CartItemEntity ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    Optional<CartItemEntity> findByCartIdAndProductId(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItemEntity ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    int deleteByCartIdAndProductId(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItemEntity ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);

    List<CartItemEntity> findByProduct(Product product);
}