CREATE TABLE inactivity_reminder_log (
    id UUID PRIMARY KEY,
    student_user_id UUID NOT NULL REFERENCES users (id),
    tutor_user_id UUID NOT NULL REFERENCES users (id),
    activity_baseline_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    student_sent_at TIMESTAMPTZ,
    tutor_sent_at TIMESTAMPTZ,
    student_error TEXT,
    tutor_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_inactivity_reminder_unique
    ON inactivity_reminder_log (student_user_id, tutor_user_id, activity_baseline_at);

CREATE INDEX ix_inactivity_reminder_student ON inactivity_reminder_log (student_user_id);
CREATE INDEX ix_inactivity_reminder_status ON inactivity_reminder_log (status);
