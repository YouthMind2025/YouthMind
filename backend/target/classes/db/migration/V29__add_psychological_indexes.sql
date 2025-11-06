-- V29: Add performance indexes on user_id, created_at, interaction_type, and psychological state dimensions
-- This migration adds comprehensive indexes for optimal performance of the psychological state system

-- Additional indexes for psychological_state_vectors table
CREATE INDEX CONCURRENTLY idx_psychological_vectors_user_type ON psychological_state_vectors(user_id, vector_type);
CREATE INDEX CONCURRENTLY idx_psychological_vectors_source_type_id ON psychological_state_vectors(source_type, source_id);
CREATE INDEX CONCURRENTLY idx_psychological_vectors_intensity ON psychological_state_vectors(intensity_factor) WHERE intensity_factor IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_psychological_vectors_created_at_desc ON psychological_state_vectors(created_at DESC);

-- GIN indexes for JSONB vector data queries
CREATE INDEX CONCURRENTLY idx_psychological_vectors_data_gin ON psychological_state_vectors USING GIN (vector_data);
CREATE INDEX CONCURRENTLY idx_user_psychological_states_vector_gin ON user_psychological_states USING GIN (current_state_vector);

-- Additional indexes for user_psychological_states table
CREATE INDEX CONCURRENTLY idx_user_psychological_states_updated_count ON user_psychological_states(update_count);
CREATE INDEX CONCURRENTLY idx_user_psychological_states_base_alpha ON user_psychological_states(base_alpha);

-- Additional indexes for state_update_history table
CREATE INDEX CONCURRENTLY idx_state_history_user_type ON state_update_history(user_id, update_type);
CREATE INDEX CONCURRENTLY idx_state_history_user_created ON state_update_history(user_id, created_at DESC);
CREATE INDEX CONCURRENTLY idx_state_history_source_type ON state_update_history(update_type, source_id);
CREATE INDEX CONCURRENTLY idx_state_history_alpha_range ON state_update_history(alpha_used) WHERE alpha_used IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_state_history_theta_range ON state_update_history(theta_factor) WHERE theta_factor IS NOT NULL;

-- GIN indexes for JSONB state vectors in history
CREATE INDEX CONCURRENTLY idx_state_history_previous_state_gin ON state_update_history USING GIN (previous_state) WHERE previous_state IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_state_history_new_state_gin ON state_update_history USING GIN (new_state);
CREATE INDEX CONCURRENTLY idx_state_history_metadata_gin ON state_update_history USING GIN (computation_metadata) WHERE computation_metadata IS NOT NULL;

-- Additional indexes for buddhist_psychology_factors table
CREATE INDEX CONCURRENTLY idx_buddhist_factors_subcategory ON buddhist_psychology_factors(subcategory);
CREATE INDEX CONCURRENTLY idx_buddhist_factors_core_secondary ON buddhist_psychology_factors(is_core_factor, is_secondary_affliction);
CREATE INDEX CONCURRENTLY idx_buddhist_factors_polarity_category ON buddhist_psychology_factors(polarity, category);

-- Additional indexes for user_interaction_modulations table
CREATE INDEX CONCURRENTLY idx_interaction_modulations_user_active ON user_interaction_modulations(user_id, is_active);
CREATE INDEX CONCURRENTLY idx_interaction_modulations_type_active ON user_interaction_modulations(interaction_type, is_active);
CREATE INDEX CONCURRENTLY idx_interaction_modulations_beta_range ON user_interaction_modulations(beta_factor);
CREATE INDEX CONCURRENTLY idx_interaction_modulations_mu_range ON user_interaction_modulations(mu_factor);
CREATE INDEX CONCURRENTLY idx_interaction_modulations_ws_range ON user_interaction_modulations(ws_weight);

-- Additional indexes for interaction_modulation_history table
CREATE INDEX CONCURRENTLY idx_modulation_history_user_type ON interaction_modulation_history(user_id, interaction_type);
CREATE INDEX CONCURRENTLY idx_modulation_history_changed_by_date ON interaction_modulation_history(changed_by, created_at DESC) WHERE changed_by IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_modulation_history_reason ON interaction_modulation_history(change_reason) WHERE change_reason IS NOT NULL;

