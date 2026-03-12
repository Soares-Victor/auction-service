CREATE TABLE comments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    auction_id UUID        NOT NULL REFERENCES auctions (id),
    user_id    VARCHAR(255) NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_auction_created ON comments (auction_id, created_at ASC);
