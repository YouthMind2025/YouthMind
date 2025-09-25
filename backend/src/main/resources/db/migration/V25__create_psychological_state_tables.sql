-- V25: Create psychological state tables for the 43-dimensional Buddhist mental factors system
-- This migration creates the core tables for tracking user psychological states and vectors

-- Table for storing 43-dimensional psychological state vectors
CREATE TABLE psychological_state_vectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    vector_data JSONB NOT NULL, -- 43-dimensional vector as JSON array
    vector_type VARCHAR(50) NOT NULL CHECK (vector_type IN ('V_raw', 'S_current', 'R_raw', 'B_balance')),
    intensity_factor DECIMAL(5,4) CHECK (intensity_factor >= 0 AND intensity_factor <= 1), -- θ factor
    source_type VARCHAR(50) NOT NULL CHECK (source_type IN ('POST', 'INTERACTION', 'SYSTEM', 'COMPUTED')),
    source_id UUID, -- Reference to post, interaction, or other source
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_psychological_vectors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_vector_data_format CHECK (jsonb_array_length(vector_data) = 43),
    CONSTRAINT chk_intensity_factor_required CHECK (
        (vector_type = 'V_raw' AND intensity_factor IS NOT NULL) OR 
        (vector_type != 'V_raw' AND intensity_factor IS NULL)
    )
);

-- Table for tracking user's current psychological state
CREATE TABLE user_psychological_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    current_state_vector JSONB NOT NULL, -- Current S vector (43 dimensions)
    base_alpha DECIMAL(5,4) NOT NULL DEFAULT 0.1, -- Base learning rate
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    update_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_psychological_states_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_state_vector_format CHECK (jsonb_array_length(current_state_vector) = 43),
    CONSTRAINT chk_base_alpha_range CHECK (base_alpha >= 0 AND base_alpha <= 1)
);

-- Table for tracking state update history
CREATE TABLE state_update_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    update_type VARCHAR(50) NOT NULL CHECK (update_type IN ('POSTING', 'INTERACTION', 'PERSONALITY_COMPUTATION', 'SYSTEM_UPDATE')),
    previous_state JSONB, -- Previous S vector
    new_state JSONB NOT NULL, -- New S vector after update
    alpha_used DECIMAL(5,4), -- α value used in update
    theta_factor DECIMAL(5,4), -- θ factor (for posting updates)
    beta_factor DECIMAL(5,4), -- β factor (for interaction updates)
    mu_factor DECIMAL(5,4), -- μ factor (for interaction updates)
    ws_weight DECIMAL(5,4), -- Ws weight (for interaction updates)
    source_id UUID, -- Reference to post, interaction, or computation
    computation_metadata JSONB, -- Additional metadata about the computation
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_state_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_state_vectors_format CHECK (
        (previous_state IS NULL OR jsonb_array_length(previous_state) = 43) AND
        jsonb_array_length(new_state) = 43
    ),
    CONSTRAINT chk_posting_update_params CHECK (
        (update_type = 'POSTING' AND alpha_used IS NOT NULL AND theta_factor IS NOT NULL) OR
        (update_type != 'POSTING')
    ),
    CONSTRAINT chk_interaction_update_params CHECK (
        (update_type = 'INTERACTION' AND beta_factor IS NOT NULL AND mu_factor IS NOT NULL AND ws_weight IS NOT NULL) OR
        (update_type != 'INTERACTION')
    )
);

-- Indexes for performance
CREATE INDEX idx_psychological_vectors_user_id ON psychological_state_vectors(user_id);
CREATE INDEX idx_psychological_vectors_type ON psychological_state_vectors(vector_type);
CREATE INDEX idx_psychological_vectors_source ON psychological_state_vectors(source_type, source_id);
CREATE INDEX idx_psychological_vectors_created_at ON psychological_state_vectors(created_at);

CREATE INDEX idx_user_psychological_states_user_id ON user_psychological_states(user_id);
CREATE INDEX idx_user_psychological_states_updated_at ON user_psychological_states(last_updated_at);

CREATE INDEX idx_state_history_user_id ON state_update_history(user_id);
CREATE INDEX idx_state_history_type ON state_update_history(update_type);
CREATE INDEX idx_state_history_created_at ON state_update_history(created_at);
CREATE INDEX idx_state_history_source ON state_update_history(source_id);

-- Comments for documentation
COMMENT ON TABLE psychological_state_vectors IS 'Stores 43-dimensional psychological state vectors for users, posts, and interactions';
COMMENT ON COLUMN psychological_state_vectors.vector_data IS '43-dimensional vector representing Buddhist mental factors as JSON array';
COMMENT ON COLUMN psychological_state_vectors.intensity_factor IS 'θ factor indicating psychological impact intensity (0-1)';
COMMENT ON COLUMN psychological_state_vectors.vector_type IS 'Type of vector: V_raw (from posts), S_current (user state), R_raw (interaction), B_balance (system)';

COMMENT ON TABLE user_psychological_states IS 'Current psychological state for each user, updated in real-time';
COMMENT ON COLUMN user_psychological_states.current_state_vector IS 'Current 43-dimensional S vector representing user psychological state';
COMMENT ON COLUMN user_psychological_states.base_alpha IS 'Base learning rate for state updates (0-1)';

COMMENT ON TABLE state_update_history IS 'Complete history of psychological state updates for audit and analysis';
COMMENT ON COLUMN state_update_history.alpha_used IS 'α value used in posting update: base_alpha + theta_factor';
COMMENT ON COLUMN state_update_history.theta_factor IS 'θ factor from posting (psychological impact intensity)';
COMMENT ON COLUMN state_update_history.beta_factor IS 'β factor for interaction modulation (user state amplification)';
COMMENT ON COLUMN state_update_history.mu_factor IS 'μ factor for interaction modulation (positive balance amplification)';
COMMENT ON COLUMN state_update_history.ws_weight IS 'Ws weight for interaction fusion (varies by interaction type)';
