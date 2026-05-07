package com.kanishka.demo.Review;

import com.kanishka.demo.catalog.Product;
import com.kanishka.demo.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // ── Used by ReviewController ──────────────────────────────────────────

    Optional<Review> findByUserAndProduct(User user, Product product);

    boolean existsByUserAndProduct(User user, Product product);

    // approved=true and deleted=false — matches boolean approved field in Review
    Page<Review> findByProductAndApprovedTrueAndDeletedFalse(
            Product product, Pageable pageable);

    long countByProductAndRatingAndApprovedTrueAndDeletedFalse(
            Product product, int rating);

    // Native SQL — avoids LazyInitializationException when checking purchase
    @Query(value = """
        SELECT CASE WHEN COUNT(oi.id) > 0 THEN 1 ELSE 0 END
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE o.user_id    = :userId
          AND oi.product_id = :productId
    """, nativeQuery = true)
    int hasUserOrderedProductNative(
            @Param("userId")    Long userId,
            @Param("productId") Long productId);


    // ── Used by AdminReviewController ─────────────────────────────────────

    // approved=true, not deleted
    List<Review> findByApprovedTrueAndDeletedFalseOrderByCreatedAtDesc();

    // all not deleted
    List<Review> findByDeletedFalseOrderByCreatedAtDesc();

    // approved=false (pending), not deleted, oldest first
    List<Review> findByApprovedFalseAndDeletedFalseOrderByCreatedAtAsc();

    long countByApprovedFalseAndDeletedFalse();

    long countByApprovedTrueAndDeletedFalse();
}