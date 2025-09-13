-- Migration V15: Add AI-specific optimization indexes and database improvements
-- This migration adds indexes specifically for AI features and performance optimization

-- AI API Usage Optimization Indexes
-- These indexes optimize queries for AI usage analytics and monitoring

-- Index for AI usage queries by user and date range (for user analytics)
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_user_created_at_desc ON ai_api_usage(user_id, created_at DESC);

-- Index for AI usage queries by request type and date (for type analytics)
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_type_created_at_desc ON ai_api_usage(request_type, created_at DESC);

-- Index for AI usage queries by model and date (for model analytics)
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_model_created_at_desc ON ai_api_usage(model_name, created_at DESC);

-- Index for AI usage success/failure analysis
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_success_created_at ON ai_api_usage(success, created_at DESC);

-- Index for AI usage cost analysis
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_cost_created_at ON ai_api_usage(total_cost, created_at DESC);

-- Index for AI usage performance analysis (response time)
CREATE INDEX IF NOT EXISTS idx_ai_api_usage_duration_created_at ON ai_api_usage(request_duration_ms, created_at DESC);

-- Post Moderation Optimization Indexes
-- These indexes optimize queries for content moderation features

-- Index for flagged posts queries (moderation dashboard)
CREATE INDEX IF NOT EXISTS idx_posts_flagged_created_at ON posts(flagged, created_at DESC);

-- Index for flagged posts by user (user moderation history)
CREATE INDEX IF NOT EXISTS idx_posts_flagged_user_created_at ON posts(flagged, user_id, created_at DESC);

-- Index for posts flagged at specific times (moderation analytics)
CREATE INDEX IF NOT EXISTS idx_posts_flagged_at ON posts(flagged_at);

-- Index for posts with flag reasons (moderation categorization)
CREATE INDEX IF NOT EXISTS idx_posts_flag_reason ON posts(flag_reason) WHERE flag_reason IS NOT NULL;

-- User Moderation Status Optimization Indexes
-- These indexes optimize queries for user moderation and warning system

-- Index for users by status (active, warned, banned, suspended)
CREATE INDEX IF NOT EXISTS idx_users_status_created_at ON users(user_status, created_at DESC);

-- Index for users by warning count (progressive discipline queries)
CREATE INDEX IF NOT EXISTS idx_users_warnings_count ON users(warnings_count);

-- Index for users with active bans (ban management queries)
CREATE INDEX IF NOT EXISTS idx_users_ban_expires_active ON users(ban_expires_at) WHERE ban_expires_at IS NOT NULL;

-- Index for users by role and status (admin queries)
CREATE INDEX IF NOT EXISTS idx_users_role_status ON users(role, user_status);

-- Warning History Optimization Indexes
-- These indexes optimize queries for warning system analytics

-- Index for warnings by user and date (user warning history)
CREATE INDEX IF NOT EXISTS idx_warning_history_user_created_at ON warning_history(user_id, created_at DESC);

-- Index for warnings by type (warning categorization)
CREATE INDEX IF NOT EXISTS idx_warning_history_type ON warning_history(warning_type);

-- Index for warnings by severity (severity analysis)
CREATE INDEX IF NOT EXISTS idx_warning_history_severity ON warning_history(severity);

-- Index for unacknowledged warnings (pending actions)
CREATE INDEX IF NOT EXISTS idx_warning_history_unacknowledged ON warning_history(user_id, acknowledged_at) WHERE acknowledged_at IS NULL;

-- Index for warnings by moderator (moderator activity)
CREATE INDEX IF NOT EXISTS idx_warning_history_moderator ON warning_history(moderator_id, created_at DESC);

-- User Moderation Status Table Optimization Indexes
-- These indexes optimize queries for user moderation status management

-- Index for users by warning level (progressive discipline)
CREATE INDEX IF NOT EXISTS idx_user_moderation_warning_level ON user_moderation_status(warning_level);

-- Index for users requiring manual review
CREATE INDEX IF NOT EXISTS idx_user_moderation_manual_review ON user_moderation_status(manual_review_required) WHERE manual_review_required = true;

-- Index for users with auto-moderation disabled
CREATE INDEX IF NOT EXISTS idx_user_moderation_auto_disabled ON user_moderation_status(auto_moderation_enabled) WHERE auto_moderation_enabled = false;

-- Post Views Optimization Indexes
-- These indexes optimize queries for view tracking and analytics

-- Index for post views by post and date (post analytics)
CREATE INDEX IF NOT EXISTS idx_post_views_post_viewed_at ON post_views(post_id, viewed_at DESC);

-- Index for user view history (user activity)
CREATE INDEX IF NOT EXISTS idx_post_views_user_viewed_at ON post_views(user_id, viewed_at DESC);

-- Composite index for view analytics (post popularity)
CREATE INDEX IF NOT EXISTS idx_post_views_analytics ON post_views(post_id, viewed_at DESC, user_id);

-- General Performance Optimization Indexes
-- These indexes optimize common query patterns across the application

-- Index for posts by emotion and date (emotion-based feeds)
CREATE INDEX IF NOT EXISTS idx_posts_emotion_created_at ON posts(emotion, created_at DESC);

-- Index for posts by topics (topic-based filtering)
CREATE INDEX IF NOT EXISTS idx_posts_topics ON posts USING gin(to_tsvector('english', topics)) WHERE topics IS NOT NULL;

-- Index for posts by AI metadata (AI analysis queries)
CREATE INDEX IF NOT EXISTS idx_posts_ai_metadata ON posts USING gin(to_tsvector('english', ai_metadata)) WHERE ai_metadata IS NOT NULL;

