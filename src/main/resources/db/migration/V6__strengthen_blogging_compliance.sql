ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_interaction_date TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_users_last_interaction_date ON users (last_interaction_date);

ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS attachment_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_comments_attachment'
    ) THEN
        ALTER TABLE comments
            ADD CONSTRAINT fk_comments_attachment
            FOREIGN KEY (attachment_id)
            REFERENCES post_attachments (id)
            ON DELETE CASCADE;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS ix_comments_attachment_id ON comments (attachment_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_posts_body_not_blank'
    ) THEN
        ALTER TABLE posts
            ADD CONSTRAINT ck_posts_body_not_blank
            CHECK (length(trim(body)) > 0);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_comments_comment_not_blank'
    ) THEN
        ALTER TABLE comments
            ADD CONSTRAINT ck_comments_comment_not_blank
            CHECK (length(trim(comment)) > 0);
    END IF;
END
$$;
