-- Migration V2: Add performance indexes for query optimization
-- Task 3: Query Performance Optimization

-- REQUIRED INDIVIDUAL INDEXES (Task 3 Requirements)

-- 1. Add index for users.email_hash (login lookup optimization)
-- This index will dramatically improve performance for:
-- - findByEmailHash(String emailHash) 
-- - existsByEmailHash(String emailHash)
CREATE INDEX IF NOT EXISTS idx_users_email_hash ON users(email_hash);

-- 2. Add index for posts.created_at (feed sorting optimization)
-- This index will optimize feed timeline queries with ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);

-- 3. Add index for posts.emotion_tag (emotion filtering optimization)  
-- This index will optimize emotion-based filtering queries
CREATE INDEX IF NOT EXISTS idx_posts_emotion_tag ON posts(emotion_tag);

-- ADVANCED COMPOSITE INDEXES (Performance Bonuses)

-- 4. Add composite index for posts filtering by user and emotion (advanced optimization)
CREATE INDEX IF NOT EXISTS idx_posts_user_emotion ON posts(user_id, emotion_tag);

-- 5. Add composite index for posts feed queries (user + created_at for timeline)
CREATE INDEX IF NOT EXISTS idx_posts_user_created_at ON posts(user_id, created_at DESC);

-- Performance analysis summary:
-- These indexes will optimize:
-- 1. Login queries: O(1) vs O(n) lookup on email_hash
-- 2. Feed sorting: Fast ORDER BY created_at DESC operations
-- 3. Emotion filtering: Fast WHERE emotion_tag = 'value' queries
-- 4. User timeline: Fast sorting by created_at with user filter  
-- 5. Combined filtering: User + emotion queries with optimal performance 