-- Migration V2: Add performance indexes for query optimization
-- Task 3: Query Performance Optimization

-- Add index for users.email_hash (login lookup optimization)
-- This index will dramatically improve performance for:
-- - findByEmailHash(String emailHash) 
-- - existsByEmailHash(String emailHash)
CREATE INDEX IF NOT EXISTS idx_users_email_hash ON users(email_hash);

-- Add composite index for posts filtering by user and emotion (advanced optimization)
CREATE INDEX IF NOT EXISTS idx_posts_user_emotion ON posts(user_id, emotion_tag);

-- Add composite index for posts feed queries (user + created_at for timeline)
CREATE INDEX IF NOT EXISTS idx_posts_user_created_at ON posts(user_id, created_at DESC);

-- Performance analysis comment
-- These indexes will optimize:
-- 1. Login queries: O(1) vs O(n) lookup on email_hash
-- 2. User timeline: Fast sorting by created_at with user filter  
-- 3. Emotion filtering: Combined user + emotion queries 