ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(30),
    ADD COLUMN remaining_appointments INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN calendar_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN membership_expires_at DATE;

ALTER TABLE users
    ADD CONSTRAINT chk_users_remaining_appointments_non_negative CHECK (remaining_appointments >= 0);
