-- ============================================================
--  Moodle cleanup script – run BEFORE exporting DB to production
--  Removes test/fake users and test courses
-- ============================================================

-- Step 1: Remove fake users' role assignments
DELETE FROM mdl_role_assignments WHERE userid IN (3, 4, 5, 7);

-- Step 2: Remove fake users' enrolments
DELETE FROM mdl_user_enrolments
WHERE userid IN (3, 4, 5, 7);

-- Step 3: Soft-delete fake users (Moodle uses deleted=1, not hard delete)
UPDATE mdl_user
SET deleted   = 1,
    username  = CONCAT('deleted_', id, '_', username),
    email     = CONCAT('deleted_', id, '@invalid.local'),
    suspended = 1
WHERE id IN (3, 4, 5, 7);
-- Note: id=6 (student2 – Thịnh Đỗ) kept – decide manually

-- Step 4: Delete test course (id=3, shortname='test')
-- First remove enrolments in that course
DELETE ue FROM mdl_user_enrolments ue
JOIN mdl_enrol e ON e.id = ue.enrolid
WHERE e.courseid = 3;

DELETE FROM mdl_enrol WHERE courseid = 3;

-- Delete course context and course itself
-- (Moodle cascades via course_delete_course in PHP, but for DB-only cleanup:)
DELETE FROM mdl_context WHERE instanceid = 3 AND contextlevel = 50;
DELETE FROM mdl_course WHERE id = 3;

-- Step 5: Clear all session data (force re-login on production)
DELETE FROM mdl_sessions;
