DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('ADMIN', 'TUTOR', 'STUDENT');
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    role user_role NOT NULL,
    username VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ,
    last_login_date TIMESTAMPTZ,
    deleted_date TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users (email);
CREATE INDEX IF NOT EXISTS ix_users_deleted_date ON users (deleted_date);

CREATE TABLE IF NOT EXISTS allocations (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    tutor_id UUID NOT NULL REFERENCES users(id),
    allocated_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_allocations_student_id ON allocations (student_id);
CREATE INDEX IF NOT EXISTS ix_allocations_tutor_id ON allocations (tutor_id);
CREATE INDEX IF NOT EXISTS ix_allocations_deleted_date ON allocations (deleted_date);
