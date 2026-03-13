CREATE TABLE IF NOT EXISTS posts (
    id UUID PRIMARY KEY,
    created_by_id UUID NOT NULL,
    body TEXT NOT NULL,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    CONSTRAINT fk_posts_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS post_attachments (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    CONSTRAINT fk_post_attachments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_targets (
    post_id UUID NOT NULL,
    student_user_id UUID NOT NULL,
    PRIMARY KEY (post_id, student_user_id),
    CONSTRAINT fk_post_targets_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_targets_student FOREIGN KEY (student_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    comment TEXT NOT NULL,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS ix_posts_created_by_id ON posts (created_by_id);
CREATE INDEX IF NOT EXISTS ix_posts_created_date ON posts (created_date DESC);
CREATE INDEX IF NOT EXISTS ix_post_attachments_post_id ON post_attachments (post_id);
CREATE INDEX IF NOT EXISTS ix_post_targets_student_user_id ON post_targets (student_user_id);
CREATE INDEX IF NOT EXISTS ix_comments_post_id ON comments (post_id);
CREATE INDEX IF NOT EXISTS ix_comments_author_user_id ON comments (author_user_id);
