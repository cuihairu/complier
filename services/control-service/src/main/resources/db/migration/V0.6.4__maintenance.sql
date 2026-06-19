-- Maintenance and System Management Tables
-- Phase 6: Maintenance Mode and System Management

-- Maintenance Windows table
CREATE TABLE maintenance_windows (
    id VARCHAR(32) PRIMARY KEY,
    maintenance_type VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    maintenance_status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    impact_scope VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    game_id VARCHAR(32),
    environment_id VARCHAR(32),
    service VARCHAR(100),
    region VARCHAR(50),
    scheduled_start TIMESTAMP NOT NULL,
    scheduled_end TIMESTAMP NOT NULL,
    actual_start TIMESTAMP,
    actual_end TIMESTAMP,
    extended_until TIMESTAMP,
    estimated_duration_minutes INTEGER,
    progress_percent INTEGER DEFAULT 0,
    affected_services TEXT,
    notification_sent BOOLEAN DEFAULT false,
    notification_sent_at TIMESTAMP,
    notification_channels TEXT,
    maintenance_tasks TEXT,
    rollout_plan TEXT,
    rollback_plan TEXT,
    impact_summary TEXT,
    created_by VARCHAR(64) NOT NULL,
    assigned_to VARCHAR(64),
    approved_by VARCHAR(64),
    approved_at TIMESTAMP,
    completion_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_maintenance_windows_maintenance_status ON maintenance_windows(maintenance_status);
CREATE INDEX idx_maintenance_windows_maintenance_type ON maintenance_windows(maintenance_type);
CREATE INDEX idx_maintenance_windows_impact_scope ON maintenance_windows(impact_scope);
CREATE INDEX idx_maintenance_windows_game_id ON maintenance_windows(game_id);
CREATE INDEX idx_maintenance_windows_scheduled_start ON maintenance_windows(scheduled_start);
CREATE INDEX idx_maintenance_windows_scheduled_end ON maintenance_windows(scheduled_end);
CREATE INDEX idx_maintenance_windows_deleted_at ON maintenance_windows(deleted_at);

-- Index for active maintenances
CREATE INDEX idx_maintenance_windows_active ON maintenance_windows(maintenance_status) WHERE maintenance_status = 'IN_PROGRESS';

-- System Configs table
CREATE TABLE system_configs (
    id VARCHAR(32) PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOM',
    config_value TEXT,
    default_value TEXT,
    value_type VARCHAR(20),
    description VARCHAR(500),
    category VARCHAR(100),
    is_sensitive BOOLEAN DEFAULT false,
    is_encrypted BOOLEAN DEFAULT false,
    is_public BOOLEAN DEFAULT false,
    is_readonly BOOLEAN DEFAULT false,
    requires_restart BOOLEAN DEFAULT false,
    validation_regex VARCHAR(500),
    min_value VARCHAR(100),
    max_value VARCHAR(100),
    allowed_values TEXT,
    version INTEGER DEFAULT 1,
    last_modified_by VARCHAR(64),
    last_modified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes for efficient lookups
CREATE INDEX idx_system_configs_config_key ON system_configs(config_key);
CREATE INDEX idx_system_configs_config_type ON system_configs(config_type);
CREATE INDEX idx_system_configs_category ON system_configs(category);
CREATE INDEX idx_system_configs_is_public ON system_configs(is_public);
CREATE INDEX idx_system_configs_deleted_at ON system_configs(deleted_at);

-- Feature Flags table
CREATE TABLE feature_flags (
    id VARCHAR(32) PRIMARY KEY,
    flag_key VARCHAR(100) NOT NULL UNIQUE,
    flag_name VARCHAR(100) NOT NULL,
    flag_status VARCHAR(30) NOT NULL DEFAULT 'DISABLED',
    flag_type VARCHAR(30) NOT NULL DEFAULT 'BOOLEAN',
    description TEXT,
    category VARCHAR(100),
    owner VARCHAR(64),
    tags TEXT,
    default_value BOOLEAN DEFAULT false,
    percentage_value INTEGER DEFAULT 0,
    whitelist_users TEXT,
    blacklist_users TEXT,
    whitelist_games TEXT,
    blacklist_games TEXT,
    conditions TEXT,
    rules TEXT,
    dependencies TEXT,
    rollout_strategy VARCHAR(50),
    rollout_steps TEXT,
    current_step INTEGER DEFAULT 0,
    scheduled_enable_at TIMESTAMP,
    scheduled_disable_at TIMESTAMP,
    expiry_date TIMESTAMP,
    created_by VARCHAR(64) NOT NULL,
    last_modified_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes for efficient lookups
CREATE INDEX idx_feature_flags_flag_key ON feature_flags(flag_key);
CREATE INDEX idx_feature_flags_flag_status ON feature_flags(flag_status);
CREATE INDEX idx_feature_flags_flag_type ON feature_flags(flag_type);
CREATE INDEX idx_feature_flags_category ON feature_flags(category);
CREATE INDEX idx_feature_flags_scheduled_enable_at ON feature_flags(scheduled_enable_at);
CREATE INDEX idx_feature_flags_scheduled_disable_at ON feature_flags(scheduled_disable_at);
CREATE INDEX idx_feature_flags_expiry_date ON feature_flags(expiry_date);
CREATE INDEX idx_feature_flags_deleted_at ON feature_flags(deleted_at);

-- Index for enabled flags
CREATE INDEX idx_feature_flags_enabled ON feature_flags(flag_status) WHERE flag_status = 'ENABLED';

-- Insert example maintenance windows
INSERT INTO maintenance_windows (id, maintenance_type, maintenance_status, title, impact_scope, scheduled_start, scheduled_end, created_by) VALUES
('mw_1', 'SCHEDULED', 'COMPLETED', 'Database Maintenance', 'GLOBAL', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '2 hours', 'admin'),
('mw_2', 'SCHEDULED', 'SCHEDULED', 'API Server Upgrade', 'GLOBAL', NOW() + INTERVAL '7 days', NOW() + INTERVAL '7 days' + INTERVAL '4 hours', 'admin');

-- Insert example system configs
INSERT INTO system_configs (id, config_key, config_type, config_value, value_type, description, category) VALUES
('sc_1', 'system.maintenance_mode', 'SYSTEM', 'false', 'boolean', 'Enable maintenance mode', 'system'),
('sc_2', 'api.max_requests_per_minute', 'LIMIT', '10000', 'integer', 'Maximum API requests per minute', 'performance'),
('sc_3', 'retention.events_days', 'RETENTION', '90', 'integer', 'Event data retention period in days', 'retention'),
('sc_4', 'notification.email_enabled', 'NOTIFICATION', 'true', 'boolean', 'Enable email notifications', 'notification');

-- Insert example feature flags
INSERT INTO feature_flags (id, flag_key, flag_name, flag_status, flag_type, description, category, created_by) VALUES
('ff_1', 'realtime_analytics', 'Real-time Analytics', 'ENABLED', 'BOOLEAN', 'Enable real-time analytics processing', 'analytics', 'admin'),
('ff_2', 'advanced_dashboard', 'Advanced Dashboard', 'CONDITIONAL', 'WHITELIST', 'Enable advanced dashboard features', 'ui', 'admin'),
('ff_3', 'export_large_datasets', 'Export Large Datasets', 'STAGED_ROLLOUT', 'PERCENTAGE', 'Enable export of large datasets', 'export', 'admin');
