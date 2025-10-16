ALTER TABLE todo_list ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE task ADD COLUMN IF NOT EXISTS user_id UUID;


ALTER TABLE todo_list ADD CONSTRAINT IF NOT EXISTS fk_list_user
    FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE task ADD CONSTRAINT IF NOT EXISTS fk_task_user
    FOREIGN KEY (user_id) REFERENCES app_user(id);


-- Nota: mantemos NULLABLE por enquanto; depois de propagar userId no código,
-- tornamos NOT NULL em uma V5.