-- V42: Add AI emotion classification fields to posts table
-- This migration adds support for AI-powered emotion classification
-- alongside the existing user-provided emotion field

-- Add AI emotion classification fields
ALTER TABLE posts 
ADD COLUMN ai_emotion VARCHAR(100),
ADD COLUMN emotion_confidence DECIMAL(3,2),
ADD COLUMN emotion_source VARCHAR(50) DEFAULT 'user';

-- Add comments explaining each field
COMMENT ON COLUMN posts.ai_emotion IS 'AI-detected emotion classification result';
COMMENT ON COLUMN posts.emotion_confidence IS 'AI confidence score for emotion classification (0.00 to 1.00)';
COMMENT ON COLUMN posts.emotion_source IS 'Source of emotion: user, ai_classified, or ai_fallback';

-- Add indexes for efficient querying
CREATE INDEX idx_posts_ai_emotion ON posts(ai_emotion);
CREATE INDEX idx_posts_emotion_source ON posts(emotion_source);

-- Add check constraint for emotion confidence range
ALTER TABLE posts 
ADD CONSTRAINT chk_posts_emotion_confidence_range 
CHECK (emotion_confidence IS NULL OR (emotion_confidence >= 0.00 AND emotion_confidence <= 1.00));

-- Update existing posts to set emotion_source = 'user' where emotion IS NOT NULL
UPDATE posts 
SET emotion_source = 'user' 
WHERE emotion IS NOT NULL AND emotion_source = 'user';

-- Add comment to the table
COMMENT ON TABLE posts IS 'Posts table with support for both user-provided and AI-classified emotions';
