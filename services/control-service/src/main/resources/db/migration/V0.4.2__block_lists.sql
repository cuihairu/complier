-- Block Lists Table
-- Phase 4: Gateway Hard Blocking - Block Lists for enforced blocking at gateway layer

-- Block Lists table
CREATE TABLE block_lists (
    id VARCHAR(32) PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    environment_id VARCHAR(32),
    risk_case_id VARCHAR(32),
    target_type VARCHAR(50) NOT NULL,
    target_value VARCHAR(500) NOT NULL,
    target_name VARCHAR(200),
    block_reason VARCHAR(500),
    risk_level VARCHAR(20),
    block_category VARCHAR(50),
    block_type VARCHAR(20) NOT NULL DEFAULT 'HARD',
    is_permanent BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP,
    block_scope VARCHAR(50),
    blocked_by VARCHAR(64),
    blocked_at TIMESTAMP,
    unblocked_by VARCHAR(64),
    unblocked_at TIMESTAMP,
    unblock_reason VARCHAR(500),
    hit_count BIGINT DEFAULT 0,
    last_hit_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id),
    FOREIGN KEY (environment_id) REFERENCES game_environments(id),
    FOREIGN KEY (risk_case_id) REFERENCES risk_cases(id)
);

-- Indexes for efficient lookups
CREATE INDEX idx_block_lists_game_id ON block_lists(game_id);
CREATE INDEX idx_block_lists_environment_id ON block_lists(environment_id);
CREATE INDEX idx_block_lists_risk_case_id ON block_lists(risk_case_id);
CREATE INDEX idx_block_lists_target ON block_lists(target_type, target_value);
CREATE INDEX idx_block_lists_target_type ON block_lists(target_type);
CREATE INDEX idx_block_lists_block_type ON block_lists(block_type);
CREATE INDEX idx_block_lists_block_category ON block_lists(block_category);
CREATE INDEX idx_block_lists_expires_at ON block_lists(expires_at);
CREATE INDEX idx_block_lists_created_at ON block_lists(created_at DESC);

-- Index for active blocks (most common query)
CREATE INDEX idx_block_lists_active ON block_lists(game_id, deleted_at, unblocked_at, is_permanent, expires_at);

-- Composite index for IP lookups
CREATE INDEX idx_block_lists_ip ON block_lists(target_type, target_value, deleted_at, unblocked_at, expires_at) WHERE target_type = 'ip';

-- Index for shadow blocks
CREATE INDEX idx_block_lists_shadow ON block_lists(game_id, block_type, deleted_at, unblocked_at) WHERE block_type = 'SHADOW' AND deleted_at IS NULL AND unblocked_at IS NULL;

-- Insert example block entries for reference
INSERT INTO block_lists (id, game_id, target_type, target_value, block_reason, block_category, block_type, is_permanent, blocked_by) VALUES
('bl_example_ip', 'DEFAULT', 'ip', '192.168.1.100', 'Example IP block', 'security', 'HARD', FALSE, 'system'),
('bl_example_device', 'DEFAULT', 'device_id', 'device_cheat_001', 'Example device block for cheating', 'cheating', 'HARD', TRUE, 'system'),
('bl_example_user', 'DEFAULT', 'user_id', 'user_fraud_001', 'Example user block for fraud', 'fraud', 'SOFT', FALSE, 'system');
