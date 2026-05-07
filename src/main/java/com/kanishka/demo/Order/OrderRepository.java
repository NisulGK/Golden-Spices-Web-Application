package com.kanishka.demo.Order;

import com.kanishka.demo.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findByOrderNumber(String orderNumber);

    // FIX: added @EntityGraph so items are eagerly loaded —
    // prevents LazyInitializationException in orders/list.html
    @EntityGraph(attributePaths = {"items"})
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findAllByOrderByCreatedAtDesc();
}