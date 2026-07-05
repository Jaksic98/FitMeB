-- De-duplicate placeholder phone numbers before adding unique constraint
-- Since V10 set all NULL phone_numbers to '000000', we need to make each unique
UPDATE users SET phone_number = '000000' || CAST(id AS VARCHAR) WHERE phone_number = '000000';

-- Add unique constraint for phone_number (excluding DELETED users)
CREATE UNIQUE INDEX uk_users_phone_number_not_deleted
ON users (phone_number)
WHERE status <> 2;

-- Add OTP columns for WhatsApp verification
ALTER TABLE users ADD COLUMN otp_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN otp_expires_at TIMESTAMP;
