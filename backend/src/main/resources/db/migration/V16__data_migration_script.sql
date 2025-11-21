-- Migration V16: Data migration script for AI features and system updates
-- This migration handles data updates and cleanup for existing records

-- Update existing posts to have proper default values for new AI fields
-- This ensures all existing posts have the new flagged fields properly initialized

UPDATE posts 
SET flagged = false 
WHERE flagged IS NULL;

UPDATE posts 
SET flag_reason = NULL 
WHERE flag_reason = '';

UPDATE posts 
SET flagged_at = NULL 
WHERE flagged_at IS NULL;

-- Update existing users to have proper default values for warning system fields
-- This ensures all existing users have the new warning fields properly initialized

UPDATE users 
SET warnings_count = 0 
WHERE warnings_count IS NULL;

UPDATE users 
SET user_status = 'ACTIVE' 
WHERE user_status IS NULL;

UPDATE users 
SET ban_expires_at = NULL 
WHERE ban_expires_at IS NULL;

-- Update existing users to have proper role values
-- Ensure all users have a valid role (default to USER if not set)

UPDATE users 
SET role = 'USER' 
WHERE role IS NULL;

-- Update existing posts to have proper default values for engagement fields
-- This ensures all existing posts have proper engagement counts

UPDATE posts 
SET resonance = 0 
WHERE resonance IS NULL;

UPDATE posts 
SET view_count = 0 
WHERE view_count IS NULL;

UPDATE posts 
SET collect_count = 0 
WHERE collect_count IS NULL;

-- Update existing posts to have proper title values
-- Ensure all posts have a title (use content preview if title is empty)

UPDATE posts 
SET title = CASE 
    WHEN title IS NULL OR title = '' OR title = 'Untitled Post' THEN 
        CASE 
            WHEN LENGTH(content) > 50 THEN LEFT(content, 47) || '...'
            ELSE content
        END
    ELSE title
END;

-- Update existing users to have proper default values for engagement counts
-- This ensures all existing users have proper engagement counts

UPDATE users 
SET posts_count = 0 
WHERE posts_count IS NULL;

UPDATE users 
SET likes_count = 0 
WHERE likes_count IS NULL;

UPDATE users 
SET bookmarks_count = 0 
WHERE bookmarks_count IS NULL;

-- Update existing users to have proper username values
-- Generate usernames from email if username is not set

UPDATE users 
SET username = LOWER(SPLIT_PART(email, '@', 1)) 
WHERE username IS NULL OR username = '';

-- Handle duplicate usernames by appending numbers
-- This ensures all usernames are unique

WITH numbered_users AS (
    SELECT id, username, ROW_NUMBER() OVER (PARTITION BY username ORDER BY created_at) as rn
    FROM users 
    WHERE username IS NOT NULL
)
UPDATE users 
SET username = u.username || '_' || u.rn
FROM numbered_users u
WHERE users.id = u.id AND u.rn > 1;

-- Update existing comments to have proper default values
-- This ensures all existing comments have proper engagement counts

UPDATE comments 
SET likes = 0 
WHERE likes IS NULL;

-- Update existing drafts to have proper default values
-- This ensures all existing drafts have proper timestamps

UPDATE drafts 
SET created_at = CURRENT_TIMESTAMP 
WHERE created_at IS NULL;

UPDATE drafts 
SET updated_at = CURRENT_TIMESTAMP 
WHERE updated_at IS NULL;

-- Update existing conversations to have proper default values
-- This ensures all existing conversations have proper timestamps and flags

UPDATE conversations 
SET created_at = CURRENT_TIMESTAMP 
WHERE created_at IS NULL;

UPDATE conversations 
SET updated_at = CURRENT_TIMESTAMP 
WHERE updated_at IS NULL;

UPDATE conversations 
SET is_pinned = false 
WHERE is_pinned IS NULL;

-- Update existing messages to have proper default values
-- This ensures all existing messages have proper timestamps

UPDATE messages 
SET timestamp = CURRENT_TIMESTAMP 
WHERE timestamp IS NULL;

-- Update existing articles to have proper default values
-- This ensures all existing articles have proper timestamps and status

UPDATE articles 
SET created_at = CURRENT_TIMESTAMP 
WHERE created_at IS NULL;

UPDATE articles 
SET updated_at = CURRENT_TIMESTAMP 
WHERE updated_at IS NULL;

UPDATE articles 
SET published = true 
WHERE published IS NULL;

-- Clean up any orphaned records
-- Remove any records that reference non-existent users or posts

-- Clean up orphaned comments
DELETE FROM comments 
WHERE author_id NOT IN (SELECT id FROM users);

DELETE FROM comments 
WHERE post_id NOT IN (SELECT id FROM posts);