-- Additional indexes for personality_computations table
CREATE INDEX CONCURRENTLY idx_personality_computations_user_period ON personality_computations(user_id, computation_period);
CREATE INDEX CONCURRENTLY idx_personality_computations_user_date ON personality_computations(user_id, computation_date DESC);
CREATE INDEX CONCURRENTLY idx_personality_computations_period_date ON personality_computations(computation_period, computation_date DESC);
CREATE INDEX CONCURRENTLY idx_personality_computations_valid_recomputation ON personality_computations(is_valid, needs_recomputation);
CREATE INDEX CONCURRENTLY idx_personality_computations_confidence ON personality_computations(final_confidence);
CREATE INDEX CONCURRENTLY idx_personality_computations_posts_count ON personality_computations(active_posts_count);
CREATE INDEX CONCURRENTLY idx_personality_computations_interactions_count ON personality_computations(subconscious_interactions_count);

-- GIN indexes for JSONB personality vectors
CREATE INDEX CONCURRENTLY idx_personality_computations_active_vector_gin ON personality_computations USING GIN (active_personality_vector);
CREATE INDEX CONCURRENTLY idx_personality_computations_subconscious_vector_gin ON personality_computations USING GIN (subconscious_personality_vector);
CREATE INDEX CONCURRENTLY idx_personality_computations_final_vector_gin ON personality_computations USING GIN (final_personality_vector);
CREATE INDEX CONCURRENTLY idx_personality_computations_posts_analyzed_gin ON personality_computations USING GIN (posts_analyzed) WHERE posts_analyzed IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_personality_computations_interactions_analyzed_gin ON personality_computations USING GIN (interactions_analyzed) WHERE interactions_analyzed IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_personality_computations_parameters_gin ON personality_computations USING GIN (computation_parameters) WHERE computation_parameters IS NOT NULL;

-- Additional indexes for personality_computation_schedules table
CREATE INDEX CONCURRENTLY idx_personality_schedules_user_active ON personality_computation_schedules(user_id, is_active);
CREATE INDEX CONCURRENTLY idx_personality_schedules_next_date_active ON personality_computation_schedules(next_computation_date, is_active);
CREATE INDEX CONCURRENTLY idx_personality_schedules_frequency_active ON personality_computation_schedules(computation_frequency, is_active);

-- Additional indexes for personality_computation_trends table
CREATE INDEX CONCURRENTLY idx_personality_trends_user_type ON personality_computation_trends(user_id, trend_type);
CREATE INDEX CONCURRENTLY idx_personality_trends_user_dimension ON personality_computation_trends(user_id, dimension_index) WHERE dimension_index IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_personality_trends_type_dimension ON personality_computation_trends(trend_type, dimension_index) WHERE dimension_index IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_personality_trends_significance ON personality_computation_trends(significance_level) WHERE significance_level IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_personality_trends_direction ON personality_computation_trends(trend_direction) WHERE trend_direction IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_personality_trends_change_magnitude ON personality_computation_trends(change_magnitude);

-- Additional indexes for personality_mappings table
CREATE INDEX CONCURRENTLY idx_personality_mappings_user_type ON personality_mappings(user_id, mapping_type);
CREATE INDEX CONCURRENTLY idx_personality_mappings_computation_type ON personality_mappings(computation_id, mapping_type);
CREATE INDEX CONCURRENTLY idx_personality_mappings_confidence ON personality_mappings(mapping_confidence);

-- GIN indexes for JSONB mapping data
CREATE INDEX CONCURRENTLY idx_personality_mappings_scores_gin ON personality_mappings USING GIN (mapping_scores);
CREATE INDEX CONCURRENTLY idx_personality_mappings_metadata_gin ON personality_mappings USING GIN (mapping_metadata) WHERE mapping_metadata IS NOT NULL;

-- Composite indexes for common query patterns
CREATE INDEX CONCURRENTLY idx_psychological_vectors_user_type_created ON psychological_state_vectors(user_id, vector_type, created_at DESC);
CREATE INDEX CONCURRENTLY idx_state_history_user_type_created ON state_update_history(user_id, update_type, created_at DESC);
CREATE INDEX CONCURRENTLY idx_personality_computations_user_period_date ON personality_computations(user_id, computation_period, computation_date DESC);
CREATE INDEX CONCURRENTLY idx_personality_trends_user_type_created ON personality_computation_trends(user_id, trend_type, created_at DESC);

