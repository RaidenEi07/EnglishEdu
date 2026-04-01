-- V24: Add per-user Moodle token for student quiz/assignment operations
ALTER TABLE users ADD COLUMN IF NOT EXISTS moodle_token VARCHAR(255);
