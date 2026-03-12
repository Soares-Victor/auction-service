CREATE TABLE bids (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    auction_id UUID           NOT NULL REFERENCES auctions (id),
    user_id    VARCHAR(255)   NOT NULL,
    amount     NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bids_auction_amount  ON bids (auction_id, amount DESC);
CREATE INDEX idx_bids_auction_created ON bids (auction_id, created_at DESC);
