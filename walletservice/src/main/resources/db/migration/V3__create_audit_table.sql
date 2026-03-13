-- Audit log table (for compliance)
CREATE TABLE IF NOT EXISTS wallet_audit (
    id BIGSERIAL PRIMARY KEY,
    wallet_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_wallet ON wallet_audit(wallet_id);
CREATE INDEX idx_audit_created ON wallet_audit(created_at);

-- Daily summary table for reporting
CREATE TABLE IF NOT EXISTS wallet_daily_summary (
    id BIGSERIAL PRIMARY KEY,
    wallet_id UUID NOT NULL,
    summary_date DATE NOT NULL,
    opening_balance DECIMAL(19,4) NOT NULL,
    closing_balance DECIMAL(19,4) NOT NULL,
    total_credits DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_debits DECIMAL(19,4) NOT NULL DEFAULT 0,
    transaction_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(wallet_id, summary_date)
);