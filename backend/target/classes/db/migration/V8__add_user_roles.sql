-- Add role field to users table for admin functionality
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'));

-- Create index for role-based queries
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Note: Admin user creation is handled by DataInitializer.java for better flexibility
