-- Machine Learning Models Tables
-- Phase 7: ML Model Management - Model lifecycle, training, and predictions

-- ML Models table
CREATE TABLE ml_models (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32),
    model_name VARCHAR(100) NOT NULL,
    model_type VARCHAR(30) NOT NULL,
    model_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    version INTEGER DEFAULT 1,
    description VARCHAR(500),
    algorithm VARCHAR(100),
    framework VARCHAR(50),
    framework_version VARCHAR(50),
    model_config TEXT,
    hyperparameters TEXT,
    feature_config TEXT,
    input_schema TEXT,
    output_schema TEXT,
    training_config TEXT,
    model_artifact_path VARCHAR(500),
    model_size_bytes BIGINT,
    accuracy_metric DECIMAL(10,6),
    precision_metric DECIMAL(10,6),
    recall_metric DECIMAL(10,6),
    f1_score DECIMAL(10,6),
    auc_score DECIMAL(10,6),
    custom_metrics TEXT,
    baseline_model_id VARCHAR(32),
    is_ab_test BOOLEAN DEFAULT false,
    ab_test_config TEXT,
    traffic_split INTEGER DEFAULT 0,
    deployment_config TEXT,
    serving_endpoint VARCHAR(500),
    canary_deployment BOOLEAN DEFAULT false,
    monitoring_config TEXT,
    retrain_policy TEXT,
    last_trained_at TIMESTAMP,
    last_deployed_at TIMESTAMP,
    last_prediction_at TIMESTAMP,
    prediction_count BIGINT DEFAULT 0,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_ml_models_game_id ON ml_models(game_id);
CREATE INDEX idx_ml_models_model_type ON ml_models(model_type);
CREATE INDEX idx_ml_models_model_status ON ml_models(model_status);
CREATE INDEX idx_ml_models_framework ON ml_models(framework);
CREATE INDEX idx_ml_models_created_by ON ml_models(created_by);
CREATE INDEX idx_ml_models_deleted_at ON ml_models(deleted_at);

-- Index for deployed models
CREATE INDEX idx_ml_models_deployed ON ml_models(model_status) WHERE model_status = 'DEPLOYED';

-- Index for A/B test models
CREATE INDEX idx_ml_models_ab_test ON ml_models(is_ab_test) WHERE is_ab_test = true;

-- Index for model name uniqueness
CREATE UNIQUE INDEX idx_ml_models_name_game ON ml_models(model_name, game_id) WHERE deleted_at IS NULL;

-- Model Training Jobs table
CREATE TABLE model_training_jobs (
    id VARCHAR(32) PRIMARY KEY,
    model_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32),
    training_job_name VARCHAR(100) NOT NULL,
    training_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    triggered_by VARCHAR(64),
    trigger_type VARCHAR(50),
    training_config TEXT,
    dataset_config TEXT,
    hyperparameter_config TEXT,
    validation_config TEXT,
    training_epochs INTEGER,
    current_epoch INTEGER DEFAULT 0,
    batch_size INTEGER,
    learning_rate DECIMAL(10,8),
    training_samples BIGINT DEFAULT 0,
    validation_samples BIGINT DEFAULT 0,
    test_samples BIGINT DEFAULT 0,
    training_metrics TEXT,
    validation_metrics TEXT,
    test_metrics TEXT,
    loss_history TEXT,
    best_epoch INTEGER,
    best_loss DECIMAL(20,10),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    gpu_hours DECIMAL(10,4),
    cpu_hours DECIMAL(10,4),
    worker_node VARCHAR(100),
    checkpoint_path VARCHAR(500),
    artifact_path VARCHAR(500),
    error_message TEXT,
    error_stack_trace TEXT,
    progress_percent INTEGER DEFAULT 0,
    eta_minutes INTEGER,
    logs_path VARCHAR(500),
    tensorboard_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES ml_models(id),
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_model_training_jobs_model_id ON model_training_jobs(model_id);
CREATE INDEX idx_model_training_jobs_game_id ON model_training_jobs(game_id);
CREATE INDEX idx_model_training_jobs_training_status ON model_training_jobs(training_status);
CREATE INDEX idx_model_training_jobs_triggered_by ON model_training_jobs(triggered_by);
CREATE INDEX idx_model_training_jobs_created_at ON model_training_jobs(created_at DESC);
CREATE INDEX idx_model_training_jobs_started_at ON model_training_jobs(started_at DESC);

-- Index for running jobs
CREATE INDEX idx_model_training_jobs_running ON model_training_jobs(training_status) WHERE training_status = 'RUNNING';

-- Index for pending jobs
CREATE INDEX idx_model_training_jobs_pending ON model_training_jobs(created_at) WHERE training_status = 'PENDING';

