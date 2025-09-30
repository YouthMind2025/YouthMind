-- V28: Create personality_computations table for dual personality system
-- This migration creates tables for tracking personality computation results and frequency

-- Table for personality computation results
CREATE TABLE personality_computations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    computation_period VARCHAR(20) NOT NULL CHECK (computation_period IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    computation_date DATE NOT NULL,
    
    -- Active personality (μ_post) - from user's own posts
    active_personality_vector JSONB NOT NULL, -- 43-dimensional vector
    active_personality_confidence DECIMAL(5,4) NOT NULL, -- Confidence in active personality
    active_posts_count INTEGER NOT NULL DEFAULT 0, -- Number of posts used in computation
    active_posts_weighted_sum DECIMAL(12,6) NOT NULL DEFAULT 0, -- Sum of θ weights
    
    -- Subconscious personality (V_shadow) - from interaction patterns
    subconscious_personality_vector JSONB NOT NULL, -- 43-dimensional vector
    subconscious_personality_confidence DECIMAL(5,4) NOT NULL, -- Confidence in subconscious personality
    subconscious_interactions_count INTEGER NOT NULL DEFAULT 0, -- Number of interactions used
    top_k_dimensions INTEGER NOT NULL DEFAULT 10, -- Number of top dimensions used
    resonance_dimensions JSONB, -- Array of dimension indices with high resonance
    
    -- Final personality (μ) - combined personality
    final_personality_vector JSONB NOT NULL, -- 43-dimensional vector
    lambda1 DECIMAL(5,4) NOT NULL DEFAULT 0.7, -- Weight for active personality
    lambda2 DECIMAL(5,4) NOT NULL DEFAULT 0.3, -- Weight for subconscious personality
    final_confidence DECIMAL(5,4) NOT NULL, -- Overall confidence in final personality
    
    -- Computation metadata
    computation_duration_ms INTEGER, -- Time taken for computation
    posts_analyzed JSONB, -- Array of post IDs analyzed
    interactions_analyzed JSONB, -- Array of interaction IDs analyzed
    computation_parameters JSONB, -- Additional computation parameters
    computation_version VARCHAR(20) DEFAULT '1.0', -- Version of computation algorithm
    
    -- Status and validation
    is_valid BOOLEAN DEFAULT TRUE, -- Whether computation is valid
    validation_errors TEXT[], -- Array of validation error messages
    needs_recomputation BOOLEAN DEFAULT FALSE, -- Whether recomputation is needed
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_personality_computations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_personality_vectors_format CHECK (
        jsonb_array_length(active_personality_vector) = 43 AND
        jsonb_array_length(subconscious_personality_vector) = 43 AND
        jsonb_array_length(final_personality_vector) = 43
    ),
    CONSTRAINT chk_confidence_ranges CHECK (
        active_personality_confidence >= 0 AND active_personality_confidence <= 1 AND
        subconscious_personality_confidence >= 0 AND subconscious_personality_confidence <= 1 AND
        final_confidence >= 0 AND final_confidence <= 1
    ),
    CONSTRAINT chk_lambda_weights CHECK (
        lambda1 >= 0 AND lambda1 <= 1 AND
        lambda2 >= 0 AND lambda2 <= 1 AND
        ABS(lambda1 + lambda2 - 1.0) < 0.001 -- Should sum to approximately 1
    ),
    CONSTRAINT chk_counts_positive CHECK (
        active_posts_count >= 0 AND
        subconscious_interactions_count >= 0 AND
        top_k_dimensions > 0
    ),
    CONSTRAINT uk_user_period_date UNIQUE (user_id, computation_period, computation_date)
);

