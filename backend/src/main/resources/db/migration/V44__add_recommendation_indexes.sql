-- V44: Add indexes for recommendation engine performance
-- Description: Creates composite and partial indexes to optimize post selection queries

-- Main composite index for emotion + time + views
-- Covers the most common recommendation query pattern
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_recommendation_main 
ON posts(emotion, created_at DESC, view_count DESC) 
WHERE flagged = false AND emotion IS NOT NULL;

-- Partial index for hot posts (view_count >= 100)
-- Optimizes selection of high-engagement content
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_hot_by_emotion 
ON posts(emotion, view_count DESC, created_at DESC) 
WHERE view_count >= 100 AND flagged = false;

-- Partial index for normal posts (50 <= view_count < 100)
-- Optimizes selection of medium-engagement content
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_normal_by_emotion 
ON posts(emotion, created_at DESC, view_count DESC) 
WHERE view_count >= 50 AND view_count < 100 AND flagged = false;

-- Partial index for cold posts (view_count < 50)
-- Optimizes selection of low-engagement/fresh content
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_cold_by_emotion 
ON posts(emotion, created_at DESC) 
WHERE view_count < 50 AND flagged = false;

-- Index for random selection within emotion category
-- Supports ORDER BY RANDOM() queries efficiently
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_random_by_emotion 
ON posts(emotion, id) 
WHERE flagged = false AND emotion IS NOT NULL;

-- Composite index for view count range queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_posts_emotion_views_range 
ON posts(emotion, view_count, created_at DESC)
WHERE flagged = false;

-- Update table statistics for query planner
ANALYZE posts;
ANALYZE post_views;
ANALYZE user_recommendation_vectors;

-- Add helpful comments
COMMENT ON INDEX idx_posts_recommendation_main IS 'Primary index for recommendation engine - covers emotion, recency, and popularity';
COMMENT ON INDEX idx_posts_hot_by_emotion IS 'Optimizes hot post selection (>=100 views) - used for 30% of feed';
COMMENT ON INDEX idx_posts_normal_by_emotion IS 'Optimizes normal post selection (50-99 views) - used for 50% of feed';
COMMENT ON INDEX idx_posts_cold_by_emotion IS 'Optimizes cold post selection (<50 views) - used for 20% of feed';
COMMENT ON INDEX idx_posts_random_by_emotion IS 'Supports random sampling within emotion categories for variety';

