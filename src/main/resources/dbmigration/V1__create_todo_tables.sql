CREATE TABLE todo_list (
    id          UUID NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_todo_list PRIMARY KEY (id)
);

CREATE TABLE task (
    id         UUID NOT NULL,
    list_id    UUID NOT NULL,
    title      VARCHAR(140) NOT NULL,
    notes      VARCHAR(1000),
    priority   VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',   -- LOW | MEDIUM | HIGH
    status     VARCHAR(16) NOT NULL DEFAULT 'OPEN',      -- OPEN | IN_PROGRESS | DONE | BLOCKED | ARCHIVED
    due_date   DATE,
    position   INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_task PRIMARY KEY (id),

    CONSTRAINT fk_task_list
        FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,

    CONSTRAINT task_priority_chk CHECK (priority IN ('LOW','MEDIUM','HIGH')),
    CONSTRAINT task_status_chk   CHECK (status   IN ('OPEN','IN_PROGRESS','DONE','BLOCKED','ARCHIVED'))
);
