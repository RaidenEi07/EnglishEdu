package com.sso.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "course_teachers", uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "teacher_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CourseTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(length = 30, nullable = false)
    @Builder.Default
    private String role = "PRIMARY";  // PRIMARY, ASSISTANT

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
