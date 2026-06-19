-- Reports Table
-- Phase 5: Custom Report Builder - User-defined reports and executions

-- Reports table
CREATE TABLE reports (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
    description TEXT,
    report_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOM',
    report_category VARCHAR(50),
    data_source TEXT,
    query_config TEXT,
    filters TEXT,
    parameters TEXT,
    visualization TEXT,
    chart_type VARCHAR(50),
    group_by TEXT,
    aggregations TEXT,
    default_time_range VARCHAR(50) DEFAULT '7d',
    time_granularity VARCHAR(20) DEFAULT 'day',
    export_formats TEXT,
    schedule_config TEXT,
    recipients TEXT,
    access_control TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_runs BIGINT DEFAULT 0,
    last_run_at TIMESTAMP,
    last_run_status VARCHAR(50),
    version VARCHAR(20) DEFAULT '1.0',
    parent_report_id VARCHAR(32),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id),
    FOREIGN KEY (parent_report_id) REFERENCES reports(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_reports_game_id ON reports(game_id);
CREATE INDEX idx_reports_environment_id ON reports(environment_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_report_type ON reports(report_type);
CREATE INDEX idx_reports_report_category ON reports(report_category);
CREATE INDEX idx_reports_is_public ON reports(is_public);
CREATE INDEX idx_reports_total_runs ON reports(total_runs DESC);
CREATE INDEX idx_reports_last_run_at ON reports(last_run_at DESC);
CREATE INDEX idx_reports_parent_report_id ON reports(parent_report_id);

-- Unique constraint on game + name
CREATE UNIQUE INDEX idx_reports_name ON reports(game_id, name) WHERE deleted_at IS NULL;

-- Index for published reports
CREATE INDEX idx_reports_published ON reports(game_id, status) WHERE status = 'PUBLISHED';

-- Index for scheduled reports
CREATE INDEX idx_reports_scheduled ON reports(status) WHERE status = 'SCHEDULED';

-- Report Executions table
CREATE TABLE report_executions (
    id VARCHAR(32) PRIMARY KEY,
    report_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32) NOT NULL,
    execution_number BIGINT,
    triggered_by VARCHAR(64),
    trigger_type VARCHAR(20),
    parameters TEXT,
    filters TEXT,
    time_range VARCHAR(50),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    execution_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status_message VARCHAR(500),
    error_message TEXT,
    row_count BIGINT,
    result_summary TEXT,
    result_data TEXT,
    result_storage_path VARCHAR(500),
    result_size_bytes BIGINT,
    export_format VARCHAR(20),
    export_path VARCHAR(500),
    export_size_bytes BIGINT,
    execution_time_ms BIGINT,
    query_time_ms BIGINT,
    render_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES reports(id),
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_report_executions_report_id ON report_executions(report_id);
CREATE INDEX idx_report_executions_game_id ON report_executions(game_id);
CREATE INDEX idx_report_executions_execution_status ON report_executions(execution_status);
CREATE INDEX idx_report_executions_created_at ON report_executions(created_at DESC);
CREATE INDEX idx_report_executions_trigger_type ON report_executions(trigger_type);
CREATE INDEX idx_report_executions_start_time ON report_executions(start_time);

-- Index for pending executions
CREATE INDEX idx_report_executions_pending ON report_executions(execution_status) WHERE execution_status = 'PENDING';

-- Index for running executions
CREATE INDEX idx_report_executions_running ON report_executions(execution_status, start_time) WHERE execution_status = 'RUNNING';

-- Insert example reports
INSERT INTO reports (id, game_id, name, display_name, description, report_type, report_category, chart_type, status, query_config) VALUES
('rpt_daily_active_users', 'DEFAULT', 'daily_active_users', 'Daily Active Users', 'Daily active users report', 'TEMPLATE', 'analytics', 'line', 'PUBLISHED', '{"table": "events", "metric": "unique_users", "groupBy": ["date"]}'),
('rpt_revenue_summary', 'DEFAULT', 'revenue_summary', 'Revenue Summary', 'Revenue breakdown by category', 'TEMPLATE', 'revenue', 'bar', 'PUBLISHED', '{"table": "revenue", "metrics": ["total_revenue", "avg_revenue"]}'),
('rpt_risk_overview', 'DEFAULT', 'risk_overview', 'Risk Overview', 'Risk cases and blocks summary', 'TEMPLATE', 'risk', 'table', 'PUBLISHED', '{"tables": ["risk_cases", "block_lists"]}');

-- Insert example executions
INSERT INTO report_executions (id, report_id, game_id, execution_number, trigger_type, execution_status, row_count, execution_time_ms) VALUES
('re_1', 'rpt_daily_active_users', 'DEFAULT', 1, 'manual', 'COMPLETED', 30, 150),
('re_2', 'rpt_revenue_summary', 'DEFAULT', 1, 'manual', 'COMPLETED', 15, 120),
('re_3', 'rpt_risk_overview', 'DEFAULT', 1, 'manual', 'COMPLETED', 50, 200);
