-- V2: seed the first admin user
-- password is "admin123" hashed with BCrypt
-- generated at: https://bcrypt-generator.com (12 rounds)

INSERT INTO users (email, password, first_name, last_name, role, enabled)
VALUES (
    'admin@lms.com',
    '$2a$12$UfHmeTQZu2NTQQfLYMfR4.VOnKEFXnHrYqHNLDrOF1gwAhymv2SHO',
    'Super',
    'Admin',
    'ADMIN',
    true
);