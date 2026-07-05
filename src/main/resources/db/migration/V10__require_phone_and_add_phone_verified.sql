UPDATE users SET phone_number = '000000' WHERE phone_number IS NULL;

ALTER TABLE users ALTER COLUMN phone_number SET NOT NULL;

ALTER TABLE users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
