-- Wallet transactions table (sharded by wallet_id)
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    transaction_id VARCHAR(100) UNIQUE NOT NULL,
    idempotency_key VARCHAR(100) UNIQUE,
    transaction_type VARCHAR(50) NOT NULL, -- CREDIT, DEBIT, HOLD, RELEASE
    amount DECIMAL(19,4) NOT NULL,
    balance_before DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETED, FAILED, CANCELLED
    description TEXT,
    reference_id VARCHAR(100), -- Reference to external transaction
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT valid_amount CHECK (amount > 0)
);

CREATE INDEX idx_transactions_wallet ON wallet_transactions(wallet_id);
CREATE INDEX idx_transactions_idempotency ON wallet_transactions(idempotency_key);
CREATE INDEX idx_transactions_created ON wallet_transactions(created_at);
CREATE INDEX idx_transactions_reference ON wallet_transactions(reference_id);

-- Holds table for pending transactions
CREATE TABLE IF NOT EXISTS holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    hold_id VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, RELEASED, EXPIRED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT valid_hold_amount CHECK (amount > 0)
);

CREATE INDEX idx_holds_wallet ON holds(wallet_id);
CREATE INDEX idx_holds_expires ON holds(expires_at) WHERE status = 'ACTIVE';