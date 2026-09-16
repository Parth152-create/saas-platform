ALTER TABLE users
    ADD COLUMN invite_token VARCHAR(255),
    ADD COLUMN invite_token_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_users_invite_token ON users (invite_token);