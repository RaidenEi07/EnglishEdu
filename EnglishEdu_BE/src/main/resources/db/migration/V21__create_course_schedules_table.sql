-- ============================================
-- V21: Course Schedules
-- Each course can have recurring weekly schedules managed by admin.
-- Students/teachers view these on their dashboard calendar.
-- ============================================

CREATE TABLE course_schedules (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    room        VARCHAR(100),
    note        VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_schedule_time CHECK (end_time > start_time)
);

CREATE INDEX idx_course_schedules_course ON course_schedules(course_id);
CREATE INDEX idx_course_schedules_day    ON course_schedules(day_of_week);
