DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'meeting_mode') THEN
        CREATE TYPE meeting_mode AS ENUM ('IN_PERSON', 'VIRTUAL');
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS meetings (
    id UUID PRIMARY KEY,
    student_user_id UUID NOT NULL REFERENCES users (id),
    tutor_user_id UUID NOT NULL REFERENCES users (id),
    created_by_id UUID NOT NULL REFERENCES users (id),
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    mode meeting_mode NOT NULL,
    location TEXT,
    link TEXT,
    description TEXT,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    CONSTRAINT chk_meeting_range CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS ix_meetings_student_user_id ON meetings (student_user_id);
CREATE INDEX IF NOT EXISTS ix_meetings_tutor_user_id ON meetings (tutor_user_id);
CREATE INDEX IF NOT EXISTS ix_meetings_created_by_id ON meetings (created_by_id);
