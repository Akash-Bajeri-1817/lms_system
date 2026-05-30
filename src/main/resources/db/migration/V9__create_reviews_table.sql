CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT NOT NULL REFERENCES users(id),
    course_id   BIGINT NOT NULL REFERENCES courses(id),
    rating      INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(student_id, course_id)
);

CREATE INDEX idx_reviews_course  ON reviews(course_id);
CREATE INDEX idx_reviews_student ON reviews(student_id);