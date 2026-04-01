-- ============================================
-- V14: Lesson Progress (auto-calculated enrollment progress)
-- ============================================

CREATE TABLE lesson_progress (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id    BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',  -- NOT_STARTED, IN_PROGRESS, COMPLETED
    completed_at TIMESTAMP,
    created_at   TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, lesson_id)
);

CREATE INDEX idx_lesson_progress_user   ON lesson_progress(user_id);
CREATE INDEX idx_lesson_progress_lesson ON lesson_progress(lesson_id);
