-- Add warning system fields to users table
ALTER TABLE users 
ADD COLUMN warnings_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN user_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
ADD COLUMN ban_expires_at TIMESTAMP NULL;

-- Create index for user status queries
CREATE INDEX idx_users_status ON users(user_status);
CREATE INDEX idx_users_ban_expires ON users(ban_expires_at);

-- Add check constraint for user_status
ALTER TABLE users ADD CONSTRAINT chk_user_status 
CHECK (user_status IN ('ACTIVE', 'WARNED', 'BANNED', 'SUSPENDED'));
