-- ============================================
-- V15: Course Pricing & Payments
-- ============================================

-- 1. Add pricing fields to courses
ALTER TABLE courses ADD COLUMN is_free BOOLEAN DEFAULT TRUE;
ALTER TABLE courses ADD COLUMN price   DECIMAL(12, 2) DEFAULT 0;

-- 2. Payments table
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id       BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    enrollment_id   BIGINT REFERENCES enrollments(id) ON DELETE SET NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'VND',
    payment_method  VARCHAR(30),             -- VNPAY, MOMO, STRIPE, BANK_TRANSFER
    transaction_id  VARCHAR(255) UNIQUE,     -- ID from payment gateway
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, COMPLETED, FAILED, REFUNDED
    paid_at         TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_payments_user       ON payments(user_id);
CREATE INDEX idx_payments_course     ON payments(course_id);
CREATE INDEX idx_payments_status     ON payments(status);
CREATE INDEX idx_payments_txn        ON payments(transaction_id);

-- 3. Mark all existing courses as free
UPDATE courses SET is_free = TRUE WHERE is_free IS NULL;
