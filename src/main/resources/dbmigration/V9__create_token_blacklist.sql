-- Create token blacklist table for revoked tokens
CREATE TABLE blacklisted_token (
    id UUID NOT NULL,
    token_jti VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    reason VARCHAR(100),
    CONSTRAINT pk_blacklisted_token PRIMARY KEY (id),
    CONSTRAINT fk_blacklisted_token_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create indexes for fast lookups
CREATE INDEX idx_blacklisted_token_jti ON blacklisted_token(token_jti);
CREATE INDEX idx_blacklisted_token_expires_at ON blacklisted_token(expires_at);
CREATE INDEX idx_blacklisted_token_user_id ON blacklisted_token(user_id);
