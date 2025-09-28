-- Create post_collections table for tracking user collections/saves
CREATE TABLE post_collections (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_post_collections_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_collections_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uk_post_collections_user_post UNIQUE(user_id, post_id)
);

-- Create indexes for better performance
CREATE INDEX idx_post_collections_user_post ON post_collections(user_id, post_id);
CREATE INDEX idx_post_collections_post ON post_collections(post_id);
CREATE INDEX idx_post_collections_collected_at ON post_collections(collected_at);

-- Add comments for documentation
COMMENT ON TABLE post_collections IS 'Tracks user collections (saves) for posts';
COMMENT ON COLUMN post_collections.user_id IS 'User who collected (saved) the post';
COMMENT ON COLUMN post_collections.post_id IS 'Post that was collected (saved)';
COMMENT ON COLUMN post_collections.collected_at IS 'Timestamp when user collected the post'; 