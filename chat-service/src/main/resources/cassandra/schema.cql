-- Create keyspace
CREATE KEYSPACE IF NOT EXISTS fintech_chat
WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 1
};

USE fintech_chat;

-- Messages table - partitioned by conversation_id, clustered by message_time
CREATE TABLE IF NOT EXISTS messages (
    conversation_id UUID,
    message_id UUID,
    sender_id UUID,
    content TEXT,
    message_type TEXT, -- TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION
    media_url TEXT,
    metadata TEXT, -- JSON string for additional data
    status TEXT, -- SENT, DELIVERED, READ
    reply_to_id UUID,
    forwarded_from UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_for SET<UUID>, -- Users who deleted this message
    PRIMARY KEY (conversation_id, created_at, message_id)
) WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);

-- Create index on sender_id for quick lookup
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages (sender_id);

-- Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    type TEXT, -- ONE_TO_ONE, GROUP
    name TEXT,
    created_by UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_message_id UUID,
    last_message_content TEXT,
    last_message_time TIMESTAMP,
    last_message_sender UUID,
    is_archived BOOLEAN,
    metadata TEXT -- JSON string for group icon, description, etc.
);

-- Conversation participants
CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id UUID,
    user_id UUID,
    role TEXT, -- ADMIN, MEMBER
    joined_at TIMESTAMP,
    left_at TIMESTAMP,
    is_active BOOLEAN,
    last_read_message_id UUID,
    last_read_time TIMESTAMP,
    notification_enabled BOOLEAN,
    PRIMARY KEY (conversation_id, user_id)
);

-- User conversations index (for quick lookup of user's conversations)
CREATE TABLE IF NOT EXISTS user_conversations (
    user_id UUID,
    conversation_id UUID,
    last_message_time TIMESTAMP,
    unread_count INT,
    PRIMARY KEY (user_id, last_message_time, conversation_id)
) WITH CLUSTERING ORDER BY (last_message_time DESC);

-- Delivery receipts
CREATE TABLE IF NOT EXISTS delivery_receipts (
    message_id UUID,
    user_id UUID,
    status TEXT, -- DELIVERED, READ
    updated_at TIMESTAMP,
    PRIMARY KEY (message_id, user_id)
);