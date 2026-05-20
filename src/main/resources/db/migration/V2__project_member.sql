CREATE TABLE project_member (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uk_project_member UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_member_project ON project_member(project_id);
