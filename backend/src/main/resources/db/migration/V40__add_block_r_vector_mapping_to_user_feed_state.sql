-- Add block_r_vector_mapping column to user_feed_state table
-- This column stores a JSON mapping of block numbers to R vector versions
-- Used for implementing "two rounds behind" logic in feed block generation

-- Add the new column
ALTER TABLE user_feed_state 
ADD COLUMN block_r_vector_mapping JSONB;

-- Add a comment explaining the column purpose
COMMENT ON COLUMN user_feed_state.block_r_vector_mapping IS 
'JSON mapping of block numbers to R vector versions used for generating those blocks. Format: {"1": 5, "2": 8, "3": 12}. Used for "two rounds behind" logic where block N uses R vector from block N-2.';

-- Create an index on the JSONB column for efficient queries
-- This allows fast lookups of R vector versions by block number
CREATE INDEX idx_user_feed_state_block_r_vector_mapping 
ON user_feed_state USING GIN (block_r_vector_mapping);

-- Add a check constraint to ensure the JSON structure is valid
-- The JSON should be an object with integer keys and integer values
ALTER TABLE user_feed_state 
ADD CONSTRAINT chk_block_r_vector_mapping_format 
CHECK (
    block_r_vector_mapping IS NULL 
    OR (
        jsonb_typeof(block_r_vector_mapping) = 'object'
        AND block_r_vector_mapping != '{}'::jsonb
    )
);

-- Initialize existing records with empty JSON object
-- This ensures all existing user_feed_state records have a valid JSON structure
UPDATE user_feed_state 
SET block_r_vector_mapping = '{}'::jsonb 
WHERE block_r_vector_mapping IS NULL;
