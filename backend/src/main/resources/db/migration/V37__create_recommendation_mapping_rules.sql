-- V37: Create recommendation_mapping_rules table for emotion combination to percentage mapping
-- This migration creates the table for storing recommendation mapping rules

-- Table for storing recommendation mapping rules
CREATE TABLE recommendation_mapping_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emotion_combination VARCHAR(255) NOT NULL UNIQUE,
    sad_percentage INTEGER NOT NULL CHECK (sad_percentage >= 0 AND sad_percentage <= 100),
    fear_percentage INTEGER NOT NULL CHECK (fear_percentage >= 0 AND fear_percentage <= 100),
    angry_percentage INTEGER NOT NULL CHECK (angry_percentage >= 0 AND angry_percentage <= 100),
    happy_percentage INTEGER NOT NULL CHECK (happy_percentage >= 0 AND happy_percentage <= 100),
    conflicted_percentage INTEGER NOT NULL CHECK (conflicted_percentage >= 0 AND conflicted_percentage <= 100),
    surprised_percentage INTEGER NOT NULL CHECK (surprised_percentage >= 0 AND surprised_percentage <= 100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Additional constraint to ensure total percentage equals 100
    CONSTRAINT chk_total_percentage_equals_100 CHECK (
        (sad_percentage + fear_percentage + angry_percentage + 
         happy_percentage + conflicted_percentage + surprised_percentage) = 100
    )
);

-- Create indexes for performance
CREATE INDEX idx_recommendation_mapping_emotion_combination ON recommendation_mapping_rules(emotion_combination);
CREATE INDEX idx_recommendation_mapping_created_at ON recommendation_mapping_rules(created_at);

-- Create unique constraint on emotion_combination (already included in table definition but explicit for clarity)
-- UNIQUE constraint is already defined in the column definition above

-- Add comments for documentation
COMMENT ON TABLE recommendation_mapping_rules IS 'Stores mapping rules from emotion combinations to percentage distributions for content recommendations';
COMMENT ON COLUMN recommendation_mapping_rules.id IS 'Primary key UUID for the mapping rule';
COMMENT ON COLUMN recommendation_mapping_rules.emotion_combination IS 'Sorted emotion combination key (e.g., "angry,fear,sad")';
COMMENT ON COLUMN recommendation_mapping_rules.sad_percentage IS 'Percentage allocation for sad emotion content (0-100)';
COMMENT ON COLUMN recommendation_mapping_rules.fear_percentage IS 'Percentage allocation for fear emotion content (0-100)';
COMMENT ON COLUMN recommendation_mapping_rules.angry_percentage IS 'Percentage allocation for angry emotion content (0-100)';
COMMENT ON COLUMN recommendation_mapping_rules.happy_percentage IS 'Percentage allocation for happy emotion content (0-100)';
COMMENT ON COLUMN recommendation_mapping_rules.conflicted_percentage IS 'Percentage allocation for conflicted emotion content (0-100)';
COMMENT ON COLUMN recommendation_mapping_rules.surprised_percentage IS 'Percentage allocation for surprised emotion content (0-100)';
COMMENT ON COLUMN recommendation_mapping_rules.created_at IS 'Timestamp when the mapping rule was created';
COMMENT ON COLUMN recommendation_mapping_rules.updated_at IS 'Timestamp when the mapping rule was last updated';




