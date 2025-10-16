-- Align task ownership with its list before enforcing composite constraints
UPDATE task t
SET user_id = (
    SELECT l.user_id FROM todo_list l WHERE l.id = t.list_id
)
WHERE t.user_id <> (
    SELECT l.user_id FROM todo_list l WHERE l.id = t.list_id
);

-- Ensure todo_list has a unique key that includes the owner
ALTER TABLE todo_list ADD CONSTRAINT uq_todolist_id_user UNIQUE (id, user_id);

-- Refresh foreign keys with consistent naming
ALTER TABLE todo_list DROP CONSTRAINT IF EXISTS fk_list_user;
ALTER TABLE todo_list ADD CONSTRAINT fk_todolist_user
    FOREIGN KEY (user_id) REFERENCES app_user(id);

ALTER TABLE task DROP CONSTRAINT IF EXISTS fk_task_list;
ALTER TABLE task DROP CONSTRAINT IF EXISTS fk_task_user;

ALTER TABLE task ADD CONSTRAINT fk_task_user
    FOREIGN KEY (user_id) REFERENCES app_user(id);

ALTER TABLE task ADD CONSTRAINT fk_task_list_user
    FOREIGN KEY (list_id, user_id) REFERENCES todo_list(id, user_id)
    ON DELETE CASCADE;

-- Multi-tenant friendly indexes
CREATE INDEX IF NOT EXISTS ix_todolist_user_id_id ON todo_list(user_id, id);
CREATE INDEX IF NOT EXISTS ix_task_user_id_list_id ON task(user_id, list_id);
CREATE INDEX IF NOT EXISTS ix_task_user_id_status ON task(user_id, status);
