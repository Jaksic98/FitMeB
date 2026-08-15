ALTER TABLE users ADD COLUMN password_reset_token_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN password_reset_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP;
CREATE INDEX idx_users_password_reset_token_hash ON users (password_reset_token_hash);
