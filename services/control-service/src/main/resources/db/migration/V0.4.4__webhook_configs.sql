-- Webhook Configuration and Logging Tables
-- Phase 4: Webhook Integration - Risk alerts and notifications

-- Webhook Configs table
CREATE TABLE webhook_configs (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
    description TEXT,
    webhook_url VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) DEFAULT 'POST',
    auth_type VARCHAR(20),
    auth_config TEXT,
    event_types TEXT,
    risk_levels TEXT,
    request_headers TEXT,
    request_template TEXT,
    timeout_seconds INTEGER DEFAULT 30,
    retry_config TEXT,
    max_retries INTEGER DEFAULT 3,
    retry_backoff_ms INTEGER DEFAULT 1000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_sent BIGINT DEFAULT 0,
    total_success BIGINT DEFAULT 0,
    total_failed BIGINT DEFAULT 0,
    last_sent_at TIMESTAMP,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    last_error TEXT,
    version VARCHAR(20) DEFAULT '1.0',
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_webhook_configs_game_id ON webhook_configs(game_id);
CREATE INDEX idx_webhook_configs_environment_id ON webhook_configs(environment_id);
CREATE INDEX idx_webhook_configs_status ON webhook_configs(status);
CREATE INDEX idx_webhook_configs_created_at ON webhook_configs(created_at DESC);

-- Unique constraint on game + name
CREATE UNIQUE INDEX idx_webhook_configs_name ON webhook_configs(game_id, name) WHERE deleted_at IS NULL;

-- Webhook Logs table
CREATE TABLE webhook_logs (
    id VARCHAR(32) PRIMARY KEY,
    webhook_config_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32) NOT NULL,
    risk_case_id VARCHAR(32),
    event_type VARCHAR(50),
    event_id VARCHAR(100),
    request_url VARCHAR(500),
    request_method VARCHAR(10),
    request_headers TEXT,
    request_body TEXT,
    response_status INTEGER,
    response_headers TEXT,
    response_body TEXT,
    response_time_ms BIGINT,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    attempt_number INTEGER DEFAULT 1,
    error_message TEXT,
    error_type VARCHAR(100),
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (webhook_config_id) REFERENCES webhook_configs(id),
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (risk_case_id) REFERENCES risk_cases(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_webhook_logs_config_id ON webhook_logs(webhook_config_id);
CREATE INDEX idx_webhook_logs_game_id ON webhook_logs(game_id);
CREATE INDEX idx_webhook_logs_risk_case_id ON webhook_logs(risk_case_id);
CREATE INDEX idx_webhook_logs_delivery_status ON webhook_logs(delivery_status);
CREATE INDEX idx_webhook_logs_created_at ON webhook_logs(created_at DESC);
CREATE INDEX idx_webhook_logs_next_retry_at ON webhook_logs(next_retry_at);

-- Index for pending retries
CREATE INDEX idx_webhook_logs_pending_retry ON webhook_logs(delivery_status, next_retry_at) WHERE delivery_status = 'RETRYING';

-- Insert example webhook configs
INSERT INTO webhook_configs (id, game_id, name, display_name, description, webhook_url, status, event_types) VALUES
('wc_slack_default', 'DEFAULT', 'slack_alerts', 'Slack Alerts', 'Send risk alerts to Slack channel', 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL', 'ACTIVE', 'risk_case,block'),
('wc_email_default', 'DEFAULT', 'email_alerts', 'Email Alerts', 'Send critical risk alerts via email', 'https://api.email-service.com/webhook', 'ACTIVE', 'risk_case'),
('wc_discord_default', 'DEFAULT', 'discord_alerts', 'Discord Alerts', 'Send risk alerts to Discord channel', 'https://discord.com/api/webhooks/YOUR/WEBHOOK', 'INACTIVE', 'risk_case');