-- Table for personality computation schedules
CREATE TABLE personality_computation_schedules (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    computation_frequency VARCHAR(20) NOT NULL CHECK (computation_frequency IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    next_computation_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_computation_id UUID, -- Reference to last successful computation
    computation_settings JSONB, -- User-specific computation settings
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_personality_schedules_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_personality_schedules_last_computation FOREIGN KEY (last_computation_id) REFERENCES personality_computations(id) ON DELETE SET NULL,
    CONSTRAINT uk_user_frequency UNIQUE (user_id, computation_frequency)
);

-- Table for personality computation history and trends
CREATE TABLE personality_computation_trends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    computation_period VARCHAR(20) NOT NULL,
    trend_type VARCHAR(50) NOT NULL CHECK (trend_type IN ('ACTIVE_PERSONALITY', 'SUBCONSCIOUS_PERSONALITY', 'FINAL_PERSONALITY', 'DIMENSION_CHANGE', 'CONFIDENCE_CHANGE')),
    dimension_index INTEGER CHECK (dimension_index >= 0 AND dimension_index <= 42), -- For dimension-specific trends
    previous_value DECIMAL(12,6),
    current_value DECIMAL(12,6) NOT NULL,
    change_magnitude DECIMAL(12,6) NOT NULL, -- Absolute change
    change_percentage DECIMAL(8,4), -- Percentage change
    trend_direction VARCHAR(20) CHECK (trend_direction IN ('INCREASING', 'DECREASING', 'STABLE')),
    significance_level DECIMAL(5,4), -- Statistical significance of the change
    computation_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_personality_trends_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_personality_trends_computation FOREIGN KEY (computation_id) REFERENCES personality_computations(id) ON DELETE CASCADE,
    CONSTRAINT chk_dimension_index CHECK (
        (trend_type IN ('DIMENSION_CHANGE') AND dimension_index IS NOT NULL) OR
        (trend_type NOT IN ('DIMENSION_CHANGE') AND dimension_index IS NULL)
    ),
    CONSTRAINT chk_significance_range CHECK (significance_level >= 0 AND significance_level <= 1)
);

-- Table for personality mapping to standard models
CREATE TABLE personality_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    computation_id UUID NOT NULL,
    mapping_type VARCHAR(50) NOT NULL CHECK (mapping_type IN ('BIG_FIVE', 'HEXACO', 'MBTI', 'CUSTOM')),
    mapping_scores JSONB NOT NULL, -- Mapping scores for the personality model
    mapping_confidence DECIMAL(5,4) NOT NULL,
    mapping_metadata JSONB, -- Additional mapping information
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_personality_mappings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_personality_mappings_computation FOREIGN KEY (computation_id) REFERENCES personality_computations(id) ON DELETE CASCADE,
    CONSTRAINT chk_mapping_confidence CHECK (mapping_confidence >= 0 AND mapping_confidence <= 1),
    CONSTRAINT uk_user_computation_mapping UNIQUE (user_id, computation_id, mapping_type)
);

-- Create indexes for performance
CREATE INDEX idx_personality_computations_user_id ON personality_computations(user_id);
CREATE INDEX idx_personality_computations_period ON personality_computations(computation_period);
CREATE INDEX idx_personality_computations_date ON personality_computations(computation_date);
CREATE INDEX idx_personality_computations_valid ON personality_computations(is_valid);
CREATE INDEX idx_personality_computations_recomputation ON personality_computations(needs_recomputation);
CREATE INDEX idx_personality_computations_created_at ON personality_computations(created_at);

CREATE INDEX idx_personality_schedules_user_id ON personality_computation_schedules(user_id);
CREATE INDEX idx_personality_schedules_frequency ON personality_computation_schedules(computation_frequency);
CREATE INDEX idx_personality_schedules_next_date ON personality_computation_schedules(next_computation_date);
CREATE INDEX idx_personality_schedules_active ON personality_computation_schedules(is_active);

CREATE INDEX idx_personality_trends_user_id ON personality_computation_trends(user_id);
CREATE INDEX idx_personality_trends_type ON personality_computation_trends(trend_type);
CREATE INDEX idx_personality_trends_dimension ON personality_computation_trends(dimension_index);
CREATE INDEX idx_personality_trends_computation ON personality_computation_trends(computation_id);
CREATE INDEX idx_personality_trends_created_at ON personality_computation_trends(created_at);

CREATE INDEX idx_personality_mappings_user_id ON personality_mappings(user_id);
CREATE INDEX idx_personality_mappings_type ON personality_mappings(mapping_type);
CREATE INDEX idx_personality_mappings_computation ON personality_mappings(computation_id);

