-- WhatsApp OTP verification removed; account activation goes through email again.
ALTER TABLE users DROP COLUMN otp_hash;
ALTER TABLE users DROP COLUMN otp_expires_at;
ALTER TABLE users DROP COLUMN phone_verified;
