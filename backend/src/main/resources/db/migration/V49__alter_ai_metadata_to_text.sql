-- V49: Alter ai_metadata column from VARCHAR(255) to TEXT
-- This migration fixes the column size constraint issue where AI metadata JSON
-- exceeds the 255 character limit when storing psychological vectors and analysis data
-- 
-- Reason: The ai_metadata column needs to store large JSON objects containing:
-- - psychological_vector: 43-element array of doubles
-- - generation_method, vector_valid, analysis_timestamp, analysis_version
-- - psychological_confidence and other metadata
-- Total size can easily exceed 255 characters, requiring TEXT type

-- Alter the column type from VARCHAR(255) to TEXT
ALTER TABLE posts 
ALTER COLUMN ai_metadata TYPE TEXT;

-- Add comment explaining the column purpose
COMMENT ON COLUMN posts.ai_metadata IS 'AI analysis metadata stored as JSON (TEXT type to support large psychological vectors and analysis data)';

