CREATE TABLE enrollments (
    id           BIGSERIAL PRIMARY KEY,
    student_id   BIGINT NOT NULL REFERENCES users(id),
    course_id    BIGINT NOT NULL REFERENCES courses(id),
    status       VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,

    -- prevent a student from enrolling in the same course twice
    UNIQUE(student_id, course_id)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_course  ON enrollments(course_id);