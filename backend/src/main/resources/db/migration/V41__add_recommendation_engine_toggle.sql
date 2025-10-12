-- Add recommendation engine toggle to users table
-- This allows users to switch between personalized recommendations and chronological feed

ALTER TABLE users 
ADD COLUMN recommendation_engine_enabled BOOLEAN NOT NULL DEFAULT true;

-- Add comment for documentation
COMMENT ON COLUMN users.recommendation_engine_enabled IS 
'Flag to enable/disable personalized recommendation engine. When true, user gets AI-powered personalized feed. When false, user gets chronological feed. Default is true to enable recommendations for all users.';

-- Create index for efficient querying
CREATE INDEX idx_users_recommendation_engine_enabled 
ON users (recommendation_engine_enabled);

-- Update existing users to have the default value (though DEFAULT should handle this)
UPDATE users 
SET recommendation_engine_enabled = true 
WHERE recommendation_engine_enabled IS NULL;
