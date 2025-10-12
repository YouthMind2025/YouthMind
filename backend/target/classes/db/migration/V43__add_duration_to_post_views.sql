-- V43: Add duration tracking to post_views table
-- Description: Renames viewed_at to view_started_at and adds duration tracking fields

-- Rename existing viewed_at column to view_started_at
ALTER TABLE post_views RENAME COLUMN viewed_at TO view_started_at;

-- Add new columns for duration tracking
ALTER TABLE post_views 
ADD COLUMN view_ended_at TIMESTAMP,
ADD COLUMN view_duration_seconds INTEGER;

-- Add comments for documentation
COMMENT ON COLUMN post_views.view_started_at IS 'Timestamp when user started viewing the post';
COMMENT ON COLUMN post_views.view_ended_at IS 'Timestamp when user stopped viewing the post';
COMMENT ON COLUMN post_views.view_duration_seconds IS 'Duration in seconds that user viewed the post';

-- Create index for significant views (>= 15 seconds) - used by recommendation engine
CREATE INDEX idx_post_views_duration_significant 
ON post_views(view_duration_seconds) 
WHERE view_duration_seconds >= 15;

-- Create index for post duration analysis
CREATE INDEX idx_post_views_post_duration 
ON post_views(post_id, view_duration_seconds)
WHERE view_duration_seconds IS NOT NULL;

-- Add check constraint to ensure duration is non-negative
ALTER TABLE post_views 
ADD CONSTRAINT chk_post_views_duration_non_negative 
CHECK (view_duration_seconds IS NULL OR view_duration_seconds >= 0);

-- Add comment explaining the significance threshold
COMMENT ON INDEX idx_post_views_duration_significant IS 'Optimizes queries for significant views (15+ seconds) used in recommendation engine R vector updates';

