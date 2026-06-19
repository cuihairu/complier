-- Health Monitoring Tables
-- Phase 6: System Health Monitoring - Health checks and metrics

-- Health Checks table
CREATE TABLE health_checks (
    id VARCHAR(32) PRIMARY KEY,
    check_type VARCHAR(50) NOT NULL,
    check_name VARCHAR(100) NOT NULL,
    health_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    status_message VARCHAR(500),
    response_time_ms BIGINT,
    endpoint VARCHAR(500),
    details TEXT,
    metrics TEXT,
    threshold_config TEXT,
    warning_threshold_ms INTEGER DEFAULT 1000,
    critical_threshold_ms INTEGER DEFAULT 5000,
    consecutive_failures INTEGER DEFAULT 0,
    last_healthy_at TIMESTAMP,
    last_unhealthy_at TIMESTAMP,
    total_checks INTEGER DEFAULT 0,
    failed_checks INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT true,
    check_interval_seconds INTEGER DEFAULT 60,
    last_checked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for efficient lookups
CREATE INDEX idx_health_checks_check_type ON health_checks(check_type);
CREATE INDEX idx_health_checks_health_status ON health_checks(health_status);
CREATE INDEX idx_health_checks_enabled ON health_checks(enabled);
CREATE INDEX idx_health_checks_last_checked_at ON health_checks(last_checked_at);
CREATE INDEX idx_health_checks_last_unhealthy_at ON health_checks(last_unhealthy_at);

-- Index for unhealthy checks
CREATE INDEX idx_health_checks_unhealthy ON health_checks(health_status, enabled) WHERE health_status IN ('UNHEALTHY', 'DOWN');

-- Health Metrics table
CREATE TABLE health_metrics (
    id VARCHAR(32) PRIMARY KEY,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    source VARCHAR(100),
    metric_value DECIMAL(20,4),
    unit VARCHAR(20),
    min_value DECIMAL(20,4),
    max_value DECIMAL(20,4),
    avg_value DECIMAL(20,4),
    percentile_50 DECIMAL(20,4),
    percentile_95 DECIMAL(20,4),
    percentile_99 DECIMAL(20,4),
    warning_threshold DECIMAL(20,4),
    critical_threshold DECIMAL(20,4),
    is_anomaly BOOLEAN DEFAULT false,
    tags TEXT,
    dimensions TEXT,
    collected_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient lookups
CREATE INDEX idx_health_metrics_metric_type ON health_metrics(metric_type);
CREATE INDEX idx_health_metrics_source ON health_metrics(source);
CREATE INDEX idx_health_metrics_collected_at ON health_metrics(collected_at DESC);
CREATE INDEX idx_health_metrics_is_anomaly ON health_metrics(is_anomaly);

-- Index for recent metrics
CREATE INDEX idx_health_metrics_recent ON health_metrics(metric_type, collected_at DESC);

-- System Alerts table
CREATE TABLE system_alerts (
    id VARCHAR(32) PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    alert_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    source VARCHAR(100),
    affected_resource VARCHAR(200),
    metric_value DECIMAL(20,4),
    threshold_value DECIMAL(20,4),
    condition VARCHAR(50),
    details TEXT,
    context TEXT,
    runbook_url VARCHAR(500),
    acknowledged_by VARCHAR(64),
    acknowledged_at TIMESTAMP,
    acknowledgement_comment TEXT,
    assigned_to VARCHAR(64),
    resolved_by VARCHAR(64),
    resolved_at TIMESTAMP,
    resolution_comment TEXT,
    escalation_level INTEGER DEFAULT 0,
    escalated_at TIMESTAMP,
    notification_sent BOOLEAN DEFAULT false,
    snoozed_until TIMESTAMP,
    occurrence_count INTEGER DEFAULT 1,
    first_occurred_at TIMESTAMP,
    last_occurred_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes for efficient lookups
CREATE INDEX idx_system_alerts_alert_type ON system_alerts(alert_type);
CREATE INDEX idx_system_alerts_severity ON system_alerts(severity);
CREATE INDEX idx_system_alerts_alert_status ON system_alerts(alert_status);
CREATE INDEX idx_system_alerts_source ON system_alerts(source);
CREATE INDEX idx_system_alerts_deleted_at ON system_alerts(deleted_at);
CREATE INDEX idx_system_alerts_created_at ON system_alerts(created_at DESC);
CREATE INDEX idx_system_alerts_resolved_at ON system_alerts(resolved_at DESC);

-- Index for active alerts
CREATE INDEX idx_system_alerts_active ON system_alerts(alert_status, severity, deleted_at) WHERE alert_status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING');

-- Index for critical alerts
CREATE INDEX idx_system_alerts_critical ON system_alerts(severity, alert_status, deleted_at) WHERE severity IN ('CRITICAL', 'EMERGENCY');

-- Insert example health checks
INSERT INTO health_checks (id, check_type, check_name, health_status, enabled, check_interval_seconds) VALUES
('hc_1', 'DATABASE', 'primary-database', 'HEALTHY', true, 60),
('hc_2', 'STORAGE', 's3-storage', 'HEALTHY', true, 120),
('hc_3', 'CACHE', 'redis-cache', 'HEALTHY', true, 60),
('hc_4', 'EXTERNAL_API', 'payment-gateway', 'HEALTHY', true, 300);

-- Insert example system alerts
INSERT INTO system_alerts (id, alert_type, severity, alert_status, title, source, first_occurred_at, last_occurred_at) VALUES
('alert_1', 'HIGH_CPU', 'WARNING', 'OPEN', 'High CPU usage detected', 'server-1', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '5 minutes'),
('alert_2', 'SLOW_RESPONSE', 'ERROR', 'ACKNOWLEDGED', 'Slow API response times', 'api-gateway', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '30 minutes');