-- Clean up orphaned drafts
DELETE FROM drafts 
WHERE user_id NOT IN (SELECT id FROM users);

-- Clean up orphaned conversations
DELETE FROM conversations 
WHERE user_id NOT IN (SELECT id FROM users);

-- Clean up orphaned messages
DELETE FROM messages 
WHERE conversation_id NOT IN (SELECT id FROM conversations);

-- Clean up orphaned user engagements
DELETE FROM user_engagements 
WHERE user_id NOT IN (SELECT id FROM users);

DELETE FROM user_engagements 
WHERE post_id NOT IN (SELECT id FROM posts);

-- Clean up orphaned comment engagements
DELETE FROM comment_engagements 
WHERE user_id NOT IN (SELECT id FROM users);

DELETE FROM comment_engagements 
WHERE comment_id NOT IN (SELECT id FROM comments);

-- Clean up orphaned post collections
DELETE FROM post_collections 
WHERE user_id NOT IN (SELECT id FROM users);

DELETE FROM post_collections 
WHERE post_id NOT IN (SELECT id FROM posts);

-- Clean up orphaned post resonances
DELETE FROM post_resonances 
WHERE user_id NOT IN (SELECT id FROM users);

DELETE FROM post_resonances 
WHERE post_id NOT IN (SELECT id FROM posts);

-- Clean up orphaned post views
DELETE FROM post_views 
WHERE user_id NOT IN (SELECT id FROM users);

DELETE FROM post_views 
WHERE post_id NOT IN (SELECT id FROM posts);

-- Clean up orphaned warning history
DELETE FROM warning_history 
WHERE user_id NOT IN (SELECT id FROM users);

DELETE FROM warning_history 
WHERE moderator_id NOT IN (SELECT id FROM users);

DELETE FROM warning_history 
WHERE post_id NOT IN (SELECT id FROM posts);

-- Clean up orphaned user moderation status
DELETE FROM user_moderation_status 
WHERE user_id NOT IN (SELECT id FROM users);

-- Clean up orphaned AI API usage
DELETE FROM ai_api_usage 
WHERE user_id NOT IN (SELECT id FROM users);

-- Clean up orphaned AI analysis
DELETE FROM ai_analysis 
WHERE post_id NOT IN (SELECT id FROM posts);

-- Update statistics for existing users
-- Recalculate post counts for all users

UPDATE users 
SET posts_count = (
    SELECT COUNT(*) 
    FROM posts 
    WHERE posts.user_id = users.id
);

-- Recalculate engagement counts for all users
-- This is a simplified calculation - in production, you might want more sophisticated logic

UPDATE users 
SET likes_count = (
    SELECT COALESCE(SUM(resonance), 0) 
    FROM posts 
    WHERE posts.user_id = users.id
);

-- Update statistics for existing posts
-- Recalculate view counts for all posts

UPDATE posts 
SET view_count = (
    SELECT COUNT(*) 
    FROM post_views 
    WHERE post_views.post_id = posts.id
);

-- Recalculate collect counts for all posts

UPDATE posts 
SET collect_count = (
    SELECT COUNT(*) 
    FROM post_collections 
    WHERE post_collections.post_id = posts.id
);

-- Recalculate resonance counts for all posts

UPDATE posts 
SET resonance = (
    SELECT COUNT(*) 
    FROM post_resonances 
    WHERE post_resonances.post_id = posts.id
);

-- Add comments for documentation
COMMENT ON TABLE posts IS 'Updated with AI moderation fields and proper defaults';
COMMENT ON TABLE users IS 'Updated with warning system fields and proper defaults';
COMMENT ON TABLE comments IS 'Updated with proper engagement counts and defaults';
COMMENT ON TABLE drafts IS 'Updated with proper timestamps and defaults';
COMMENT ON TABLE conversations IS 'Updated with proper timestamps and flags';
COMMENT ON TABLE messages IS 'Updated with proper timestamps and defaults';
COMMENT ON TABLE articles IS 'Updated with proper timestamps and status';
COMMENT ON TABLE user_engagements IS 'Cleaned up orphaned records';
COMMENT ON TABLE comment_engagements IS 'Cleaned up orphaned records';
COMMENT ON TABLE post_collections IS 'Cleaned up orphaned records';
COMMENT ON TABLE post_resonances IS 'Cleaned up orphaned records';
COMMENT ON TABLE post_views IS 'Cleaned up orphaned records';
COMMENT ON TABLE warning_history IS 'Cleaned up orphaned records';
COMMENT ON TABLE user_moderation_status IS 'Cleaned up orphaned records';
COMMENT ON TABLE ai_api_usage IS 'Cleaned up orphaned records';
COMMENT ON TABLE ai_analysis IS 'Cleaned up orphaned records';
