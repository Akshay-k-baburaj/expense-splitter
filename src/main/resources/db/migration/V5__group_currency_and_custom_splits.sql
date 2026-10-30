ALTER TABLE groups
    ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'INR';

ALTER TABLE expenses
    ADD COLUMN split_type VARCHAR(10) NOT NULL DEFAULT 'EQUAL',
    ALTER COLUMN amount TYPE NUMERIC(13, 3) USING amount::NUMERIC(13, 3),
    ADD CONSTRAINT chk_expenses_split_type CHECK (split_type IN ('EQUAL', 'CUSTOM'));

ALTER TABLE expense_splits
    ALTER COLUMN amount TYPE NUMERIC(13, 3) USING amount::NUMERIC(13, 3);

ALTER TABLE settlements
    ALTER COLUMN amount TYPE NUMERIC(13, 3) USING amount::NUMERIC(13, 3);
