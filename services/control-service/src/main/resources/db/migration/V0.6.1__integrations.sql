-- Integration Tables
-- Phase 6: Integration Layer - External system connectors

-- Integrations table
CREATE TABLE integrations (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    integration_type VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    auth_type VARCHAR(50),
    endpoint_url VARCHAR(500),
    api_key VARCHAR(255),
    api_secret TEXT,
    bearer_token VARCHAR(500),
    username VARCHAR(100),
    password VARCHAR(255),
    config TEXT,
    headers TEXT,
    integration_status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    last_verified_at TIMESTAMP,
    last_error TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 30,
    enabled BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(64),
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_integrations_game_id ON integrations(game_id);
CREATE INDEX idx_integrations_integration_type ON integrations(integration_type);
CREATE INDEX idx_integrations_integration_status ON integrations(integration_status);
CREATE INDEX idx_integrations_enabled ON integrations(enabled);
CREATE INDEX idx_integrations_deleted_at ON integrations(deleted_at);
CREATE INDEX idx_integrations_priority ON integrations(priority DESC);
CREATE INDEX idx_integrations_last_verified_at ON integrations(last_verified_at);

-- Index for active integrations
CREATE INDEX idx_integrations_active ON integrations(game_id, integration_status, enabled) WHERE integration_status = 'ACTIVE' AND enabled = true;

-- Index for failed integrations
CREATE INDEX idx_integrations_failed ON integrations(integration_status, retry_count, max_retries) WHERE integration_status = 'FAILED';

-- Integration Logs table
CREATE TABLE integration_logs (
    id VARCHAR(32) PRIMARY KEY,
    integration_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32) NOT NULL,
    integration_type VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    call_status VARCHAR(20) NOT NULL,
    log_level VARCHAR(20) NOT NULL DEFAULT 'INFO',
    http_method VARCHAR(10),
    request_url VARCHAR(1000),
    request_headers TEXT,
    request_body TEXT,
    response_status INTEGER,
    response_headers TEXT,
    response_body TEXT,
    error_message TEXT,
    error_stack_trace TEXT,
    retry_attempt INTEGER DEFAULT 0,
    duration_ms BIGINT,
    request_size_bytes BIGINT,
    response_size_bytes BIGINT,
    correlation_id VARCHAR(100),
    triggered_by VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (integration_id) REFERENCES integrations(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_integration_logs_integration_id ON integration_logs(integration_id);
CREATE INDEX idx_integration_logs_game_id ON integration_logs(game_id);
CREATE INDEX idx_integration_logs_event_type ON integration_logs(event_type);
CREATE INDEX idx_integration_logs_call_status ON integration_logs(call_status);
CREATE INDEX idx_integration_logs_created_at ON integration_logs(created_at DESC);
CREATE INDEX idx_integration_logs_correlation_id ON integration_logs(correlation_id);
CREATE INDEX idx_integration_logs_expires_at ON integration_logs(expires_at);

-- Index for failed logs
CREATE INDEX idx_integration_logs_failed ON integration_logs(integration_id, call_status) WHERE call_status IN ('FAILED', 'TIMEOUT');

-- Index for recent logs
CREATE INDEX idx_integration_logs_recent ON integration_logs(game_id, created_at DESC);

-- Insert example integrations
INSERT INTO integrations (id, game_id, integration_type, name, auth_type, endpoint_url, integration_status, enabled, priority) VALUES
('int_1', 'DEFAULT', 'SLACK', 'Default Slack Notifications', 'WEBHOOK', 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX', 'ACTIVE', true, 10),
('int_2', 'DEFAULT', 'EMAIL_SES', 'AWS SES Email Service', 'API_KEY', 'https://email.us-east-1.amazonaws.com', 'INACTIVE', true, 5),
('int_3', 'DEFAULT', 'WEBHOOK', 'Custom Webhook Endpoint', 'NONE', 'https://api.example.com/webhook', 'ACTIVE', true, 0);

-- Insert example integration logs
INSERT INTO integration_logs (id, integration_id, game_id, integration_type, event_type, call_status, http_method, request_url, response_status, duration_ms) VALUES
('ilog_1', 'int_1', 'DEFAULT', 'SLACK', 'risk_case', 'SUCCESS', 'POST', 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX', 200, 150),
('ilog_2', 'int_1', 'DEFAULT', 'SLACK', 'block', 'SUCCESS', 'POST', 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX', 200, 120),
('ilog_3', 'int_3', 'DEFAULT', 'WEBHOOK', 'alert', 'FAILED', 'POST', 'https://api.example.com/webhook', 500, 5000);
