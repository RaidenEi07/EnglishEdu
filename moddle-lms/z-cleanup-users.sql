-- ============================================================
--  Post-import cleanup: remove test users
--  File prefix 'z-' ensures this runs AFTER moodle-db-export.sql
--  Execute against: bitnami_moodle database (MariaDB)
-- ============================================================

USE bitnami_moodle;

-- ── Soft-delete student2 (id=6, student2@gmail.com) ─────────
-- Moodle never hard-deletes users; we follow the same convention.
UPDATE mdl_user SET
    deleted   = 1,
    suspended = 1,
    username  = 'student2.deleted',
    email     = 'student2.deleted@invalid.local',
    password  = 'x'
WHERE id = 6 AND username = 'student2';

-- ── Remove their role assignments ───────────────────────────
DELETE FROM mdl_role_assignments WHERE userid = 6;

-- ── Remove their course enrolments ──────────────────────────
DELETE FROM mdl_user_enrolments WHERE userid = 6;

-- ── Clear their notifications ───────────────────────────────
DELETE FROM mdl_notifications WHERE useridto = 6;

-- ── Clear their message inbox ───────────────────────────────
DELETE FROM mdl_messages WHERE useridfrom = 6 OR useridto = 6;
DELETE FROM mdl_message_conversation_members WHERE userid = 6;