-- Partial indexes for specific use cases
CREATE INDEX CONCURRENTLY idx_psychological_vectors_recent_v_raw ON psychological_state_vectors(user_id, created_at DESC) 
    WHERE vector_type = 'V_raw' AND created_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

CREATE INDEX CONCURRENTLY idx_psychological_vectors_recent_s_current ON psychological_state_vectors(user_id, created_at DESC) 
    WHERE vector_type = 'S_current' AND created_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

CREATE INDEX CONCURRENTLY idx_state_history_recent_updates ON state_update_history(user_id, created_at DESC) 
    WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

CREATE INDEX CONCURRENTLY idx_personality_computations_recent_valid ON personality_computations(user_id, computation_date DESC) 
    WHERE is_valid = TRUE AND computation_date > CURRENT_DATE - INTERVAL '1 year';

CREATE INDEX CONCURRENTLY idx_personality_trends_significant_changes ON personality_computation_trends(user_id, created_at DESC) 
    WHERE significance_level > 0.8 AND created_at > CURRENT_TIMESTAMP - INTERVAL '90 days';

-- Indexes for specific dimension queries (for the 5 core factors)
CREATE INDEX CONCURRENTLY idx_psychological_vectors_dimension_0 ON psychological_state_vectors USING GIN ((vector_data->0)) WHERE vector_type = 'V_raw';
CREATE INDEX CONCURRENTLY idx_psychological_vectors_dimension_1 ON psychological_state_vectors USING GIN ((vector_data->1)) WHERE vector_type = 'V_raw';
CREATE INDEX CONCURRENTLY idx_psychological_vectors_dimension_2 ON psychological_state_vectors USING GIN ((vector_data->2)) WHERE vector_type = 'V_raw';
CREATE INDEX CONCURRENTLY idx_psychological_vectors_dimension_3 ON psychological_state_vectors USING GIN ((vector_data->3)) WHERE vector_type = 'V_raw';
CREATE INDEX CONCURRENTLY idx_psychological_vectors_dimension_4 ON psychological_state_vectors USING GIN ((vector_data->4)) WHERE vector_type = 'V_raw';

-- Indexes for user state vector dimensions (core factors)
CREATE INDEX CONCURRENTLY idx_user_psychological_states_dimension_0 ON user_psychological_states USING GIN ((current_state_vector->0));
CREATE INDEX CONCURRENTLY idx_user_psychological_states_dimension_1 ON user_psychological_states USING GIN ((current_state_vector->1));
CREATE INDEX CONCURRENTLY idx_user_psychological_states_dimension_2 ON user_psychological_states USING GIN ((current_state_vector->2));
CREATE INDEX CONCURRENTLY idx_user_psychological_states_dimension_3 ON user_psychological_states USING GIN ((current_state_vector->3));
CREATE INDEX CONCURRENTLY idx_user_psychological_states_dimension_4 ON user_psychological_states USING GIN ((current_state_vector->4));

-- Comments for documentation
COMMENT ON INDEX idx_psychological_vectors_user_type IS 'Composite index for user queries by vector type';
COMMENT ON INDEX idx_psychological_vectors_data_gin IS 'GIN index for JSONB vector data queries and operations';
COMMENT ON INDEX idx_psychological_vectors_recent_v_raw IS 'Partial index for recent V_raw vectors (last 30 days)';
COMMENT ON INDEX idx_psychological_vectors_recent_s_current IS 'Partial index for recent S_current vectors (last 7 days)';
COMMENT ON INDEX idx_personality_computations_recent_valid IS 'Partial index for recent valid personality computations (last year)';
COMMENT ON INDEX idx_personality_trends_significant_changes IS 'Partial index for significant personality changes (last 90 days)';
COMMENT ON INDEX idx_psychological_vectors_dimension_0 IS 'GIN index for Attention dimension (dimension 0) queries';
COMMENT ON INDEX idx_psychological_vectors_dimension_1 IS 'GIN index for Contact dimension (dimension 1) queries';
COMMENT ON INDEX idx_psychological_vectors_dimension_2 IS 'GIN index for Feeling dimension (dimension 2) queries';
COMMENT ON INDEX idx_psychological_vectors_dimension_3 IS 'GIN index for Perception dimension (dimension 3) queries';
COMMENT ON INDEX idx_psychological_vectors_dimension_4 IS 'GIN index for Volition dimension (dimension 4) queries';
