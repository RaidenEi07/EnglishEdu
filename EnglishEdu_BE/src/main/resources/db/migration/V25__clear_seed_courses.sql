-- V25: Remove all seed courses that have no Moodle link.
-- These are the 24 courses from V8__seed_courses.sql that were seeded before
-- Moodle integration. They are now orphaned because Moodle was reset with a
-- fresh DB and the moodle_course_id references no longer match.
--
-- ON DELETE CASCADE on enrollments, course_reviews, and course_teachers
-- means those rows are cleaned up automatically.
--
-- After this migration runs, trigger:
--   POST /api/v1/admin/moodle/import-courses
-- to pull all real Moodle courses into the local DB.

DELETE FROM courses WHERE moodle_course_id IS NULL;
