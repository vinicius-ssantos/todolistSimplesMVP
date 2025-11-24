-- Add email verification fields to app_user table
ALTER TABLE app_user
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE app_user
ADD COLUMN email_verification_token VARCHAR(255);

ALTER TABLE app_user
ADD COLUMN verification_token_expires_at TIMESTAMP;

-- Create index for fast token lookups (H2 compatible - no partial index)
CREATE INDEX idx_app_user_verification_token ON app_user(email_verification_token);
