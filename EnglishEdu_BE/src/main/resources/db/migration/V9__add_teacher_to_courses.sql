-- V9: Add teacher assignment to courses + enforce role enum on users

-- Allow each course to be assigned to a teacher
ALTER TABLE courses
    ADD COLUMN teacher_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Enforce valid role values (existing data only has STUDENT/ADMIN, both valid)
ALTER TABLE users
    ADD CONSTRAINT chk_user_role CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN'));

CREATE INDEX idx_courses_teacher ON courses(teacher_id);
