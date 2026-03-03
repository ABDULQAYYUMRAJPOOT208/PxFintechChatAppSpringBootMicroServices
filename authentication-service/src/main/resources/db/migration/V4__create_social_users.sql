CREATE TABLE IF NOT EXISTS social_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    name VARCHAR(100),
    profile_picture_url VARCHAR(500),
    locale VARCHAR(10),
    verified_email BOOLEAN DEFAULT FALSE,
    raw_attributes TEXT,
    access_token VARCHAR(1000),
    refresh_token VARCHAR(1000),
    token_expiry TIMESTAMP WITH TIME ZONE,
    is_linked BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_provider_provider_id UNIQUE(provider, provider_id)
);

CREATE INDEX idx_social_users_user_id ON social_users(user_id);
CREATE INDEX idx_social_users_provider ON social_users(provider);
CREATE INDEX idx_social_users_provider_id ON social_users(provider_id);
CREATE INDEX idx_social_users_email ON social_users(email);