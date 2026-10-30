ALTER TABLE groups
    ADD COLUMN invite_code_hash VARCHAR(64),
    ADD COLUMN invite_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_groups_invite_code_hash
    ON groups (invite_code_hash)
    WHERE invite_code_hash IS NOT NULL;

CREATE TABLE group_join_attempts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_join_attempts_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_group_join_attempts_user_time
    ON group_join_attempts (user_id, attempted_at);
