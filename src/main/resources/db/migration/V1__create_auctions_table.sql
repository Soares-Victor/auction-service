CREATE TABLE auctions (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id         VARCHAR(255)    NOT NULL,
    title             VARCHAR(255)    NOT NULL,
    description       TEXT,
    start_time        TIMESTAMPTZ     NOT NULL,
    end_time          TIMESTAMPTZ     NOT NULL,
    starting_price    NUMERIC(19, 4)  NOT NULL CHECK (starting_price > 0),
    min_bid_increment NUMERIC(19, 4)  NOT NULL DEFAULT 1.0000 CHECK (min_bid_increment > 0),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_auction_times CHECK (end_time > start_time)
);

CREATE INDEX idx_auctions_start_time ON auctions (start_time DESC);
CREATE INDEX idx_auctions_end_time   ON auctions (end_time);
