CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT NOT NULL REFERENCES users(id),
    course_id           BIGINT NOT NULL REFERENCES courses(id),
    razorpay_order_id   VARCHAR(100) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(100),
    razorpay_signature  VARCHAR(500),
    amount              DECIMAL(10,2) NOT NULL,
    currency            VARCHAR(10) NOT NULL DEFAULT 'INR',
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_student   ON payments(student_id);
CREATE INDEX idx_payments_course    ON payments(course_id);
CREATE INDEX idx_payments_order     ON payments(razorpay_order_id);