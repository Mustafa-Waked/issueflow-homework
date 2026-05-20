CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT
);

CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    owner_id BIGINT NOT NULL REFERENCES app_user(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT
);

CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    type VARCHAR(30) NOT NULL,
    project_id BIGINT NOT NULL REFERENCES project(id),
    assignee_id BIGINT REFERENCES app_user(id),
    due_date TIMESTAMP,
    overdue BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT
);

CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES ticket(id),
    author_id BIGINT NOT NULL REFERENCES app_user(id),
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT
);

CREATE TABLE mention (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comment(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    CONSTRAINT uk_mention_comment_user UNIQUE (comment_id, user_id)
);

CREATE TABLE ticket_dependency (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    blocked_by_id BIGINT NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_blocker UNIQUE (ticket_id, blocked_by_id)
);

CREATE TABLE attachment (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id VARCHAR(120) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE revoked_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_project_deleted ON project(deleted);
CREATE INDEX idx_ticket_project_deleted ON ticket(project_id, deleted);
CREATE INDEX idx_ticket_assignee_status ON ticket(assignee_id, status);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor);
CREATE INDEX idx_audit_action ON audit_log(action);