-- Function to create a new personality computation
CREATE OR REPLACE FUNCTION create_personality_computation(
    p_user_id UUID,
    p_computation_period VARCHAR(20),
    p_computation_date DATE,
    p_active_vector JSONB,
    p_subconscious_vector JSONB,
    p_final_vector JSONB,
    p_metadata JSONB DEFAULT '{}'::jsonb
)
RETURNS UUID AS $$
DECLARE
    computation_id UUID;
    active_confidence DECIMAL(5,4);
    subconscious_confidence DECIMAL(5,4);
    final_confidence DECIMAL(5,4);
BEGIN
    -- Calculate confidence scores based on vector quality
    active_confidence := LEAST(0.9, GREATEST(0.1, (p_metadata->>'active_posts_count')::INTEGER / 10.0));
    subconscious_confidence := LEAST(0.9, GREATEST(0.1, (p_metadata->>'subconscious_interactions_count')::INTEGER / 20.0));
    final_confidence := (active_confidence + subconscious_confidence) / 2.0;
    
    -- Insert the computation
    INSERT INTO personality_computations (
        user_id, computation_period, computation_date,
        active_personality_vector, active_personality_confidence,
        subconscious_personality_vector, subconscious_personality_confidence,
        final_personality_vector, final_confidence,
        active_posts_count, subconscious_interactions_count,
        computation_parameters
    ) VALUES (
        p_user_id, p_computation_period, p_computation_date,
        p_active_vector, active_confidence,
        p_subconscious_vector, subconscious_confidence,
        p_final_vector, final_confidence,
        COALESCE((p_metadata->>'active_posts_count')::INTEGER, 0),
        COALESCE((p_metadata->>'subconscious_interactions_count')::INTEGER, 0),
        p_metadata
    ) RETURNING id INTO computation_id;
    
    RETURN computation_id;
END;
$$ LANGUAGE plpgsql;

-- Function to get latest personality computation for a user
CREATE OR REPLACE FUNCTION get_latest_personality_computation(
    p_user_id UUID,
    p_computation_period VARCHAR(20) DEFAULT 'MONTHLY'
)
RETURNS TABLE (
    computation_id UUID,
    active_personality_vector JSONB,
    subconscious_personality_vector JSONB,
    final_personality_vector JSONB,
    computation_date DATE,
    final_confidence DECIMAL(5,4)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pc.id,
        pc.active_personality_vector,
        pc.subconscious_personality_vector,
        pc.final_personality_vector,
        pc.computation_date,
        pc.final_confidence
    FROM personality_computations pc
    WHERE pc.user_id = p_user_id 
        AND pc.computation_period = p_computation_period
        AND pc.is_valid = TRUE
    ORDER BY pc.computation_date DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- Comments for documentation
COMMENT ON TABLE personality_computations IS 'Results of personality computations using dual personality system (active + subconscious)';
COMMENT ON COLUMN personality_computations.active_personality_vector IS 'μ_post: 43-dimensional vector from user posts (explicit personality)';
COMMENT ON COLUMN personality_computations.subconscious_personality_vector IS 'V_shadow: 43-dimensional vector from interactions (implicit personality)';
COMMENT ON COLUMN personality_computations.final_personality_vector IS 'μ: Combined personality vector (λ1*μ_post + λ2*V_shadow)';
COMMENT ON COLUMN personality_computations.lambda1 IS 'Weight for active personality in final computation (default 0.7)';
COMMENT ON COLUMN personality_computations.lambda2 IS 'Weight for subconscious personality in final computation (default 0.3)';
COMMENT ON COLUMN personality_computations.resonance_dimensions IS 'Array of dimension indices with high resonance in interactions';

COMMENT ON TABLE personality_computation_schedules IS 'Schedules for periodic personality computations per user';
COMMENT ON TABLE personality_computation_trends IS 'Analysis of personality changes over time and their significance';
COMMENT ON TABLE personality_mappings IS 'Mappings of computed personality vectors to standard personality models';

COMMENT ON FUNCTION create_personality_computation IS 'Create a new personality computation with automatic confidence calculation';
COMMENT ON FUNCTION get_latest_personality_computation IS 'Get the most recent valid personality computation for a user';
