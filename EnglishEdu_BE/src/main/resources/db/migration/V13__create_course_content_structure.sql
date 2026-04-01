-- ============================================
-- V13: Course Content Structure (Module -> Lesson)
-- ============================================

-- 1. Course Modules
CREATE TABLE course_modules (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_course_modules_course ON course_modules(course_id);

-- 2. Lessons within modules
CREATE TABLE lessons (
    id          BIGSERIAL PRIMARY KEY,
    module_id   BIGINT NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    content     TEXT,
    type        VARCHAR(30) NOT NULL DEFAULT 'TEXT',  -- TEXT, VIDEO, DOCUMENT, QUIZ
    video_url   VARCHAR(1000),
    duration    INT,                                   -- duration in minutes
    sort_order  INT DEFAULT 0,
    is_free     BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lessons_module ON lessons(module_id);
