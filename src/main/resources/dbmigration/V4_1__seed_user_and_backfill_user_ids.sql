-- 1) cria um usuário seed (dev/test) se ainda não existir
INSERT INTO app_user (id, email, password_hash, created_at)
SELECT CAST('00000000-0000-0000-0000-000000000001' AS UUID),
       'seed@example.com',
       'noop-dev-seed',              -- só pra preencher; não será usado pra login
       CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'seed@example.com');

-- 2) preenche user_id das listas sem dono
UPDATE todo_list
SET user_id = (SELECT id FROM app_user WHERE email = 'seed@example.com')
WHERE user_id IS NULL;

-- 3) preenche user_id das tasks sem dono (herdando da lista)
UPDATE task t
SET user_id = (SELECT l.user_id FROM todo_list l WHERE l.id = t.list_id)
WHERE t.user_id IS NULL;
