-- V21: Add user preferences table for enhanced user experience

-- Create user_preferences table
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT' CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM')),
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    privacy_level VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' CHECK (privacy_level IN ('PUBLIC', 'FRIENDS', 'PRIVATE')),
    ai_suggestions_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    anonymous_posting_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for fast user preference lookups
CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences(user_id);

-- Insert default preferences for existing users
INSERT INTO user_preferences (user_id, theme, language, notifications_enabled, email_notifications, push_notifications, privacy_level, ai_suggestions_enabled, anonymous_posting_allowed)
SELECT id, 'LIGHT', 'en', TRUE, TRUE, TRUE, 'PUBLIC', TRUE, FALSE
FROM users
WHERE id NOT IN (SELECT user_id FROM user_preferences WHERE user_id IS NOT NULL);

-- Add trigger to create default preferences for new users
CREATE OR REPLACE FUNCTION create_default_user_preferences()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_preferences (user_id, theme, language, notifications_enabled, email_notifications, push_notifications, privacy_level, ai_suggestions_enabled, anonymous_posting_allowed)
    VALUES (NEW.id, 'LIGHT', 'en', TRUE, TRUE, TRUE, 'PUBLIC', TRUE, FALSE);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_create_user_preferences
    AFTER INSERT ON users
    FOR EACH ROW EXECUTE FUNCTION create_default_user_preferences();
