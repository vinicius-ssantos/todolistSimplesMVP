-- Ajuste dados legados (se existir)
-- update todo_list set user_id = <algum uuid> where user_id is null;
-- update task set user_id = <mesmo uuid> where user_id is null;

ALTER TABLE task      ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE todo_list ALTER COLUMN user_id SET NOT NULL;