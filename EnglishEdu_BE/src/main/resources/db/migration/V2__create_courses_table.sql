CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    external_id INTEGER UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    level VARCHAR(50),
    description TEXT,
    image_url VARCHAR(512),
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_courses_category ON courses(category);
CREATE INDEX idx_courses_published ON courses(is_published);
