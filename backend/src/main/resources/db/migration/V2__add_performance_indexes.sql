-- Migration V2: Add performance indexes for query optimization
-- AI Integration Performance Optimization

-- REQUIRED INDIVIDUAL INDEXES (AI Integration Requirements)

-- 1. Add index for users.email (login lookup optimization)
-- This index will dramatically improve performance for:
-- - findByEmail(String email)
-- - existsByEmail(String email)
CREATE INDEX IF NOT EXISTS idx_users_email_lookup ON users(email);

-- 2. Add index for posts.user_id (user post retrieval)
-- This index will optimize:
-- - findByUser(User user)
-- - findByUserOrderByCreatedAtDesc(User user)
CREATE INDEX IF NOT EXISTS idx_posts_user_lookup ON posts(user_id);

-- 3. Add index for posts.created_at (timeline queries)
-- This index will optimize:
-- - Time-based post ordering
-- - Recent posts queries
CREATE INDEX IF NOT EXISTS idx_posts_timeline ON posts(created_at DESC);

-- 4. Add index for posts.emotion_tag (emotional content filtering)
-- This index will optimize:
-- - findByEmotionTag(String emotionTag)
-- - findByUserAndEmotionTag(User user, String emotionTag)
CREATE INDEX IF NOT EXISTS idx_posts_emotion_lookup ON posts(emotion_tag);

-- 5. Add composite index for user emotion queries
-- This index will optimize:
-- - Complex queries filtering by user and emotion together
CREATE INDEX IF NOT EXISTS idx_posts_user_emotion ON posts(user_id, emotion_tag, created_at DESC);

-- 6. Add index for AI analysis post lookup
-- This index will optimize:
-- - AI analysis retrieval by post
CREATE INDEX IF NOT EXISTS idx_ai_analysis_lookup ON ai_analysis(post_id);