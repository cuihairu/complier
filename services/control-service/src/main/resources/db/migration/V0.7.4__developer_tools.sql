-- Developer Tools Tables
-- Phase 7: Developer Tools and SDK Enhancements

-- SDK Keys table
CREATE TABLE sdk_keys (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    environment VARCHAR(50),
    key_name VARCHAR(100) NOT NULL,
    public_key VARCHAR(64) NOT NULL,
    secret_key_hash VARCHAR(128),
    platform VARCHAR(30) NOT NULL,
    key_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    delivery_mode VARCHAR(30) NOT NULL DEFAULT 'REALTIME',
    sdk_version_constraint VARCHAR(50),
    min_sdk_version VARCHAR(20),
    max_sdk_version VARCHAR(20),
    allowed_domains TEXT,
    allowed_ips TEXT,
    rate_limit_rpm INTEGER DEFAULT 1000,
    rate_limit_rps INTEGER DEFAULT 100,
    batch_size_limit INTEGER DEFAULT 500,
    batch_interval_ms INTEGER DEFAULT 3000,
    max_event_size_bytes INTEGER DEFAULT 65536,
    max_batch_size_bytes INTEGER DEFAULT 1048576,
    enable_compression BOOLEAN DEFAULT true,
    enable_encryption BOOLEAN DEFAULT false,
    retry_policy TEXT,
    offline_config TEXT,
    flush_config TEXT,
    telemetry_config TEXT,
    custom_config TEXT,
    total_events_sent BIGINT DEFAULT 0,
    total_batches_sent BIGINT DEFAULT 0,
    total_errors BIGINT DEFAULT 0,
    last_event_at TIMESTAMP,
    last_error_at TIMESTAMP,
    last_error_message TEXT,
    expires_at TIMESTAMP,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_sdk_keys_game_id ON sdk_keys(game_id);
CREATE INDEX idx_sdk_keys_environment ON sdk_keys(environment);
CREATE INDEX idx_sdk_keys_platform ON sdk_keys(platform);
CREATE INDEX idx_sdk_keys_key_status ON sdk_keys(key_status);
CREATE INDEX idx_sdk_keys_delivery_mode ON sdk_keys(delivery_mode);
CREATE INDEX idx_sdk_keys_public_key ON sdk_keys(public_key);
CREATE INDEX idx_sdk_keys_created_by ON sdk_keys(created_by);
CREATE INDEX idx_sdk_keys_deleted_at ON sdk_keys(deleted_at);

-- Index for active keys
CREATE INDEX idx_sdk_keys_active ON sdk_keys(key_status, expires_at) WHERE key_status = 'ACTIVE';

-- Unique index for public key
CREATE UNIQUE INDEX idx_sdk_keys_public_key_unique ON sdk_keys(public_key) WHERE deleted_at IS NULL;

-- SDK Versions table
CREATE TABLE sdk_versions (
    id VARCHAR(32) PRIMARY KEY,
    platform VARCHAR(30) NOT NULL,
    version VARCHAR(20) NOT NULL,
    version_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    change_type VARCHAR(30) NOT NULL DEFAULT 'PATCH',
    release_notes TEXT,
    changelog TEXT,
    breaking_changes TEXT,
    migration_guide TEXT,
    download_url VARCHAR(500),
    package_name VARCHAR(200),
    package_manager VARCHAR(50),
    checksum_sha256 VARCHAR(64),
    file_size_bytes BIGINT,
    min_platform_version VARCHAR(20),
    max_platform_version VARCHAR(20),
    dependencies TEXT,
    api_compatibility TEXT,
    feature_flags TEXT,
    deprecation_notice TEXT,
    retirement_date TIMESTAMP,
    released_at TIMESTAMP,
    released_by VARCHAR(64),
    total_downloads BIGINT DEFAULT 0,
    active_installations BIGINT DEFAULT 0,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for efficient lookups
CREATE INDEX idx_sdk_versions_platform ON sdk_versions(platform);
CREATE INDEX idx_sdk_versions_version_status ON sdk_versions(version_status);
CREATE INDEX idx_sdk_versions_change_type ON sdk_versions(change_type);
CREATE INDEX idx_sdk_versions_released_at ON sdk_versions(released_at DESC);

-- Unique index for platform + version
CREATE UNIQUE INDEX idx_sdk_versions_platform_version ON sdk_versions(platform, version);

-- Telemetry Configs table
CREATE TABLE telemetry_configs (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32),
    environment_id VARCHAR(32),
    config_name VARCHAR(100) NOT NULL,
    config_type VARCHAR(30) NOT NULL,
    config_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    description VARCHAR(500),
    is_default BOOLEAN DEFAULT false,
    priority INTEGER DEFAULT 100,
    delivery_mode VARCHAR(30),
    batch_size INTEGER DEFAULT 500,
    batch_interval_ms INTEGER DEFAULT 3000,
    max_queue_size INTEGER DEFAULT 10000,
    flush_on_background BOOLEAN DEFAULT true,
    flush_on_app_close BOOLEAN DEFAULT true,
    enable_compression BOOLEAN DEFAULT true,
    compression_algorithm VARCHAR(30) DEFAULT 'GZIP',
    compression_level INTEGER DEFAULT 6,
    compression_threshold_bytes INTEGER DEFAULT 1024,
    enable_encryption BOOLEAN DEFAULT false,
    encryption_algorithm VARCHAR(50),
    encryption_key_id VARCHAR(100),
    max_retries INTEGER DEFAULT 3,
    retry_interval_ms INTEGER DEFAULT 1000,
    retry_backoff_multiplier DECIMAL(5,2) DEFAULT 2.0,
    max_retry_interval_ms INTEGER DEFAULT 30000,
    retry_on_status_codes VARCHAR(100),
    enable_offline_storage BOOLEAN DEFAULT true,
    offline_storage_max_mb INTEGER DEFAULT 50,
    offline_storage_ttl_hours INTEGER DEFAULT 72,
    offline_batch_size INTEGER DEFAULT 100,
    enable_telemetry BOOLEAN DEFAULT true,
    telemetry_interval_ms INTEGER DEFAULT 60000,
    report_errors BOOLEAN DEFAULT true,
    report_performance BOOLEAN DEFAULT true,
    sample_rate DECIMAL(5,4) DEFAULT 1.0,
    connection_timeout_ms INTEGER DEFAULT 10000,
    read_timeout_ms INTEGER DEFAULT 30000,
    write_timeout_ms INTEGER DEFAULT 30000,
    custom_config TEXT,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_telemetry_configs_game_id ON telemetry_configs(game_id);
CREATE INDEX idx_telemetry_configs_environment_id ON telemetry_configs(environment_id);
CREATE INDEX idx_telemetry_configs_config_type ON telemetry_configs(config_type);
CREATE INDEX idx_telemetry_configs_config_status ON telemetry_configs(config_status);
CREATE INDEX idx_telemetry_configs_is_default ON telemetry_configs(is_default);
CREATE INDEX idx_telemetry_configs_priority ON telemetry_configs(priority);
CREATE INDEX idx_telemetry_configs_deleted_at ON telemetry_configs(deleted_at);

-- Index for active configs
CREATE INDEX idx_telemetry_configs_active ON telemetry_configs(game_id, config_type, config_status) WHERE config_status = 'ACTIVE';

-- Index for default configs
CREATE INDEX idx_telemetry_configs_default ON telemetry_configs(game_id, is_default) WHERE is_default = true;

-- SDK Usage Statistics table
CREATE TABLE sdk_usage_stats (
    id VARCHAR(32) PRIMARY KEY,
    sdk_key_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    stat_date DATE NOT NULL,
    stat_hour INTEGER,
    events_sent BIGINT DEFAULT 0,
    batches_sent BIGINT DEFAULT 0,
    errors_count BIGINT DEFAULT 0,
    avg_latency_ms INTEGER,
    p95_latency_ms INTEGER,
    p99_latency_ms INTEGER,
    unique_users BIGINT DEFAULT 0,
    unique_sessions BIGINT DEFAULT 0,
    data_volume_bytes BIGINT DEFAULT 0,
    compression_ratio DECIMAL(5,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sdk_key_id) REFERENCES sdk_keys(id),
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for usage stats
CREATE INDEX idx_sdk_usage_stats_sdk_key_id ON sdk_usage_stats(sdk_key_id);
CREATE INDEX idx_sdk_usage_stats_game_id ON sdk_usage_stats(game_id);
CREATE INDEX idx_sdk_usage_stats_platform ON sdk_usage_stats(platform);
CREATE INDEX idx_sdk_usage_stats_date ON sdk_usage_stats(stat_date DESC);
CREATE INDEX idx_sdk_usage_stats_key_date ON sdk_usage_stats(sdk_key_id, stat_date DESC);

-- SDK Error Logs table
CREATE TABLE sdk_error_logs (
    id VARCHAR(32) PRIMARY KEY,
    sdk_key_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    sdk_version VARCHAR(20),
    error_code VARCHAR(50),
    error_message TEXT,
    error_stack TEXT,
    device_info TEXT,
    os_version VARCHAR(50),
    app_version VARCHAR(50),
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sdk_key_id) REFERENCES sdk_keys(id),
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for error logs
CREATE INDEX idx_sdk_error_logs_sdk_key_id ON sdk_error_logs(sdk_key_id);
CREATE INDEX idx_sdk_error_logs_game_id ON sdk_error_logs(game_id);
CREATE INDEX idx_sdk_error_logs_platform ON sdk_error_logs(platform);
CREATE INDEX idx_sdk_error_logs_error_code ON sdk_error_logs(error_code);
CREATE INDEX idx_sdk_error_logs_occurred_at ON sdk_error_logs(occurred_at DESC);

-- Comments for documentation
COMMENT ON TABLE sdk_keys IS 'SDK密钥和配置管理';
COMMENT ON TABLE sdk_versions IS 'SDK版本跟踪和发布管理';
COMMENT ON TABLE telemetry_configs IS '事件交付和遥测配置';
COMMENT ON TABLE sdk_usage_stats IS 'SDK使用统计';
COMMENT ON TABLE sdk_error_logs IS 'SDK错误日志';
