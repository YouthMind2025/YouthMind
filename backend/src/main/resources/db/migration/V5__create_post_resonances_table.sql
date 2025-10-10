-- Create post_resonances table for tracking user likes/resonances
CREATE TABLE post_resonances (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    resonanced_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_post_resonances_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_resonances_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uk_post_resonances_user_post UNIQUE(user_id, post_id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_post_resonances_user_post ON post_resonances(user_id, post_id);
CREATE INDEX IF NOT EXISTS idx_post_resonances_post ON post_resonances(post_id);
CREATE INDEX IF NOT EXISTS idx_post_resonances_resonanced_at ON post_resonances(resonanced_at);

-- Add comments for documentation
COMMENT ON TABLE post_resonances IS 'Tracks user resonances (likes) for posts';
COMMENT ON COLUMN post_resonances.user_id IS 'User who resonanced (liked) the post';
COMMENT ON COLUMN post_resonances.post_id IS 'Post that was resonanced (liked)';
COMMENT ON COLUMN post_resonances.resonanced_at IS 'Timestamp when user resonanced the post'; 