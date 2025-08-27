DROP TABLE IF EXISTS watchlist CASCADE;

CREATE TABLE IF NOT EXISTS watchlist (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    symbol_id BIGINT NOT NULL REFERENCES symbols(id) ON DELETE CASCADE,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(account_id, symbol_id)
);

-- Create index for faster queries
CREATE INDEX idx_watchlist_account_order ON watchlist(account_id, order_index);
CREATE INDEX idx_watchlist_account ON watchlist(account_id);