-- Index for users by username (username lookup optimization)
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username) WHERE username IS NOT NULL;

-- Index for users by email (login optimization - already exists but ensuring it's there)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Comments Optimization Indexes
-- These indexes optimize queries for comment system

-- Index for comments by post and date (post comment threads)
CREATE INDEX IF NOT EXISTS idx_comments_post_created_at ON comments(post_id, created_at DESC);

-- Index for comments by author and date (user comment history)
CREATE INDEX IF NOT EXISTS idx_comments_author_created_at ON comments(author_id, created_at DESC);

-- Index for nested comments (comment threading)
CREATE INDEX IF NOT EXISTS idx_comments_parent_created_at ON comments(parent_id, created_at DESC) WHERE parent_id IS NOT NULL;

-- Drafts Optimization Indexes
-- These indexes optimize queries for draft management

-- Index for drafts by user and date (user draft management)
CREATE INDEX IF NOT EXISTS idx_drafts_user_updated_at ON drafts(user_id, updated_at DESC);

-- Index for drafts by emotion (emotion-based draft filtering)
CREATE INDEX IF NOT EXISTS idx_drafts_emotion ON drafts(emotion) WHERE emotion IS NOT NULL;

-- Conversations Optimization Indexes
-- These indexes optimize queries for conversation management

-- Index for conversations by user and pin status (user conversation list)
CREATE INDEX IF NOT EXISTS idx_conversations_user_pinned ON conversations(user_id, is_pinned, updated_at DESC);

-- Index for conversations by update date (recent conversations)
CREATE INDEX IF NOT EXISTS idx_conversations_updated_at ON conversations(updated_at DESC);

-- Messages Optimization Indexes
-- These indexes optimize queries for message management

-- Index for messages by conversation and timestamp (conversation threads)
CREATE INDEX IF NOT EXISTS idx_messages_conversation_timestamp ON messages(conversation_id, timestamp DESC);

-- Index for messages by sender and timestamp (sender analysis)
CREATE INDEX IF NOT EXISTS idx_messages_sender_timestamp ON messages(sender, timestamp DESC);

-- User Engagements Optimization Indexes
-- These indexes optimize queries for engagement analytics

-- Index for engagements by user and type (user engagement history)
CREATE INDEX IF NOT EXISTS idx_user_engagements_user_type ON user_engagements(user_id, type, created_at DESC);

-- Index for engagements by post and type (post engagement analytics)
CREATE INDEX IF NOT EXISTS idx_user_engagements_post_type ON user_engagements(post_id, type, created_at DESC);

-- Index for engagements by type and date (engagement analytics)
CREATE INDEX IF NOT EXISTS idx_user_engagements_type_date ON user_engagements(type, created_at DESC);

-- Comment Engagements Optimization Indexes
-- These indexes optimize queries for comment engagement analytics

-- Index for comment engagements by user and type (user comment engagement history)
CREATE INDEX IF NOT EXISTS idx_comment_engagements_user_type ON comment_engagements(user_id, type, created_at DESC);

-- Index for comment engagements by comment and type (comment engagement analytics)
CREATE INDEX IF NOT EXISTS idx_comment_engagements_comment_type ON comment_engagements(comment_id, type, created_at DESC);

-- Post Collections Optimization Indexes
-- These indexes optimize queries for post collection management

-- Index for collections by user and date (user collection management)
CREATE INDEX IF NOT EXISTS idx_post_collections_user_created_at ON post_collections(user_id, created_at DESC);

-- Index for collections by post (post collection analytics)
CREATE INDEX IF NOT EXISTS idx_post_collections_post ON post_collections(post_id, created_at DESC);

-- Post Resonances Optimization Indexes
-- These indexes optimize queries for resonance management

-- Index for resonances by user and date (user resonance history)
CREATE INDEX IF NOT EXISTS idx_post_resonances_user_created_at ON post_resonances(user_id, created_at DESC);

-- Index for resonances by post (post resonance analytics)
CREATE INDEX IF NOT EXISTS idx_post_resonances_post ON post_resonances(post_id, created_at DESC);

-- Articles Optimization Indexes
-- These indexes optimize queries for article management

-- Index for articles by published status and date (article management)
CREATE INDEX IF NOT EXISTS idx_articles_published_created_at ON articles(published, created_at DESC);

-- Index for articles by title (article search)
CREATE INDEX IF NOT EXISTS idx_articles_title ON articles USING gin(to_tsvector('english', title));

-- Index for articles by content (article content search)
CREATE INDEX IF NOT EXISTS idx_articles_content ON articles USING gin(to_tsvector('english', content));

-- Add comments for documentation
COMMENT ON INDEX idx_ai_api_usage_user_created_at_desc IS 'Optimizes AI usage queries by user and date range for analytics';
COMMENT ON INDEX idx_posts_flagged_created_at IS 'Optimizes flagged posts queries for moderation dashboard';
COMMENT ON INDEX idx_users_status_created_at IS 'Optimizes user queries by status for moderation management';
COMMENT ON INDEX idx_warning_history_user_created_at IS 'Optimizes warning history queries by user and date';
COMMENT ON INDEX idx_post_views_post_viewed_at IS 'Optimizes post view analytics queries';
COMMENT ON INDEX idx_posts_topics IS 'Full-text search index for post topics';
COMMENT ON INDEX idx_posts_ai_metadata IS 'Full-text search index for AI metadata';
COMMENT ON INDEX idx_articles_title IS 'Full-text search index for article titles';
COMMENT ON INDEX idx_articles_content IS 'Full-text search index for article content';
