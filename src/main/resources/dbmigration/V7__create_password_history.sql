-- V7: Create password history table for password reuse prevention
-- Author: Claude
-- Date: 2025-11-18

CREATE TABLE password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Foreign key to app_user table
    CONSTRAINT fk_password_history_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE
);

-- Index for efficient lookups by user
CREATE INDEX idx_password_history_user_id ON password_history(user_id);

-- Index for efficient ordering by creation date
CREATE INDEX idx_password_history_created_at ON password_history(created_at);

-- Comments for documentation
COMMENT ON TABLE password_history IS 'Stores password history to prevent users from reusing recent passwords';
COMMENT ON COLUMN password_history.user_id IS 'Reference to the user who owns this password history entry';
COMMENT ON COLUMN password_history.password_hash IS 'BCrypt hash of the previous password';
COMMENT ON COLUMN password_history.created_at IS 'When this password was set';
