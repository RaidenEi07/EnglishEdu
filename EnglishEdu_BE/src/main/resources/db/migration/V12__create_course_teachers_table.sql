-- ============================================
-- V12: Course Teachers (Many-to-Many)
-- ============================================

CREATE TABLE course_teachers (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    teacher_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL DEFAULT 'PRIMARY',  -- PRIMARY, ASSISTANT
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE(course_id, teacher_id)
);

CREATE INDEX idx_course_teachers_course  ON course_teachers(course_id);
CREATE INDEX idx_course_teachers_teacher ON course_teachers(teacher_id);

-- Migrate existing teacher_id data (keep old column for backward compat)
INSERT INTO course_teachers (course_id, teacher_id, role)
SELECT id, teacher_id, 'PRIMARY'
FROM courses
WHERE teacher_id IS NOT NULL
ON CONFLICT DO NOTHING;
