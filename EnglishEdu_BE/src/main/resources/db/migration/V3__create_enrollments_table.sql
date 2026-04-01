CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'inprogress',
    progress INTEGER NOT NULL DEFAULT 0,
    enrolled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_accessed TIMESTAMP,
    CONSTRAINT uk_enrollment_user_course UNIQUE (user_id, course_id)
);

CREATE INDEX idx_enrollments_user ON enrollments(user_id);
CREATE INDEX idx_enrollments_status ON enrollments(user_id, status);
