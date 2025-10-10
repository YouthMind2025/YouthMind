-- Create user_moderation_status table for tracking user moderation state
CREATE TABLE user_moderation_status (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'CLEAN',
    warning_level INTEGER NOT NULL DEFAULT 0,
    last_warning_at TIMESTAMP NULL,
    last_moderation_at TIMESTAMP NULL,
    total_warnings INTEGER NOT NULL DEFAULT 0,
    total_bans INTEGER NOT NULL DEFAULT 0,
    auto_moderation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    manual_review_required BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraint
    CONSTRAINT fk_user_moderation_status_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT chk_moderation_status 
        CHECK (status IN ('CLEAN', 'WATCHED', 'WARNED', 'RESTRICTED', 'BANNED')),
    CONSTRAINT chk_warning_level 
        CHECK (warning_level >= 0 AND warning_level <= 5)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_moderation_status_user_id ON user_moderation_status(user_id);
CREATE INDEX IF NOT EXISTS idx_user_moderation_status_status ON user_moderation_status(status);
CREATE INDEX IF NOT EXISTS idx_user_moderation_status_warning_level ON user_moderation_status(warning_level);
CREATE INDEX IF NOT EXISTS idx_user_moderation_status_manual_review ON user_moderation_status(manual_review_required);
CREATE INDEX IF NOT EXISTS idx_user_moderation_status_updated_at ON user_moderation_status(updated_at);
