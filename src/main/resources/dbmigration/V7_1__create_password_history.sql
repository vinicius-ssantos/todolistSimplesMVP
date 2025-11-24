-- V7.1: Create password history table for password reuse prevention
-- Author: Claude
-- Date: 2025-11-18
-- H2 compatible version

CREATE TABLE password_history (
    id UUID DEFAULT random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

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
