-- Data Pipeline Tables
-- Phase 7: Data Pipeline Enhancements - Pipeline orchestration and quality checks

-- Pipelines table
CREATE TABLE pipelines (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    pipeline_name VARCHAR(100) NOT NULL,
    pipeline_type VARCHAR(30) NOT NULL DEFAULT 'BATCH',
    pipeline_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    description VARCHAR(500),
    version INTEGER DEFAULT 1,
    source_config TEXT,
    transform_config TEXT,
    destination_config TEXT,
    quality_rules TEXT,
    error_handling TEXT,
    schedule_config TEXT,
    resource_config TEXT,
    partition_config TEXT,
    priority INTEGER DEFAULT 5,
    max_retries INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 3600,
    enabled BOOLEAN DEFAULT true,
    last_run_at TIMESTAMP,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    run_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failure_count INTEGER DEFAULT 0,
    last_error TEXT,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_pipelines_game_id ON pipelines(game_id);
CREATE INDEX idx_pipelines_environment_id ON pipelines(environment_id);
CREATE INDEX idx_pipelines_pipeline_status ON pipelines(pipeline_status);
CREATE INDEX idx_pipelines_pipeline_type ON pipelines(pipeline_type);
CREATE INDEX idx_pipelines_enabled ON pipelines(enabled);
CREATE INDEX idx_pipelines_priority ON pipelines(priority DESC);
CREATE INDEX idx_pipelines_last_run_at ON pipelines(last_run_at);
CREATE INDEX idx_pipelines_deleted_at ON pipelines(deleted_at);

-- Index for active pipelines
CREATE INDEX idx_pipelines_active ON pipelines(pipeline_status, enabled) WHERE pipeline_status = 'ACTIVE' AND enabled = true;

-- Pipeline Jobs table
CREATE TABLE pipeline_jobs (
    id VARCHAR(32) PRIMARY KEY,
    pipeline_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    job_name VARCHAR(100) NOT NULL,
    job_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    run_id VARCHAR(100),
    trigger_type VARCHAR(50),
    triggered_by VARCHAR(64),
    input_params TEXT,
    output_params TEXT,
    stage_results TEXT,
    quality_metrics TEXT,
    processed_rows BIGINT DEFAULT 0,
    error_rows BIGINT DEFAULT 0,
    skipped_rows BIGINT DEFAULT 0,
    data_source_size_bytes BIGINT,
    data_destination_size_bytes BIGINT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    queue_time_ms BIGINT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message TEXT,
    error_stack_trace TEXT,
    warning_message TEXT,
    worker_node VARCHAR(100),
    partition_key VARCHAR(100),
    batch_id VARCHAR(100),
    checkpoint_location VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (pipeline_id) REFERENCES pipelines(id),
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_pipeline_jobs_pipeline_id ON pipeline_jobs(pipeline_id);
CREATE INDEX idx_pipeline_jobs_game_id ON pipeline_jobs(game_id);
CREATE INDEX idx_pipeline_jobs_job_status ON pipeline_jobs(job_status);
CREATE INDEX idx_pipeline_jobs_created_at ON pipeline_jobs(created_at DESC);
CREATE INDEX idx_pipeline_jobs_started_at ON pipeline_jobs(started_at DESC);
CREATE INDEX idx_pipeline_jobs_completed_at ON pipeline_jobs(completed_at DESC);

-- Index for pending jobs
CREATE INDEX idx_pipeline_jobs_pending ON pipeline_jobs(job_status) WHERE job_status = 'PENDING';

-- Index for running jobs
CREATE INDEX idx_pipeline_jobs_running ON pipeline_jobs(job_status) WHERE job_status = 'RUNNING';

-- Data Quality Rules table
CREATE TABLE data_quality_rules (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32),
    pipeline_id VARCHAR(32),
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    rule_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500),
    target_table VARCHAR(100),
    target_column VARCHAR(100),
    rule_definition TEXT,
    conditions TEXT,
    threshold_value VARCHAR(255),
    threshold_operator VARCHAR(20),
    min_threshold VARCHAR(255),
    max_threshold VARCHAR(255),
    allowed_values TEXT,
    pattern_regex VARCHAR(500),
    reference_table VARCHAR(100),
    reference_column VARCHAR(100),
    sql_condition TEXT,
    error_message_template VARCHAR(500),
    action_on_failure VARCHAR(50),
    sample_size INTEGER DEFAULT 10000,
    enabled BOOLEAN DEFAULT true,
    last_evaluated_at TIMESTAMP,
    last_evaluation_result TEXT,
    last_violation_count INTEGER DEFAULT 0,
    total_evaluations INTEGER DEFAULT 0,
    total_violations INTEGER DEFAULT 0,
    tags TEXT,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (pipeline_id) REFERENCES pipelines(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_data_quality_rules_game_id ON data_quality_rules(game_id);
CREATE INDEX idx_data_quality_rules_pipeline_id ON data_quality_rules(pipeline_id);
CREATE INDEX idx_data_quality_rules_rule_type ON data_quality_rules(rule_type);
CREATE INDEX idx_data_quality_rules_severity ON data_quality_rules(severity);
CREATE INDEX idx_data_quality_rules_rule_status ON data_quality_rules(rule_status);
CREATE INDEX idx_data_quality_rules_enabled ON data_quality_rules(enabled);
CREATE INDEX idx_data_quality_rules_target_table ON data_quality_rules(target_table);
CREATE INDEX idx_data_quality_rules_deleted_at ON data_quality_rules(deleted_at);

-- Index for active rules
CREATE INDEX idx_data_quality_rules_active ON data_quality_rules(enabled, rule_status) WHERE enabled = true AND rule_status = 'ACTIVE';

-- Insert example pipelines
INSERT INTO pipelines (id, game_id, pipeline_name, pipeline_type, pipeline_status, priority, created_by) VALUES
('pipe_1', 'DEFAULT', 'Event Ingestion Pipeline', 'STREAMING', 'ACTIVE', 10, 'admin'),
('pipe_2', 'DEFAULT', 'Daily Analytics Pipeline', 'BATCH', 'ACTIVE', 5, 'admin'),
('pipe_3', 'DEFAULT', 'User Data Sync Pipeline', 'HYBRID', 'PAUSED', 3, 'admin');

-- Insert example pipeline jobs
INSERT INTO pipeline_jobs (id, pipeline_id, game_id, job_name, job_status, trigger_type, processed_rows, error_rows) VALUES
('pjob_1', 'pipe_1', 'DEFAULT', 'Event Ingestion - Batch 1', 'COMPLETED', 'schedule', 50000, 0),
('pjob_2', 'pipe_2', 'DEFAULT', 'Daily Analytics - 2025-06-19', 'RUNNING', 'schedule', 25000, 0),
('pjob_3', 'pipe_1', 'DEFAULT', 'Event Ingestion - Batch 2', 'PENDING', 'schedule', 0, 0);

-- Insert example data quality rules
INSERT INTO data_quality_rules (id, game_id, pipeline_id, rule_name, rule_type, severity, target_table, action_on_failure, created_by) VALUES
('dqr_1', 'DEFAULT', 'pipe_1', 'Event Schema Validation', 'SCHEMA', 'ERROR', 'events', 'stop', 'admin'),
('dqr_2', 'DEFAULT', 'pipe_1', 'User ID Completeness', 'COMPLETENESS', 'WARNING', 'events', 'warn', 'admin'),
('dqr_3', 'DEFAULT', 'pipe_2', 'Timestamp Range Check', 'RANGE', 'ERROR', 'events', 'stop', 'admin');
