-- ============================================
-- V16: Course Reviews & Ratings
-- ============================================

CREATE TABLE course_reviews (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating      INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment     TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE(course_id, user_id)   -- one review per user per course
);

CREATE INDEX idx_course_reviews_course ON course_reviews(course_id);
CREATE INDEX idx_course_reviews_user   ON course_reviews(user_id);

-- Add cached avg rating to courses for performance
ALTER TABLE courses ADD COLUMN avg_rating    DECIMAL(3, 2) DEFAULT 0;
ALTER TABLE courses ADD COLUMN review_count  INT DEFAULT 0;
