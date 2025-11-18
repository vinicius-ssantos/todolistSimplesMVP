-- V8: Add advanced task features (recurring tasks, tags, attachments, sharing)
-- Author: Claude
-- Date: 2025-11-18

-- =============================================================================
-- 1. Add recurring task fields to task table
-- =============================================================================

ALTER TABLE task
ADD COLUMN is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN recurrence_pattern TEXT,
ADD COLUMN parent_recurring_task_id UUID;

-- Index for finding recurring tasks
CREATE INDEX idx_task_is_recurring ON task(is_recurring) WHERE is_recurring = TRUE;

-- Index for finding child tasks of a recurring parent
CREATE INDEX idx_task_parent_recurring ON task(parent_recurring_task_id) WHERE parent_recurring_task_id IS NOT NULL;

-- Comments
COMMENT ON COLUMN task.is_recurring IS 'Whether this task recurs on a schedule';
COMMENT ON COLUMN task.recurrence_pattern IS 'JSON representation of the recurrence pattern';
COMMENT ON COLUMN task.parent_recurring_task_id IS 'Reference to the parent recurring task if this is an instance';

-- =============================================================================
-- 2. Create tags table
-- =============================================================================

CREATE TABLE tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7),
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
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

-- Comments
COMMENT ON TABLE tag IS 'Tags/categories for organizing tasks';
COMMENT ON COLUMN tag.name IS 'Tag name (unique per user)';
COMMENT ON COLUMN tag.color IS 'Hex color code for the tag (e.g., #FF5733)';

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

-- Comments
COMMENT ON TABLE task_tag IS 'Many-to-many relationship between tasks and tags';

-- =============================================================================
-- 4. Create task_attachment table
-- =============================================================================

CREATE TABLE task_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_url VARCHAR(500) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
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

-- Comments
COMMENT ON TABLE task_attachment IS 'File attachments for tasks';
COMMENT ON COLUMN task_attachment.file_size IS 'File size in bytes';
COMMENT ON COLUMN task_attachment.storage_url IS 'URL or path to the stored file';

-- =============================================================================
-- 5. Create shared_list table (for collaboration)
-- =============================================================================

CREATE TABLE shared_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_id UUID NOT NULL,
    shared_by_user_id UUID NOT NULL,
    shared_with_user_id UUID NOT NULL,
    permission VARCHAR(20) NOT NULL CHECK (permission IN ('READ', 'WRITE', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
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

-- Comments
COMMENT ON TABLE shared_list IS 'Tracks which users have access to which todo lists';
COMMENT ON COLUMN shared_list.permission IS 'Permission level: READ, WRITE, or ADMIN';

-- =============================================================================
-- 6. Create full-text search index for tasks
-- =============================================================================

-- Create a generated tsvector column for full-text search
ALTER TABLE task
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(notes, '')), 'B')
) STORED;

-- Create GIN index for fast full-text search
CREATE INDEX idx_task_search_vector ON task USING GIN (search_vector);

-- Comments
COMMENT ON COLUMN task.search_vector IS 'Full-text search vector for title and notes';
