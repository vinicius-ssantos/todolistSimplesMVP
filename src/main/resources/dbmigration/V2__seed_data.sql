INSERT INTO todo_list (id, name, created_at, updated_at)
VALUES
    ('0b2f1a75-9c1d-4c6b-9e49-8a6b4d6b2e91', 'Projetos',
        CURRENT_TIMESTAMP - INTERVAL '14' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY),
    ('8d3c2f14-7b28-4b8f-ae0f-1f2c3d4e5f60', 'Mercado',
        CURRENT_TIMESTAMP - INTERVAL '10' DAY, CURRENT_TIMESTAMP - INTERVAL '6' HOUR);

INSERT INTO task (id, list_id, title, notes, priority, status, due_date, position, created_at, updated_at)
VALUES
    ('4f3a6d28-1b5c-4bc1-8f3d-92a7b1c6e8a0', '0b2f1a75-9c1d-4c6b-9e49-8a6b4d6b2e91',
        'Criar estrutura do projeto',
        'Configurar Gradle, modulos base, endpoint /ping e profiles locais',
        'HIGH', 'IN_PROGRESS',
        CURRENT_DATE - INTERVAL '3' DAY, 1,
        CURRENT_TIMESTAMP - INTERVAL '14' DAY, CURRENT_TIMESTAMP - INTERVAL '3' HOUR),

    ('8a9c1d2e-3f4b-5a6c-7d8e-9f0a1b2c3d4e', '0b2f1a75-9c1d-4c6b-9e49-8a6b4d6b2e91',
        'Modelar entidades',
        'TodoList e Task com JPA/Hibernate; revisar relacionamentos e indices',
        'MEDIUM', 'OPEN',
        CURRENT_DATE + INTERVAL '2' DAY, 2,
        CURRENT_TIMESTAMP - INTERVAL '13' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY),

    ('c1b2a3d4-e5f6-4a1b-9c8d-7e6f5a4b3c2d', '0b2f1a75-9c1d-4c6b-9e49-8a6b4d6b2e91',
        'CRUD basico',
        'Controller -> Service -> Repository; retornar 201 com Location',
        'LOW', 'DONE',
        CURRENT_DATE - INTERVAL '7' DAY, 3,
        CURRENT_TIMESTAMP - INTERVAL '12' DAY, CURRENT_TIMESTAMP - INTERVAL '5' DAY);

INSERT INTO task (id, list_id, title, notes, priority, status, due_date, position, created_at, updated_at)
VALUES
    ('d2e3f4a5-b6c7-48d9-a0b1-c2d3e4f5a6b7', '8d3c2f14-7b28-4b8f-ae0f-1f2c3d4e5f60',
        'Comprar frango',
        '1,2 kg de peito para desfiar; verificar validade',
        'MEDIUM', 'OPEN',
        CURRENT_DATE + INTERVAL '1' DAY, 1,
        CURRENT_TIMESTAMP - INTERVAL '9' DAY, CURRENT_TIMESTAMP),

    ('e3f4a5b6-c7d8-49e0-b1c2-d3e4f5a6b7c8', '8d3c2f14-7b28-4b8f-ae0f-1f2c3d4e5f60',
        'Arroz e feijao',
        'Arroz 5 kg e feijao carioca 2 pacotes',
        'LOW', 'DONE',
        NULL, 2,
        CURRENT_TIMESTAMP - INTERVAL '9' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY);
