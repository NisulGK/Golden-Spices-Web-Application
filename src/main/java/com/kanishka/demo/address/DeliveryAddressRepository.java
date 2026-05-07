package com.kanishka.demo.address;

import com.kanishka.demo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {

    List<DeliveryAddress> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);

    Optional<DeliveryAddress> findByUserAndIsDefaultTrue(User user);

    long countByUser(User user);

    @Modifying @Transactional
    @Query("UPDATE DeliveryAddress a SET a.isDefault = false WHERE a.user = :user")
    void clearDefaultForUser(@Param("user") User user);
}