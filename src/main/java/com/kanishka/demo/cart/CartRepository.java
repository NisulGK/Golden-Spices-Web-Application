package com.kanishka.demo.cart;

import com.kanishka.demo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(Long userId);

    // JOIN FETCH items so they're always loaded in one query.
    // This prevents stale EAGER cache — after entityManager.clear(),
    // this query forces a fresh DB read including current items.
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product WHERE c.user.email = :email")
    Optional<Cart> findByUserEmail(@Param("email") String email);

    boolean existsByUser(User user);

    void deleteByUser(User user);
}