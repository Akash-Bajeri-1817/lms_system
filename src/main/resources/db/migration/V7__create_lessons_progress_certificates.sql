-- modules belong to a course
CREATE TABLE modules (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    position    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- lessons belong to a module
CREATE TABLE lessons (
    id           BIGSERIAL PRIMARY KEY,
    module_id    BIGINT NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    content_url  VARCHAR(500),
    type         VARCHAR(50) NOT NULL DEFAULT 'VIDEO',
    duration     INTEGER NOT NULL DEFAULT 0,
    position     INTEGER NOT NULL DEFAULT 0,
    is_free      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- tracks which lessons a student completed
CREATE TABLE lesson_progress (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT NOT NULL REFERENCES users(id),
    lesson_id    BIGINT NOT NULL REFERENCES lessons(id),
    course_id    BIGINT NOT NULL REFERENCES courses(id),
    completed    BOOLEAN NOT NULL DEFAULT FALSE,
    watched_seconds INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMP,

    UNIQUE(student_id, lesson_id)
);

-- certificates generated on course completion
CREATE TABLE certificates (
    id                BIGSERIAL PRIMARY KEY,
    student_id        BIGINT NOT NULL REFERENCES users(id),
    course_id         BIGINT NOT NULL REFERENCES courses(id),
    certificate_number VARCHAR(100) NOT NULL UNIQUE,
    issued_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(student_id, course_id)
);

CREATE INDEX idx_modules_course      ON modules(course_id);
CREATE INDEX idx_lessons_module      ON lessons(module_id);
CREATE INDEX idx_progress_student    ON lesson_progress(student_id);
CREATE INDEX idx_progress_course     ON lesson_progress(course_id);
CREATE INDEX idx_certificates_student ON certificates(student_id);