CREATE TABLE IF NOT EXISTS assignments (
    id UUID PRIMARY KEY,
    created_by_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,
    due_date TIMESTAMPTZ,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    deleted_date TIMESTAMPTZ,
    CONSTRAINT fk_assignments_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS assignment_attachments (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_data BYTEA,
    content_type VARCHAR(100),
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    CONSTRAINT fk_assignment_attachments_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    feedback_text TEXT,
    feedback_at TIMESTAMPTZ,
    feedback_by_id UUID,
    CONSTRAINT fk_assignment_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_submissions_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_assignment_submissions_feedback_by FOREIGN KEY (feedback_by_id) REFERENCES users (id),
    CONSTRAINT uq_assignment_submissions_assignment_student UNIQUE (assignment_id, student_id)
);

CREATE TABLE IF NOT EXISTS submission_attachments (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_data BYTEA,
    content_type VARCHAR(100),
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    CONSTRAINT fk_submission_attachments_submission FOREIGN KEY (submission_id) REFERENCES assignment_submissions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_assignments_created_by_deleted ON assignments (created_by_id, deleted_date);
CREATE INDEX IF NOT EXISTS ix_assignment_attachments_assignment_id ON assignment_attachments (assignment_id);
CREATE INDEX IF NOT EXISTS ix_assignment_submissions_assignment_id ON assignment_submissions (assignment_id);
CREATE INDEX IF NOT EXISTS ix_assignment_submissions_student_id ON assignment_submissions (student_id);
CREATE INDEX IF NOT EXISTS ix_submission_attachments_submission_id ON submission_attachments (submission_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_assignments_title_not_blank'
    ) THEN
        ALTER TABLE assignments
            ADD CONSTRAINT ck_assignments_title_not_blank CHECK (length(trim(title)) > 0);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_assignments_instructions_not_blank'
    ) THEN
        ALTER TABLE assignments
            ADD CONSTRAINT ck_assignments_instructions_not_blank CHECK (length(trim(instructions)) > 0);
    END IF;
END
$$;
