-- Cohorts Table
-- Phase 5: Cohort Analysis - User behavior cohort tracking and retention analysis

-- Cohorts table
CREATE TABLE cohorts (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
    description TEXT,
    cohort_type VARCHAR(20) NOT NULL DEFAULT 'ACQUISITION',
    start_date DATE,
    end_date DATE,
    time_unit VARCHAR(20) DEFAULT 'day',
    cohort_size INTEGER DEFAULT 1,
    behavior_definition TEXT,
    inclusion_criteria TEXT,
    exclusion_criteria TEXT,
    analysis_type VARCHAR(50) DEFAULT 'retention',
    metric_type VARCHAR(50) DEFAULT 'return_rate',
    retention_periods TEXT,
    comparison_cohorts TEXT,
    cohort_count BIGINT DEFAULT 0,
    result_data TEXT,
    result_summary TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    calculated_at TIMESTAMP,
    total_calculations BIGINT DEFAULT 0,
    last_calculation_time_ms BIGINT,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_cohorts_game_id ON cohorts(game_id);
CREATE INDEX idx_cohorts_environment_id ON cohorts(environment_id);
CREATE INDEX idx_cohorts_cohort_type ON cohorts(cohort_type);
CREATE INDEX idx_cohorts_analysis_type ON cohorts(analysis_type);
CREATE INDEX idx_cohorts_status ON cohorts(status);
CREATE INDEX idx_cohorts_start_date ON cohorts(start_date);
CREATE INDEX idx_cohorts_calculated_at ON cohorts(calculated_at DESC);

-- Unique constraint on game + name
CREATE UNIQUE INDEX idx_cohorts_name ON cohorts(game_id, name) WHERE deleted_at IS NULL;

-- Index for pending cohorts
CREATE INDEX idx_cohorts_pending ON cohorts(status) WHERE status = 'PENDING';

-- Insert example cohorts
INSERT INTO cohorts (id, game_id, name, display_name, description, cohort_type, start_date, end_date, analysis_type, retention_periods, status) VALUES
('ch_daily_acquisition', 'DEFAULT', 'daily_acquisition_cohort', 'Daily Acquisition Cohort', 'Users acquired by day', 'ACQUISITION', CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE, 'retention', '[1,7,14,30,60,90]', 'COMPLETED'),
('ch_first_purchase', 'DEFAULT', 'first_purchase_cohort', 'First Purchase Cohort', 'Users who made their first purchase', 'BEHAVIORAL', CURRENT_DATE - INTERVAL '60 days', CURRENT_DATE, 'retention', '[1,7,30,90]', 'PENDING'),
('ch_weekly_active', 'DEFAULT', 'weekly_active_cohort', 'Weekly Active Cohort', 'Weekly active users', 'ACQUISITION', CURRENT_DATE - INTERVAL '12 weeks', CURRENT_DATE, 'engagement', '[1,2,4,8,12]', 'PENDING');
