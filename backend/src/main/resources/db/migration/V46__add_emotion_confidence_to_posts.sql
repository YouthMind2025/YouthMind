-- V46: Add emotion classification confidence fields to posts table
-- Description: Adds confidence score and source tracking for emotion classifications

-- Add emotion confidence column (0.0000 to 1.0000)
ALTER TABLE posts
ADD COLUMN IF NOT EXISTS emotion_confidence DECIMAL(5,4) DEFAULT NULL;

-- Add emotion source column
ALTER TABLE posts
ADD COLUMN IF NOT EXISTS emotion_source VARCHAR(50) DEFAULT 'user';

-- Add comments for documentation
COMMENT ON COLUMN posts.emotion_confidence IS 'Confidence score (0.0-1.0) for emotion classification';
COMMENT ON COLUMN posts.emotion_source IS 'Source of emotion classification: user_selected, ai_classified, ai_classified_backfill, default';

-- Create index for querying by confidence (for quality filtering)
CREATE INDEX IF NOT EXISTS idx_posts_emotion_confidence 
ON posts(emotion_confidence DESC) 
WHERE emotion_confidence IS NOT NULL;

-- Create index for querying by source
CREATE INDEX IF NOT EXISTS idx_posts_emotion_source 
ON posts(emotion_source);

-- Create composite index for emotion analysis queries
CREATE INDEX IF NOT EXISTS idx_posts_emotion_confidence_source 
ON posts(emotion, emotion_confidence, emotion_source)
WHERE emotion IS NOT NULL;

