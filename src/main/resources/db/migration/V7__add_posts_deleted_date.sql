ALTER TABLE posts ADD COLUMN IF NOT EXISTS deleted_date TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS ix_posts_deleted_date ON posts (deleted_date);
