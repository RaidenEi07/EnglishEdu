package com.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 20)
    @Builder.Default
    private String status = "pending";

    @Builder.Default
    private Integer progress = 0;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private Instant enrolledAt;

    @Column(name = "last_accessed")
    private Instant lastAccessed;

    @Column(name = "request_date")
    private Instant requestDate;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "teacher_note", columnDefinition = "TEXT")
    private String teacherNote;

    @Builder.Default
    private boolean starred = false;

    @Builder.Default
    private boolean hidden = false;

    @Column(name = "expiry_date")
    private Instant expiryDate;
}
