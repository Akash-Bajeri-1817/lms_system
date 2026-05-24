CREATE TABLE instructor_applications (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id),
    expertise        VARCHAR(255) NOT NULL,
    experience       TEXT NOT NULL,
    reason           TEXT NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    applied_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at      TIMESTAMP,
    reviewed_by      BIGINT REFERENCES users(id),

    -- one active application per user at a time
    UNIQUE(user_id)
);

CREATE INDEX idx_applications_status  ON instructor_applications(status);
CREATE INDEX idx_applications_user    ON instructor_applications(user_id);