package com.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private Integer externalId;

    @Column(nullable = false, length = 500)
    private String name;

    // Legacy VARCHAR columns kept for backward compat
    @Column(length = 50)
    private String category;

    @Column(length = 100)
    private String level;

    // New FK-based category & level
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category categoryEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level levelEntity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Legacy single teacher (kept for backward compat)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    // New many-to-many teachers
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 30)
    @Builder.Default
    private List<CourseTeacher> courseTeachers = new ArrayList<>();

    @Column(name = "is_published")
    @Builder.Default
    private boolean published = true;

    // Pricing
    @Column(name = "is_free")
    @Builder.Default
    private boolean free = true;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    // Cached review stats
    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "moodle_course_id")
    private Long moodleCourseId;
}
