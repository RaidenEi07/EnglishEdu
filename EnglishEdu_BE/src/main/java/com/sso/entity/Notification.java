package com.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 500)
    private String link;

    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    @Column(length = 50)
    @Builder.Default
    private String type = "GENERAL";  // GENERAL, ENROLLMENT_APPROVED, ENROLLMENT_REVOKED, COURSE_UPDATE, PAYMENT, REVIEW

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
