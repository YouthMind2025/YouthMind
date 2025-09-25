-- Add search performance indexes for POST-006 search functionality
-- These indexes will significantly improve search query performance

-- Full-text search indexes for title, content, and topics
CREATE INDEX idx_posts_title_search ON posts USING gin(to_tsvector('english', title));
CREATE INDEX idx_posts_content_search ON posts USING gin(to_tsvector('english', content));
CREATE INDEX idx_posts_topics_search ON posts USING gin(to_tsvector('english', topics));

-- Composite indexes for common search patterns
CREATE INDEX idx_posts_emotion_created_at ON posts(emotion, created_at DESC);
CREATE INDEX idx_posts_title_emotion ON posts(title, emotion);
CREATE INDEX idx_posts_content_emotion ON posts(content, emotion);

-- Partial indexes for non-null topics (improves search performance)
CREATE INDEX idx_posts_topics_not_null ON posts(topics) WHERE topics IS NOT NULL;

-- Case-insensitive search indexes
CREATE INDEX idx_posts_title_lower ON posts(LOWER(title));
CREATE INDEX idx_posts_content_lower ON posts(LOWER(content));
CREATE INDEX idx_posts_topics_lower ON posts(LOWER(topics));
CREATE INDEX idx_posts_emotion_lower ON posts(LOWER(emotion));

-- Add comments for documentation
COMMENT ON INDEX idx_posts_title_search IS 'Full-text search index for post titles';
COMMENT ON INDEX idx_posts_content_search IS 'Full-text search index for post content';
COMMENT ON INDEX idx_posts_topics_search IS 'Full-text search index for post topics';
COMMENT ON INDEX idx_posts_emotion_created_at IS 'Composite index for emotion + date filtering';
COMMENT ON INDEX idx_posts_title_emotion IS 'Composite index for title + emotion searches';
COMMENT ON INDEX idx_posts_content_emotion IS 'Composite index for content + emotion searches';
COMMENT ON INDEX idx_posts_topics_not_null IS 'Partial index for posts with topics';
COMMENT ON INDEX idx_posts_title_lower IS 'Case-insensitive search index for titles';
COMMENT ON INDEX idx_posts_content_lower IS 'Case-insensitive search index for content';
COMMENT ON INDEX idx_posts_topics_lower IS 'Case-insensitive search index for topics';
COMMENT ON INDEX idx_posts_emotion_lower IS 'Case-insensitive search index for emotions'; 