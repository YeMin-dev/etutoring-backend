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
