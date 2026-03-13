-- Wallets table (sharded by user_id)
CREATE TABLE IF NOT EXISTS wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, CLOSED
    daily_limit DECIMAL(19,4) NOT NULL DEFAULT 1000,
    monthly_limit DECIMAL(19,4) NOT NULL DEFAULT 10000,
    daily_used DECIMAL(19,4) NOT NULL DEFAULT 0,
    monthly_used DECIMAL(19,4) NOT NULL DEFAULT 0,
    last_daily_reset DATE,
    last_monthly_reset DATE,
    frozen_reason TEXT,
    frozen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0, -- For optimistic locking

    CONSTRAINT valid_balance CHECK (balance >= 0),
    CONSTRAINT valid_daily_used CHECK (daily_used >= 0),
    CONSTRAINT valid_monthly_used CHECK (monthly_used >= 0)
);

-- Indexes for performance
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_status ON wallets(status);
CREATE INDEX idx_wallets_updated ON wallets(updated_at);