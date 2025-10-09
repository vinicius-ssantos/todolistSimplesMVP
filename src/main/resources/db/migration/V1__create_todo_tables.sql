-- V1__create_todo_tables.sql

CREATE TABLE IF NOT EXISTS todo_list (
                                         id         UUID PRIMARY KEY,
                                         title      VARCHAR(140) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS task (
                                    id         UUID PRIMARY KEY,
                                    list_id    UUID NOT NULL,
                                    title      VARCHAR(140) NOT NULL,
    notes      TEXT,
    priority   VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',   -- LOW | MEDIUM | HIGH
    status     VARCHAR(16) NOT NULL DEFAULT 'OPEN',      -- OPEN | IN_PROGRESS | DONE | BLOCKED | ARCHIVED
    due_date   DATE,
    position   INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK (termine com vírgula!)
    CONSTRAINT fk_task_list
    FOREIGN KEY (list_id) REFERENCES todo_list(id) ON DELETE CASCADE,

    -- CHECKs (cada um separado por vírgula)
    CONSTRAINT task_priority_chk CHECK (priority IN ('LOW','MEDIUM','HIGH')),
    CONSTRAINT task_status_chk   CHECK (status   IN ('OPEN','IN_PROGRESS','DONE','BLOCKED','ARCHIVED'))
    );
