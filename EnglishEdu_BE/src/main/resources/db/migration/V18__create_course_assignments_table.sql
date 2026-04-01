-- ============================================================
-- V18: Course Assignments
-- Admin assigns specific courses to students.
-- Students can ONLY enroll in courses they have been assigned.
-- ============================================================

CREATE TABLE course_assignments (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    course_id   BIGINT      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    assigned_by BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT  uk_course_assignment_user_course UNIQUE (user_id, course_id)
);

CREATE INDEX idx_course_assignments_user   ON course_assignments(user_id);
CREATE INDEX idx_course_assignments_course ON course_assignments(course_id);
