CREATE TABLE page_views (
    id UUID PRIMARY KEY,
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    page_path VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    browser VARCHAR(64) NOT NULL
);

CREATE INDEX ix_page_views_viewed_at ON page_views (viewed_at);
CREATE INDEX ix_page_views_page_path ON page_views (page_path);
CREATE INDEX ix_page_views_user_id ON page_views (user_id);
CREATE INDEX ix_page_views_browser ON page_views (browser);
