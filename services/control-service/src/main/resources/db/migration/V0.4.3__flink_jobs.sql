-- Flink Jobs Table
-- Phase 4: Real-time Risk Evaluation - Flink Jobs for stream processing

-- Flink Jobs table
CREATE TABLE flink_jobs (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
    description TEXT,
    job_type VARCHAR(50) NOT NULL,
    job_config TEXT,
    source_config TEXT,
    sink_config TEXT,
    parallelism INTEGER DEFAULT 1,
    checkpoint_interval INTEGER DEFAULT 60000,
    rule_ids TEXT,
    evaluation_config TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    flink_job_id VARCHAR(100),
    flink_url VARCHAR(500),
    deployed_at TIMESTAMP,
    started_at TIMESTAMP,
    stopped_at TIMESTAMP,
    total_events_processed BIGINT DEFAULT 0,
    total_risk_cases_created BIGINT DEFAULT 0,
    total_actions_executed BIGINT DEFAULT 0,
    last_metrics_update TIMESTAMP,
    error_message TEXT,
    failure_count INTEGER DEFAULT 0,
    last_failure_at TIMESTAMP,
    version VARCHAR(20) DEFAULT '1.0',
    parent_job_id VARCHAR(32),
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id),
    FOREIGN KEY (parent_job_id) REFERENCES flink_jobs(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_flink_jobs_game_id ON flink_jobs(game_id);
CREATE INDEX idx_flink_jobs_environment_id ON flink_jobs(environment_id);
CREATE INDEX idx_flink_jobs_status ON flink_jobs(status);
CREATE INDEX idx_flink_jobs_job_type ON flink_jobs(job_type);
CREATE INDEX idx_flink_jobs_flink_job_id ON flink_jobs(flink_job_id);
CREATE INDEX idx_flink_jobs_parent_job_id ON flink_jobs(parent_job_id);
CREATE INDEX idx_flink_jobs_created_at ON flink_jobs(created_at DESC);

-- Index for running jobs
CREATE INDEX idx_flink_jobs_running ON flink_jobs(game_id, status) WHERE status = 'RUNNING';

-- Unique constraint on game + name
CREATE UNIQUE INDEX idx_flink_jobs_name ON flink_jobs(game_id, name) WHERE deleted_at IS NULL;

-- Insert example Flink jobs
INSERT INTO flink_jobs (id, game_id, name, display_name, description, job_type, status, parallelism) VALUES
('fj_risk_eval_default', 'DEFAULT', 'risk_evaluation_default', 'Default Risk Evaluation', 'Real-time risk evaluation for all events', 'RISK_EVALUATION', 'DRAFT', 2),
('fj_fraud_detect_default', 'DEFAULT', 'fraud_detection_default', 'Fraud Detection', 'Fraud pattern detection and prevention', 'FRAUD_DETECTION', 'DRAFT', 1),
('fj_anomaly_detect_default', 'DEFAULT', 'anomaly_detection_default', 'Anomaly Detection', 'Statistical anomaly detection in user behavior', 'ANOMALY_DETECTION', 'DRAFT', 1);
