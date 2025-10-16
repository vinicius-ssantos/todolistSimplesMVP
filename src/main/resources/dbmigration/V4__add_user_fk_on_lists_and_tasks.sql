ALTER TABLE todo_list ADD COLUMN user_id UUID;
ALTER TABLE task ADD COLUMN user_id UUID;


ALTER TABLE todo_list ADD CONSTRAINT fk_list_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;
ALTER TABLE task ADD CONSTRAINT fk_task_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;


-- Nota: mantemos NULLABLE por enquanto; depois de propagar userId no código,
-- tornamos NOT NULL em uma V5.
