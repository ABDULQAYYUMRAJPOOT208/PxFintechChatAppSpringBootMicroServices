CREATE TABLE IF NOT EXISTS oauth2_clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret VARCHAR(255) NOT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_type VARCHAR(20) NOT NULL, -- CONFIDENTIAL, PUBLIC
    grant_types TEXT[] NOT NULL, -- authorization_code, refresh_token, client_credentials
    redirect_uris TEXT[],
    scopes TEXT[],
    authentication_methods TEXT[],
    access_token_validity INT DEFAULT 3600,
    refresh_token_validity INT DEFAULT 86400,
    require_proof_key BOOLEAN DEFAULT FALSE,
    require_authorization_consent BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oauth2_clients_client_id ON oauth2_clients(client_id);

CREATE TABLE IF NOT EXISTS oauth2_authorizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(255) NOT NULL,
    authorization_code VARCHAR(255),
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    state VARCHAR(255),
    scopes TEXT[],
    authorization_code_issued_at TIMESTAMP WITH TIME ZONE,
    authorization_code_expires_at TIMESTAMP WITH TIME ZONE,
    access_token_issued_at TIMESTAMP WITH TIME ZONE,
    access_token_expires_at TIMESTAMP WITH TIME ZONE,
    refresh_token_issued_at TIMESTAMP WITH TIME ZONE,
    refresh_token_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oauth2_authorizations_client_id ON oauth2_authorizations(client_id);
CREATE INDEX idx_oauth2_authorizations_principal ON oauth2_authorizations(principal_name);
CREATE INDEX idx_oauth2_authorizations_access_token ON oauth2_authorizations(access_token);
CREATE INDEX idx_oauth2_authorizations_refresh_token ON oauth2_authorizations(refresh_token);