-- Migration: V38__create_feed_block_cache.sql
-- Description: Create feed_block_cache table for caching personalized feed blocks
-- Author: YouthMind Team
-- Date: 2024-01-XX

-- Create feed_block_cache table
CREATE TABLE feed_block_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    block_number INTEGER NOT NULL,
    post_ids JSONB NOT NULL,
    emotion_distribution JSONB NOT NULL,
    r_vector_version INTEGER NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add table comment
COMMENT ON TABLE feed_block_cache IS 'Caches personalized feed blocks with emotion distribution and post ordering';

-- Add column comments
COMMENT ON COLUMN feed_block_cache.id IS 'Primary key - unique identifier for the cache entry';
COMMENT ON COLUMN feed_block_cache.user_id IS 'User ID - identifies which user this block belongs to';
COMMENT ON COLUMN feed_block_cache.block_number IS 'Block sequence number (1, 2, 3, etc.)';
COMMENT ON COLUMN feed_block_cache.post_ids IS 'Ordered list of post IDs in this block (JSON array)';
COMMENT ON COLUMN feed_block_cache.emotion_distribution IS 'Emotion percentage distribution used (JSON object)';
COMMENT ON COLUMN feed_block_cache.r_vector_version IS 'R vector version used to generate this block';
COMMENT ON COLUMN feed_block_cache.generated_at IS 'Timestamp when this block was generated';
COMMENT ON COLUMN feed_block_cache.expires_at IS 'Timestamp when this block expires and should be regenerated';
COMMENT ON COLUMN feed_block_cache.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN feed_block_cache.updated_at IS 'Record last update timestamp';

-- Create indexes for performance
CREATE INDEX idx_feed_block_user_block ON feed_block_cache(user_id, block_number);
CREATE INDEX idx_feed_block_user_id ON feed_block_cache(user_id);
CREATE INDEX idx_feed_block_expires_at ON feed_block_cache(expires_at);
CREATE INDEX idx_feed_block_generated_at ON feed_block_cache(generated_at);
CREATE INDEX idx_feed_block_r_vector_version ON feed_block_cache(r_vector_version);

-- Add constraints
ALTER TABLE feed_block_cache ADD CONSTRAINT chk_feed_block_block_number_positive 
    CHECK (block_number >= 1);

ALTER TABLE feed_block_cache ADD CONSTRAINT chk_feed_block_r_vector_version_positive 
    CHECK (r_vector_version >= 1);

ALTER TABLE feed_block_cache ADD CONSTRAINT chk_feed_block_expires_after_generated 
    CHECK (expires_at > generated_at);

-- Add foreign key constraint (assuming users table exists)
-- ALTER TABLE feed_block_cache ADD CONSTRAINT fk_feed_block_user_id 
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Create trigger for updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_feed_block_cache_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_feed_block_cache_updated_at
    BEFORE UPDATE ON feed_block_cache
    FOR EACH ROW
    EXECUTE FUNCTION update_feed_block_cache_updated_at();

-- Insert sample data for testing (optional - remove in production)
-- INSERT INTO feed_block_cache (user_id, block_number, post_ids, emotion_distribution, r_vector_version, generated_at, expires_at)
-- VALUES (
--     '123e4567-e89b-12d3-a456-426614174000',
--     1,
--     '["111e4567-e89b-12d3-a456-426614174001", "222e4567-e89b-12d3-a456-426614174002"]',
--     '{"sad": 20, "angry": 30, "happy": 50}',
--     1,
--     CURRENT_TIMESTAMP,
--     CURRENT_TIMESTAMP + INTERVAL '24 hours'
-- );


