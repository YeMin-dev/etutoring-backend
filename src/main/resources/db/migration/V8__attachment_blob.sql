ALTER TABLE post_attachments
    ADD COLUMN IF NOT EXISTS file_data BYTEA,
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);
