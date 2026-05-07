package com.kanishka.demo.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {

    List<OrderTracking> findByOrderOrderByCreatedAtAsc(Order order);

    List<OrderTracking> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}