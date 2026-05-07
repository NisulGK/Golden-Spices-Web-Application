package com.kanishka.demo.Review;

import com.kanishka.demo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewAndReportedBy(Review review, User reportedBy);

    List<ReviewReport> findByResolvedFalseOrderByCreatedAtDesc();

    List<ReviewReport> findByReviewOrderByCreatedAtDesc(Review review);

    long countByResolvedFalse();
}