-- OTP verification reinstated: code is now delivered via email instead of WhatsApp.
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN otp_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN otp_expires_at TIMESTAMP;