-- ML Model Predictions table
CREATE TABLE ml_model_predictions (
    id VARCHAR(32) PRIMARY KEY,
    model_id VARCHAR(32) NOT NULL,
    model_version INTEGER,
    game_id VARCHAR(32),
    environment VARCHAR(50),
    prediction_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    request_id VARCHAR(64),
    entity_type VARCHAR(50),
    entity_id VARCHAR(128),
    input_data TEXT,
    input_features TEXT,
    output_prediction TEXT,
    prediction_class VARCHAR(100),
    prediction_score DECIMAL(10,6),
    prediction_probability DECIMAL(10,6),
    top_predictions TEXT,
    feature_importance TEXT,
    explanation TEXT,
    latency_ms INTEGER,
    is_ab_test BOOLEAN DEFAULT false,
    ab_test_group VARCHAR(20),
    is_canary BOOLEAN DEFAULT false,
    cache_hit BOOLEAN DEFAULT false,
    feedback_type VARCHAR(30),
    actual_value TEXT,
    feedback_at TIMESTAMP,
    feedback_by VARCHAR(64),
    client_ip VARCHAR(45),
    client_id VARCHAR(64),
    request_source VARCHAR(50),
    batch_id VARCHAR(64),
    metadata TEXT,
    error_message TEXT,
    error_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES ml_models(id),
    FOREIGN KEY (game_id) REFERENCES games(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_ml_model_predictions_model_id ON ml_model_predictions(model_id);
CREATE INDEX idx_ml_model_predictions_game_id ON ml_model_predictions(game_id);
CREATE INDEX idx_ml_model_predictions_prediction_status ON ml_model_predictions(prediction_status);
CREATE INDEX idx_ml_model_predictions_request_id ON ml_model_predictions(request_id);
CREATE INDEX idx_ml_model_predictions_entity_type_id ON ml_model_predictions(entity_type, entity_id);
CREATE INDEX idx_ml_model_predictions_batch_id ON ml_model_predictions(batch_id);
CREATE INDEX idx_ml_model_predictions_client_id ON ml_model_predictions(client_id);
CREATE INDEX idx_ml_model_predictions_created_at ON ml_model_predictions(created_at DESC);
CREATE INDEX idx_ml_model_predictions_completed_at ON ml_model_predictions(completed_at DESC);

-- Index for predictions with feedback
CREATE INDEX idx_ml_model_predictions_feedback ON ml_model_predictions(model_id, feedback_type) WHERE feedback_type IS NOT NULL;

-- Index for A/B test predictions
CREATE INDEX idx_ml_model_predictions_ab_test ON ml_model_predictions(model_id, ab_test_group) WHERE is_ab_test = true;

-- Index for failed predictions
CREATE INDEX idx_ml_model_predictions_failed ON ml_model_predictions(model_id, created_at) WHERE prediction_status = 'FAILED';

-- Index for time-based queries
CREATE INDEX idx_ml_model_predictions_model_time ON ml_model_predictions(model_id, created_at DESC);

-- Model Performance Metrics table (for aggregated metrics)
CREATE TABLE ml_model_metrics (
    id VARCHAR(32) PRIMARY KEY,
    model_id VARCHAR(32) NOT NULL,
    model_version INTEGER,
    metric_date DATE NOT NULL,
    metric_hour INTEGER,
    metric_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,6),
    sample_count BIGINT DEFAULT 0,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES ml_models(id)
);

-- Indexes for metrics
CREATE INDEX idx_ml_model_metrics_model_id ON ml_model_metrics(model_id);
CREATE INDEX idx_ml_model_metrics_date ON ml_model_metrics(metric_date DESC);
CREATE INDEX idx_ml_model_metrics_type ON ml_model_metrics(metric_type);
CREATE INDEX idx_ml_model_metrics_model_date ON ml_model_metrics(model_id, metric_date DESC);

-- Model Versions table (for version history)
CREATE TABLE ml_model_versions (
    id VARCHAR(32) PRIMARY KEY,
    model_id VARCHAR(32) NOT NULL,
    version INTEGER NOT NULL,
    artifact_path VARCHAR(500),
    model_size_bytes BIGINT,
    accuracy_metric DECIMAL(10,6),
    precision_metric DECIMAL(10,6),
    recall_metric DECIMAL(10,6),
    f1_score DECIMAL(10,6),
    auc_score DECIMAL(10,6),
    training_job_id VARCHAR(32),
    deployed_at TIMESTAMP,
    deployed_by VARCHAR(64),
    archived_at TIMESTAMP,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES ml_models(id)
);

-- Indexes for versions
CREATE INDEX idx_ml_model_versions_model_id ON ml_model_versions(model_id);
CREATE INDEX idx_ml_model_versions_version ON ml_model_versions(model_id, version DESC);
CREATE UNIQUE INDEX idx_ml_model_versions_unique ON ml_model_versions(model_id, version);

-- Comments for documentation
COMMENT ON TABLE ml_models IS '机器学习模型定义和元数据';
COMMENT ON TABLE model_training_jobs IS '模型训练任务跟踪';
COMMENT ON TABLE ml_model_predictions IS '模型预测请求和结果记录';
COMMENT ON TABLE ml_model_metrics IS '模型性能指标聚合';
COMMENT ON TABLE ml_model_versions IS '模型版本历史';
