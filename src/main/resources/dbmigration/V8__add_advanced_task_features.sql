-- V8: Add advanced task features (recurring tasks, tags, attachments, sharing)
-- Author: Claude
-- Date: 2025-11-18
-- H2 compatible version

-- =============================================================================
-- 1. Add recurring task fields to task table
-- =============================================================================

ALTER TABLE task ADD COLUMN is_recurring BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE task ADD COLUMN recurrence_pattern TEXT;
ALTER TABLE task ADD COLUMN parent_recurring_task_id UUID;

-- Index for finding recurring tasks (H2 compatible - no partial index)
CREATE INDEX idx_task_is_recurring ON task(is_recurring);

-- Index for finding child tasks of a recurring parent
CREATE INDEX idx_task_parent_recurring ON task(parent_recurring_task_id);

-- =============================================================================
-- 2. Create tags table
-- =============================================================================

CREATE TABLE tag (
    id UUID DEFAULT random_uuid() PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7),
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign key to app_user table
    CONSTRAINT fk_tag_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    -- Unique constraint: user cannot have duplicate tag names
    CONSTRAINT uq_tag_user_name UNIQUE (user_id, name)
);

-- Indexes
CREATE INDEX idx_tag_user_id ON tag(user_id);

-- =============================================================================
-- 3. Create task_tag junction table (many-to-many)
-- =============================================================================

CREATE TABLE task_tag (
    task_id UUID NOT NULL,
    tag_id UUID NOT NULL,

    PRIMARY KEY (task_id, tag_id),

    CONSTRAINT fk_task_tag_task
        FOREIGN KEY (task_id)
        REFERENCES task(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_tag_tag
        FOREIGN KEY (tag_id)
        REFERENCES tag(id)
        ON DELETE CASCADE
);

-- Indexes for efficient lookups
CREATE INDEX idx_task_tag_task_id ON task_tag(task_id);
CREATE INDEX idx_task_tag_tag_id ON task_tag(tag_id);

-- =============================================================================
-- 4. Create task_attachment table
-- =============================================================================

CREATE TABLE task_attachment (
    id UUID DEFAULT random_uuid() PRIMARY KEY,
    task_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_url VARCHAR(500) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign key to task table
    CONSTRAINT fk_attachment_task
        FOREIGN KEY (task_id)
        REFERENCES task(id)
        ON DELETE CASCADE,

    -- Foreign key to app_user table
    CONSTRAINT fk_attachment_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_attachment_task_id ON task_attachment(task_id);
CREATE INDEX idx_attachment_user_id ON task_attachment(user_id);

-- =============================================================================
-- 5. Create shared_list table (for collaboration)
-- =============================================================================

CREATE TABLE shared_list (
    id UUID DEFAULT random_uuid() PRIMARY KEY,
    list_id UUID NOT NULL,
    shared_by_user_id UUID NOT NULL,
    shared_with_user_id UUID NOT NULL,
    permission VARCHAR(20) NOT NULL CHECK (permission IN ('READ', 'WRITE', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign key to todo_list table
    CONSTRAINT fk_shared_list_list
        FOREIGN KEY (list_id)
        REFERENCES todo_list(id)
        ON DELETE CASCADE,

    -- Foreign key to app_user table (shared by)
    CONSTRAINT fk_shared_list_shared_by
        FOREIGN KEY (shared_by_user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    -- Foreign key to app_user table (shared with)
    CONSTRAINT fk_shared_list_shared_with
        FOREIGN KEY (shared_with_user_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    -- Unique constraint: can't share the same list with the same user twice
    CONSTRAINT uq_shared_list_list_user UNIQUE (list_id, shared_with_user_id),

    -- Check constraint: can't share with yourself
    CONSTRAINT chk_shared_list_not_self CHECK (shared_by_user_id != shared_with_user_id)
);

-- Indexes
CREATE INDEX idx_shared_list_list_id ON shared_list(list_id);
CREATE INDEX idx_shared_list_shared_with_user_id ON shared_list(shared_with_user_id);

-- =============================================================================
-- 6. Full-text search (skipped - PostgreSQL specific, not supported in H2)
-- For H2, use LIKE queries or implement application-level search
-- =============================================================================
