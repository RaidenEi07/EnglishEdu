-- ============================================
-- V11: Categories & Levels as Master Data
-- ============================================

-- 1. Categories table
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 2. Levels table
CREATE TABLE levels (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 3. Seed categories from existing course data
INSERT INTO categories (name, slug, sort_order)
SELECT DISTINCT category, LOWER(REPLACE(category, ' ', '-')), ROW_NUMBER() OVER (ORDER BY category)
FROM courses
WHERE category IS NOT NULL AND category != ''
ON CONFLICT (name) DO NOTHING;

-- 4. Seed levels from existing course data
INSERT INTO levels (name, slug, sort_order)
SELECT DISTINCT level, LOWER(REPLACE(REPLACE(level, ' ', '-'), '/', '-')), ROW_NUMBER() OVER (ORDER BY level)
FROM courses
WHERE level IS NOT NULL AND level != ''
ON CONFLICT (name) DO NOTHING;

-- 5. Add FK columns to courses
ALTER TABLE courses ADD COLUMN category_id BIGINT REFERENCES categories(id);
ALTER TABLE courses ADD COLUMN level_id    BIGINT REFERENCES levels(id);

-- 6. Populate FK from existing VARCHAR data
UPDATE courses c SET category_id = cat.id
FROM categories cat WHERE c.category = cat.name;

UPDATE courses c SET level_id = lv.id
FROM levels lv WHERE c.level = lv.name;

-- 7. Indexes
CREATE INDEX idx_courses_category_id ON courses(category_id);
CREATE INDEX idx_courses_level_id ON courses(level_id);
