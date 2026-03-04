CREATE TABLE IF NOT EXISTS tutor_allocation (
    id UUID PRIMARY KEY,
    student_user_id UUID NOT NULL REFERENCES users (id),
    tutor_user_id UUID NOT NULL REFERENCES users (id),
    allocated_by_id UUID NOT NULL REFERENCES users (id),
    allocated_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_date TIMESTAMPTZ,
    reason TEXT,
    schedule_start TIMESTAMPTZ,
    schedule_end TIMESTAMPTZ,
    CONSTRAINT chk_schedule_range CHECK (
    (schedule_start IS NULL AND schedule_end IS NULL)
    OR (schedule_start IS NOT NULL AND schedule_end IS NOT NULL AND schedule_end >= schedule_start)
)
);

CREATE INDEX IF NOT EXISTS ix_tutor_allocation_student_user_id ON tutor_allocation (student_user_id);
CREATE INDEX IF NOT EXISTS ix_tutor_allocation_tutor_user_id ON tutor_allocation (tutor_user_id);
CREATE INDEX IF NOT EXISTS ix_tutor_allocation_allocated_by_id ON tutor_allocation (allocated_by_id);
CREATE INDEX IF NOT EXISTS ix_tutor_allocation_tutor_ended ON tutor_allocation (tutor_user_id, ended_date);
