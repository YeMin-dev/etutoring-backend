-- Initial schema placeholder.
-- Future migrations should create domain tables with:
-- 1) soft delete: deleted_date TIMESTAMPTZ NULL
-- 2) audit timestamps: created_date / updated_date as TIMESTAMPTZ

-- Example placeholder object so Flyway has a valid first migration:
CREATE TABLE IF NOT EXISTS schema_version_placeholder (
    id BIGSERIAL PRIMARY KEY,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
