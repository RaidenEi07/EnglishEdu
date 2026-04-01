-- ============================================
-- V17: Notification type for WebSocket routing
-- ============================================

ALTER TABLE notifications ADD COLUMN type VARCHAR(50) DEFAULT 'GENERAL';
-- Types: GENERAL, ENROLLMENT_APPROVED, ENROLLMENT_REVOKED, COURSE_UPDATE, PAYMENT, REVIEW
