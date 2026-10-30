ALTER TABLE groups
    ALTER COLUMN invite_code_hash TYPE VARCHAR(64)
    USING invite_code_hash::VARCHAR(64);
