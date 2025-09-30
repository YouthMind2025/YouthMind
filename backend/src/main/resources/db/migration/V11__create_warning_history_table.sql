-- Create warning_history table for tracking user warnings
CREATE TABLE warning_history (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL,
    warning_type VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    moderator_id UUID NULL,
    post_id UUID NULL,
    comment_id UUID NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NULL,
    acknowledged_at TIMESTAMP NULL,
    acknowledged_by UUID NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_warning_history_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_warning_history_moderator 
        FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_warning_history_post 
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE SET NULL,
    CONSTRAINT fk_warning_history_acknowledged_by 
        FOREIGN KEY (acknowledged_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_warning_type 
        CHECK (warning_type IN ('SPAM', 'HARASSMENT', 'INAPPROPRIATE_CONTENT', 'HATE_SPEECH', 'VIOLENCE', 'OTHER')),
    CONSTRAINT chk_warning_severity 
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Create indexes for better performance
CREATE INDEX idx_warning_history_user_id ON warning_history(user_id);
CREATE INDEX idx_warning_history_created_at ON warning_history(created_at);
CREATE INDEX idx_warning_history_type ON warning_history(warning_type);
CREATE INDEX idx_warning_history_severity ON warning_history(severity);
CREATE INDEX idx_warning_history_expires_at ON warning_history(expires_at);
