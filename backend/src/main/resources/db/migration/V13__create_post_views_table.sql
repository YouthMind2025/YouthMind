-- Create post_views table for tracking unique user-post views
-- This ensures one user = one view per post for accurate view counting
CREATE TABLE post_views (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    viewed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_post_views_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_views_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uk_post_views_user_post UNIQUE(user_id, post_id)
);

-- Create indexes for better performance
CREATE INDEX idx_post_views_user_post ON post_views(user_id, post_id);
CREATE INDEX idx_post_views_post ON post_views(post_id);
CREATE INDEX idx_post_views_viewed_at ON post_views(viewed_at);

-- Add comment for documentation
COMMENT ON TABLE post_views IS 'Tracks unique user views per post to prevent view count inflation';
COMMENT ON COLUMN post_views.user_id IS 'User who viewed the post';
COMMENT ON COLUMN post_views.post_id IS 'Post that was viewed';
COMMENT ON COLUMN post_views.viewed_at IS 'Timestamp when user first viewed the post'; 