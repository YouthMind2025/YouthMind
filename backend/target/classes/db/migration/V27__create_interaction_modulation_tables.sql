-- V27: Create user_interaction_modulations table to track β and μ parameters for each interaction type
-- This migration creates tables for tracking interaction modulation parameters used in the psychological state system

-- Table for tracking user interaction modulation parameters
CREATE TABLE user_interaction_modulations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    interaction_type VARCHAR(50) NOT NULL CHECK (interaction_type IN ('RESONANCE', 'LINGER', 'VIEW', 'LIKE', 'COMMENT', 'SHARE')),
    beta_factor DECIMAL(8,6) NOT NULL DEFAULT 0.1, -- β factor (user state amplification)
    mu_factor DECIMAL(8,6) NOT NULL DEFAULT 0.05, -- μ factor (positive balance amplification)
    ws_weight DECIMAL(8,6) NOT NULL, -- Ws weight (interaction influence weight)
    lower_bound DECIMAL(8,6) NOT NULL DEFAULT -10.0, -- Lower bound for clipping
    upper_bound DECIMAL(8,6) NOT NULL DEFAULT 10.0, -- Upper bound for clipping
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_interaction_modulations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_beta_factor_range CHECK (beta_factor >= 0 AND beta_factor <= 1),
    CONSTRAINT chk_mu_factor_range CHECK (mu_factor >= 0 AND mu_factor <= 1),
    CONSTRAINT chk_ws_weight_range CHECK (ws_weight >= 0 AND ws_weight <= 1),
    CONSTRAINT chk_bounds_order CHECK (lower_bound < upper_bound),
    CONSTRAINT uk_user_interaction_type UNIQUE (user_id, interaction_type)
);

-- Table for system-wide interaction type configurations
CREATE TABLE interaction_type_configs (
    id SERIAL PRIMARY KEY,
    interaction_type VARCHAR(50) NOT NULL UNIQUE,
    default_beta DECIMAL(8,6) NOT NULL,
    default_mu DECIMAL(8,6) NOT NULL,
    default_ws_weight DECIMAL(8,6) NOT NULL,
    default_lower_bound DECIMAL(8,6) NOT NULL,
    default_upper_bound DECIMAL(8,6) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_default_beta_range CHECK (default_beta >= 0 AND default_beta <= 1),
    CONSTRAINT chk_default_mu_range CHECK (default_mu >= 0 AND default_mu <= 1),
    CONSTRAINT chk_default_ws_range CHECK (default_ws_weight >= 0 AND default_ws_weight <= 1),
    CONSTRAINT chk_default_bounds_order CHECK (default_lower_bound < default_upper_bound)
);

-- Table for tracking interaction modulation history
CREATE TABLE interaction_modulation_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,
    old_beta DECIMAL(8,6),
    new_beta DECIMAL(8,6) NOT NULL,
    old_mu DECIMAL(8,6),
    new_mu DECIMAL(8,6) NOT NULL,
    old_ws_weight DECIMAL(8,6),
    new_ws_weight DECIMAL(8,6) NOT NULL,
    change_reason VARCHAR(100), -- Reason for parameter change
    changed_by UUID, -- User who made the change (NULL for system changes)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_modulation_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_modulation_history_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Insert default interaction type configurations based on the explanation model
INSERT INTO interaction_type_configs (interaction_type, default_beta, default_mu, default_ws_weight, default_lower_bound, default_upper_bound, description) VALUES
('RESONANCE', 0.15, 0.08, 0.20, -10.0, 10.0, 'High-impact interaction: user resonates with content'),
('LINGER', 0.10, 0.05, 0.01, -10.0, 10.0, 'Medium-impact interaction: user spends time viewing content'),
('VIEW', 0.05, 0.02, 0.005, -10.0, 10.0, 'Low-impact interaction: user briefly views content'),
('LIKE', 0.12, 0.06, 0.15, -10.0, 10.0, 'Medium-high impact: user likes content'),
('COMMENT', 0.18, 0.10, 0.25, -10.0, 10.0, 'High impact: user actively engages by commenting'),
('SHARE', 0.20, 0.12, 0.30, -10.0, 10.0, 'Highest impact: user shares content with others');

-- Create indexes for performance
CREATE INDEX idx_interaction_modulations_user_id ON user_interaction_modulations(user_id);
CREATE INDEX idx_interaction_modulations_type ON user_interaction_modulations(interaction_type);
CREATE INDEX idx_interaction_modulations_active ON user_interaction_modulations(is_active);
CREATE INDEX idx_interaction_modulations_updated_at ON user_interaction_modulations(updated_at);

