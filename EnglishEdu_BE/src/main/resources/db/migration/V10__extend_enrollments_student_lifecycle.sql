-- V10: Extend enrollments table to support full student lifecycle
--
-- Status lifecycle:
--   pending    → student sent request, waiting for approval
--   active     → approved by admin/teacher, not yet started
--   inprogress → student has started learning (progress > 0)
--   completed  → student finished (progress = 100)
--   revoked    → access revoked by admin

ALTER TABLE enrollments
    ALTER COLUMN status SET DEFAULT 'pending';

ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS request_date  TIMESTAMP;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS approved_at   TIMESTAMP;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS approved_by   BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS teacher_note  TEXT;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS starred       BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS hidden        BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS expiry_date   TIMESTAMP;

-- Migrate existing data: old 'inprogress' records are treated as already approved
UPDATE enrollments
SET    status      = 'active',
       approved_at = enrolled_at
WHERE  status = 'inprogress';

CREATE INDEX IF NOT EXISTS idx_enrollments_starred     ON enrollments(user_id, starred) WHERE starred = TRUE;
CREATE INDEX IF NOT EXISTS idx_enrollments_pending     ON enrollments(user_id, status)  WHERE status  = 'pending';
CREATE INDEX IF NOT EXISTS idx_enrollments_approved_by ON enrollments(approved_by);
