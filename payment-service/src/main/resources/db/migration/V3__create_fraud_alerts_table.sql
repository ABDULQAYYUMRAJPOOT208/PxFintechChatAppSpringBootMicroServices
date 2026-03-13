CREATE TABLE IF NOT EXISTS fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id VARCHAR(100) UNIQUE NOT NULL,
    transaction_id UUID REFERENCES transactions(id),
    user_id UUID NOT NULL,
    alert_type VARCHAR(50) NOT NULL, -- VELOCITY, PATTERN, AMOUNT, LOCATION
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    score DECIMAL(5,2) NOT NULL,
    description TEXT,
    metadata JSONB,
    action_taken VARCHAR(50), -- BLOCK, REVIEW, ALLOW
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_alerts_user ON fraud_alerts(user_id);
CREATE INDEX idx_fraud_alerts_transaction ON fraud_alerts(transaction_id);
CREATE INDEX idx_fraud_alerts_severity ON fraud_alerts(severity);