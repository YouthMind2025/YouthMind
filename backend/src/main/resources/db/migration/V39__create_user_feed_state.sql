-- Migration: V39__create_user_feed_state.sql
-- Description: Create user_feed_state table for tracking user feed state and preferences
-- Author: YouthMind Team
-- Date: 2024-01-XX

-- Create user_feed_state table
CREATE TABLE user_feed_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    current_block_number INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shown_post_ids JSONB NOT NULL DEFAULT '[]',
    current_r_vector_version INTEGER NOT NULL DEFAULT 1,
    total_blocks_generated INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add table comment
COMMENT ON TABLE user_feed_state IS 'Tracks user feed state, preferences, and progress across feed sessions';

-- Add column comments
COMMENT ON COLUMN user_feed_state.id IS 'Primary key - unique identifier for the state record';
COMMENT ON COLUMN user_feed_state.user_id IS 'User ID - unique identifier for the user (one state per user)';
COMMENT ON COLUMN user_feed_state.current_block_number IS 'Current block number the user is viewing (0-based)';
COMMENT ON COLUMN user_feed_state.last_accessed_at IS 'Last time the user accessed their feed';
COMMENT ON COLUMN user_feed_state.shown_post_ids IS 'Set of all post IDs shown to user across all blocks (JSON array)';
COMMENT ON COLUMN user_feed_state.current_r_vector_version IS 'Current R vector version for tracking user preferences';
COMMENT ON COLUMN user_feed_state.total_blocks_generated IS 'Total number of blocks generated for this user';
COMMENT ON COLUMN user_feed_state.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN user_feed_state.updated_at IS 'Record last update timestamp';

-- Create indexes for performance
CREATE INDEX idx_user_feed_state_user_id ON user_feed_state(user_id);
CREATE INDEX idx_user_feed_state_last_accessed ON user_feed_state(last_accessed_at);
CREATE INDEX idx_user_feed_state_current_block ON user_feed_state(current_block_number);
CREATE INDEX idx_user_feed_state_r_vector_version ON user_feed_state(current_r_vector_version);
CREATE INDEX idx_user_feed_state_total_blocks ON user_feed_state(total_blocks_generated);

-- Add constraints
ALTER TABLE user_feed_state ADD CONSTRAINT chk_user_feed_state_block_number_non_negative 
    CHECK (current_block_number >= 0);

ALTER TABLE user_feed_state ADD CONSTRAINT chk_user_feed_state_r_vector_version_positive 
    CHECK (current_r_vector_version >= 1);

ALTER TABLE user_feed_state ADD CONSTRAINT chk_user_feed_state_total_blocks_non_negative 
    CHECK (total_blocks_generated >= 0);

-- Add foreign key constraint (assuming users table exists)
-- ALTER TABLE user_feed_state ADD CONSTRAINT fk_user_feed_state_user_id 
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Create trigger for updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_user_feed_state_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_user_feed_state_updated_at
    BEFORE UPDATE ON user_feed_state
    FOR EACH ROW
    EXECUTE FUNCTION update_user_feed_state_updated_at();

-- Create function to clean up stale user feed states
CREATE OR REPLACE FUNCTION cleanup_stale_user_feed_states(stale_threshold_hours INTEGER DEFAULT 168)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_feed_state 
    WHERE last_accessed_at < CURRENT_TIMESTAMP - INTERVAL '1 hour' * stale_threshold_hours;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to get user feed statistics
CREATE OR REPLACE FUNCTION get_user_feed_stats(target_user_id UUID)
RETURNS TABLE (
    user_id UUID,
    current_block_number INTEGER,
    total_blocks_generated INTEGER,
    shown_post_count BIGINT,
    last_access_age_hours NUMERIC,
    current_r_vector_version INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ufs.user_id,
        ufs.current_block_number,
        ufs.total_blocks_generated,
        jsonb_array_length(ufs.shown_post_ids) as shown_post_count,
        EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - ufs.last_accessed_at)) / 3600 as last_access_age_hours,
        ufs.current_r_vector_version
    FROM user_feed_state ufs
    WHERE ufs.user_id = target_user_id;
END;
$$ LANGUAGE plpgsql;

-- Insert sample data for testing (optional - remove in production)
-- INSERT INTO user_feed_state (user_id, current_block_number, shown_post_ids, current_r_vector_version, total_blocks_generated)
-- VALUES (
--     '123e4567-e89b-12d3-a456-426614174000',
--     0,
--     '["111e4567-e89b-12d3-a456-426614174001", "222e4567-e89b-12d3-a456-426614174002"]',
--     1,
--     0
-- );


