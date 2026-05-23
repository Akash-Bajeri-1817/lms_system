CREATE TABLE courses (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    slug          VARCHAR(255) NOT NULL UNIQUE,
    status        VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    instructor_id BIGINT NOT NULL REFERENCES users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_status ON courses(status);