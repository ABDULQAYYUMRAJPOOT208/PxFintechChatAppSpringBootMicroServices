CREATE TABLE IF NOT EXISTS service_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(100) NOT NULL UNIQUE,
    service_id VARCHAR(100) NOT NULL UNIQUE,
    service_secret VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    allowed_scopes TEXT[],
    allowed_targets TEXT[],
    ip_whitelist TEXT[],
    rate_limit INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_registry_service_name ON service_registry(service_name);
CREATE INDEX idx_service_registry_service_id ON service_registry(service_id);
CREATE INDEX idx_service_registry_is_active ON service_registry(is_active);