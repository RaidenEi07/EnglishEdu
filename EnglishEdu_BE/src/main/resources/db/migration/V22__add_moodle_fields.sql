-- V22: Add Moodle integration fields
ALTER TABLE users   ADD COLUMN IF NOT EXISTS moodle_id        BIGINT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS moodle_course_id BIGINT;
