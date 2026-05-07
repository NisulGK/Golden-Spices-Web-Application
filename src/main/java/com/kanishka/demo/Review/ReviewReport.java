package com.kanishka.demo.Review;

import com.kanishka.demo.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "reported_by_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    private Boolean resolved = false;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ReportReason {
        SPAM,
        OFFENSIVE_LANGUAGE,
        FAKE_REVIEW,
        IRRELEVANT,
        OTHER
    }
}