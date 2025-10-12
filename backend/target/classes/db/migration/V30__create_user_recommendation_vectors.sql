-- Migration: Create user_recommendation_vectors table
-- Description: Creates the table for storing user recommendation vectors with emotion weights
-- Version: V37
-- Created: 2024-10-12

-- Create user_recommendation_vectors table
CREATE TABLE user_recommendation_vectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    version INTEGER NOT NULL,
    
    -- Emotion weights (0.0000 to 1.0000)
    sad_weight DECIMAL(5,4) NOT NULL DEFAULT 0.1000,
    fear_weight DECIMAL(5,4) NOT NULL DEFAULT 0.1000,
    angry_weight DECIMAL(5,4) NOT NULL DEFAULT 0.1000,
    happy_weight DECIMAL(5,4) NOT NULL DEFAULT 0.1000,
    conflicted_weight DECIMAL(5,4) NOT NULL DEFAULT 0.1000,
    surprised_weight DECIMAL(5,4) NOT NULL DEFAULT 0.1000,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_user_recommendation_vectors_version_positive CHECK (version > 0),
    CONSTRAINT chk_user_recommendation_vectors_sad_weight_range CHECK (sad_weight >= 0.0000 AND sad_weight <= 1.0000),
    CONSTRAINT chk_user_recommendation_vectors_fear_weight_range CHECK (fear_weight >= 0.0000 AND fear_weight <= 1.0000),
    CONSTRAINT chk_user_recommendation_vectors_angry_weight_range CHECK (angry_weight >= 0.0000 AND angry_weight <= 1.0000),
    CONSTRAINT chk_user_recommendation_vectors_happy_weight_range CHECK (happy_weight >= 0.0000 AND happy_weight <= 1.0000),
    CONSTRAINT chk_user_recommendation_vectors_conflicted_weight_range CHECK (conflicted_weight >= 0.0000 AND conflicted_weight <= 1.0000),
    CONSTRAINT chk_user_recommendation_vectors_surprised_weight_range CHECK (surprised_weight >= 0.0000 AND surprised_weight <= 1.0000)
);

-- Create indexes for efficient querying
CREATE INDEX idx_user_recommendation_vectors_user_id ON user_recommendation_vectors(user_id);
CREATE INDEX idx_user_recommendation_vectors_user_version ON user_recommendation_vectors(user_id, version);
CREATE INDEX idx_user_recommendation_vectors_created_at ON user_recommendation_vectors(created_at);
CREATE INDEX idx_user_recommendation_vectors_updated_at ON user_recommendation_vectors(updated_at);

-- Create unique constraint to prevent duplicate versions for the same user
CREATE UNIQUE INDEX idx_user_recommendation_vectors_user_version_unique ON user_recommendation_vectors(user_id, version);

-- Add foreign key constraint to users table (if users table exists)
-- Note: This assumes the users table exists with UUID primary key
-- ALTER TABLE user_recommendation_vectors 
-- ADD CONSTRAINT fk_user_recommendation_vectors_user_id 
-- FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add comments for documentation
COMMENT ON TABLE user_recommendation_vectors IS 'Stores user recommendation vectors with emotion-based weights for personalized content delivery';
COMMENT ON COLUMN user_recommendation_vectors.id IS 'Primary key - UUID';
COMMENT ON COLUMN user_recommendation_vectors.user_id IS 'Reference to the user this vector belongs to';
COMMENT ON COLUMN user_recommendation_vectors.version IS 'Version number of this recommendation vector (incremented on updates)';
COMMENT ON COLUMN user_recommendation_vectors.sad_weight IS 'Weight for sad emotion content (0.0000 to 1.0000)';
COMMENT ON COLUMN user_recommendation_vectors.fear_weight IS 'Weight for fear emotion content (0.0000 to 1.0000)';
COMMENT ON COLUMN user_recommendation_vectors.angry_weight IS 'Weight for angry emotion content (0.0000 to 1.0000)';
COMMENT ON COLUMN user_recommendation_vectors.happy_weight IS 'Weight for happy emotion content (0.0000 to 1.0000)';
COMMENT ON COLUMN user_recommendation_vectors.conflicted_weight IS 'Weight for conflicted emotion content (0.0000 to 1.0000)';
COMMENT ON COLUMN user_recommendation_vectors.surprised_weight IS 'Weight for surprised emotion content (0.0000 to 1.0000)';
COMMENT ON COLUMN user_recommendation_vectors.created_at IS 'Timestamp when this vector was created';
COMMENT ON COLUMN user_recommendation_vectors.updated_at IS 'Timestamp when this vector was last updated';
