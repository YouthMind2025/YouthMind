-- V24: Create user_memory_preferences table for AI memory system key-value preferences
-- This separates AI memory preferences from application user preferences

-- Create user_memory_preferences table for AI memory system
CREATE TABLE user_memory_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    pref_key VARCHAR(255) NOT NULL,
    pref_value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, pref_key)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_memory_pref_user_key ON user_memory_preferences(user_id, pref_key);
CREATE INDEX IF NOT EXISTS idx_memory_pref_user_id ON user_memory_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_memory_pref_updated ON user_memory_preferences(updated_at DESC);

-- Add comments for documentation
COMMENT ON TABLE user_memory_preferences IS 'AI memory system key-value preferences for dynamic user context';
COMMENT ON COLUMN user_memory_preferences.pref_key IS 'Dynamic preference key for AI memory system';
COMMENT ON COLUMN user_memory_preferences.pref_value IS 'Dynamic preference value for AI memory system';
COMMENT ON COLUMN user_memory_preferences.updated_at IS 'Last update timestamp for preference';

-- Create function to migrate any existing data from user_preferences if needed
-- (This is a safety measure in case there was any data in the wrong table)
CREATE OR REPLACE FUNCTION migrate_memory_preferences_if_needed()
RETURNS INTEGER AS $$
DECLARE
    migrated_count INTEGER := 0;
BEGIN
    -- This function is a placeholder for any future data migration needs
    -- Currently, the tables serve different purposes so no migration is needed
    RETURN migrated_count;
END;
$$ LANGUAGE plpgsql;

-- Add comment for the migration function
COMMENT ON FUNCTION migrate_memory_preferences_if_needed IS 'Placeholder function for any future memory preferences migration needs';
