CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    student_user_id UUID NOT NULL REFERENCES users (id),
    tutor_user_id UUID NOT NULL REFERENCES users (id),
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_conversations_student_tutor UNIQUE (student_user_id, tutor_user_id)
);

CREATE INDEX IF NOT EXISTS ix_conversations_student_user_id ON conversations (student_user_id);
CREATE INDEX IF NOT EXISTS ix_conversations_tutor_user_id ON conversations (tutor_user_id);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    sender_user_id UUID NOT NULL REFERENCES users (id),
    body TEXT NOT NULL,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_messages_conversation_id ON messages (conversation_id);
CREATE INDEX IF NOT EXISTS ix_messages_sender_user_id ON messages (sender_user_id);
CREATE INDEX IF NOT EXISTS ix_messages_conversation_created ON messages (conversation_id, created_date);
