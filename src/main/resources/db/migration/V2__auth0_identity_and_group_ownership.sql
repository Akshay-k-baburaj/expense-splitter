ALTER TABLE users
    ADD COLUMN auth0_subject VARCHAR(255);

CREATE UNIQUE INDEX uq_users_auth0_subject
    ON users (auth0_subject)
    WHERE auth0_subject IS NOT NULL;

ALTER TABLE groups
    ADD COLUMN owner_id BIGINT;

UPDATE groups g
SET owner_id = (
    SELECT gm.user_id
    FROM group_members gm
    WHERE gm.group_id = g.id
    ORDER BY gm.joined_at ASC, gm.user_id ASC
    LIMIT 1
);

ALTER TABLE groups
    ADD CONSTRAINT fk_groups_owner
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT;
