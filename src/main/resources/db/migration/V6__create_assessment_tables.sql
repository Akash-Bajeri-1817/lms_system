-- quizzes belong to a course
CREATE TABLE quizzes (
    id               BIGSERIAL PRIMARY KEY,
    course_id        BIGINT NOT NULL REFERENCES courses(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    passing_score    INTEGER NOT NULL DEFAULT 70,  -- percentage needed to pass
    time_limit       INTEGER,                       -- minutes, nullable = no limit
    total_marks      INTEGER NOT NULL DEFAULT 0,
    is_published     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- questions belong to a quiz
CREATE TABLE questions (
    id            BIGSERIAL PRIMARY KEY,
    quiz_id       BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    marks         INTEGER NOT NULL DEFAULT 1,
    position      INTEGER NOT NULL DEFAULT 0
);

-- options belong to a question (A, B, C, D)
CREATE TABLE options (
    id            BIGSERIAL PRIMARY KEY,
    question_id   BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_text   TEXT NOT NULL,
    is_correct    BOOLEAN NOT NULL DEFAULT FALSE
);

-- student quiz attempts
CREATE TABLE submissions (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT NOT NULL REFERENCES users(id),
    quiz_id          BIGINT NOT NULL REFERENCES quizzes(id),
    score            INTEGER NOT NULL DEFAULT 0,
    total_marks      INTEGER NOT NULL DEFAULT 0,
    total_questions  INTEGER NOT NULL DEFAULT 0,
    attempted        INTEGER NOT NULL DEFAULT 0,
    percentage       DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    passed           BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    -- one attempt per student per quiz
    UNIQUE(student_id, quiz_id)
);

-- each answer the student gave
CREATE TABLE submission_answers (
    id            BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    question_id   BIGINT NOT NULL REFERENCES questions(id),
    option_id     BIGINT REFERENCES options(id),   -- nullable = skipped question
    is_correct    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_quizzes_course     ON quizzes(course_id);
CREATE INDEX idx_questions_quiz     ON questions(quiz_id);
CREATE INDEX idx_options_question   ON options(question_id);
CREATE INDEX idx_submissions_student ON submissions(student_id);
CREATE INDEX idx_submissions_quiz   ON submissions(quiz_id);