CREATE INDEX idx_interaction_configs_type ON interaction_type_configs(interaction_type);
CREATE INDEX idx_interaction_configs_active ON interaction_type_configs(is_active);

CREATE INDEX idx_modulation_history_user_id ON interaction_modulation_history(user_id);
CREATE INDEX idx_modulation_history_type ON interaction_modulation_history(interaction_type);
CREATE INDEX idx_modulation_history_created_at ON interaction_modulation_history(created_at);
CREATE INDEX idx_modulation_history_changed_by ON interaction_modulation_history(changed_by);

-- Function to initialize user interaction modulations with defaults
CREATE OR REPLACE FUNCTION initialize_user_interaction_modulations(p_user_id UUID)
RETURNS VOID AS $$
DECLARE
    config_record RECORD;
BEGIN
    -- Insert default modulation parameters for all interaction types
    FOR config_record IN 
        SELECT * FROM interaction_type_configs WHERE is_active = TRUE
    LOOP
        INSERT INTO user_interaction_modulations (
            user_id, 
            interaction_type, 
            beta_factor, 
            mu_factor, 
            ws_weight, 
            lower_bound, 
            upper_bound
        ) VALUES (
            p_user_id,
            config_record.interaction_type,
            config_record.default_beta,
            config_record.default_mu,
            config_record.default_ws_weight,
            config_record.default_lower_bound,
            config_record.default_upper_bound
        ) ON CONFLICT (user_id, interaction_type) DO NOTHING;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to update interaction modulation parameters
CREATE OR REPLACE FUNCTION update_interaction_modulation(
    p_user_id UUID,
    p_interaction_type VARCHAR(50),
    p_new_beta DECIMAL(8,6),
    p_new_mu DECIMAL(8,6),
    p_new_ws_weight DECIMAL(8,6),
    p_change_reason VARCHAR(100) DEFAULT 'System update',
    p_changed_by UUID DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    old_record RECORD;
BEGIN
    -- Get current values for history
    SELECT beta_factor, mu_factor, ws_weight 
    INTO old_record
    FROM user_interaction_modulations 
    WHERE user_id = p_user_id AND interaction_type = p_interaction_type;
    
    -- Update the modulation parameters
    UPDATE user_interaction_modulations 
    SET 
        beta_factor = p_new_beta,
        mu_factor = p_new_mu,
        ws_weight = p_new_ws_weight,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_user_id AND interaction_type = p_interaction_type;
    
    -- Record the change in history
    INSERT INTO interaction_modulation_history (
        user_id, interaction_type,
        old_beta, new_beta,
        old_mu, new_mu,
        old_ws_weight, new_ws_weight,
        change_reason, changed_by
    ) VALUES (
        p_user_id, p_interaction_type,
        old_record.beta_factor, p_new_beta,
        old_record.mu_factor, p_new_mu,
        old_record.ws_weight, p_new_ws_weight,
        p_change_reason, p_changed_by
    );
END;
$$ LANGUAGE plpgsql;

-- Comments for documentation
COMMENT ON TABLE user_interaction_modulations IS 'User-specific interaction modulation parameters (β, μ, Ws) for psychological state updates';
COMMENT ON COLUMN user_interaction_modulations.beta_factor IS 'β factor: amplification of user current state in interaction modulation';
COMMENT ON COLUMN user_interaction_modulations.mu_factor IS 'μ factor: amplification of positive balance vector in interaction modulation';
COMMENT ON COLUMN user_interaction_modulations.ws_weight IS 'Ws weight: influence weight of interaction on user state update';
COMMENT ON COLUMN user_interaction_modulations.lower_bound IS 'Lower bound for clipping modulated vectors to prevent explosion';
COMMENT ON COLUMN user_interaction_modulations.upper_bound IS 'Upper bound for clipping modulated vectors to prevent explosion';

COMMENT ON TABLE interaction_type_configs IS 'System-wide default configurations for different interaction types';
COMMENT ON TABLE interaction_modulation_history IS 'History of changes to user interaction modulation parameters for audit trail';

COMMENT ON FUNCTION initialize_user_interaction_modulations IS 'Initialize default interaction modulation parameters for a new user';
COMMENT ON FUNCTION update_interaction_modulation IS 'Update user interaction modulation parameters and record change history';
