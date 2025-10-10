-- V22: Add enhanced moderation and content management fields

-- Add new fields to posts table for enhanced moderation
ALTER TABLE posts 
ADD COLUMN masked_content TEXT,
ADD COLUMN moderation_layer VARCHAR(50),
ADD COLUMN moderation_confidence DECIMAL(5,4) DEFAULT 0.0000,
ADD COLUMN moderation_details JSONB,
ADD COLUMN content_hash VARCHAR(64),
ADD COLUMN original_content_backup TEXT;

-- Add user consent tracking for moderation warnings
ALTER TABLE users
ADD COLUMN user_consent_given BOOLEAN DEFAULT FALSE,
ADD COLUMN consent_given_at TIMESTAMP,
ADD COLUMN consent_version VARCHAR(20) DEFAULT '1.0',
ADD COLUMN moderation_preferences JSONB DEFAULT '{}',
ADD COLUMN content_filtering_level VARCHAR(20) DEFAULT 'STANDARD' CHECK (content_filtering_level IN ('STRICT', 'STANDARD', 'RELAXED'));

-- Create moderation_logs table for detailed tracking
CREATE TABLE moderation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('POST', 'COMMENT', 'USER', 'MESSAGE')),
    entity_id UUID NOT NULL,
    user_id UUID,
    moderation_layer VARCHAR(50) NOT NULL,
    action_taken VARCHAR(30) NOT NULL CHECK (action_taken IN ('FLAGGED', 'MASKED', 'REMOVED', 'APPROVED', 'ESCALATED')),
    confidence_score DECIMAL(5,4),
    reason_code VARCHAR(50),
    reason_details TEXT,
    moderator_id UUID,
    automated BOOLEAN DEFAULT TRUE,
    appeal_status VARCHAR(20) DEFAULT 'NONE' CHECK (appeal_status IN ('NONE', 'PENDING', 'APPROVED', 'REJECTED')),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create content_safety_cache table for performance
CREATE TABLE content_safety_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_hash VARCHAR(64) UNIQUE NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    safety_score DECIMAL(5,4) NOT NULL,
    categories JSONB,
    moderation_result VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_posts_moderation_layer ON posts(moderation_layer);
CREATE INDEX IF NOT EXISTS idx_posts_content_hash ON posts(content_hash);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_entity ON moderation_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_user_id ON moderation_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_layer ON moderation_logs(moderation_layer);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_created_at ON moderation_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_content_safety_cache_hash ON content_safety_cache(content_hash);
CREATE INDEX IF NOT EXISTS idx_content_safety_cache_expires ON content_safety_cache(expires_at);

-- Create function to generate content hash
CREATE OR REPLACE FUNCTION generate_content_hash(content TEXT)
RETURNS VARCHAR(64) AS $$
BEGIN
    RETURN encode(digest(content, 'sha256'), 'hex');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Create trigger to auto-generate content hash for posts
CREATE OR REPLACE FUNCTION update_post_content_hash()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.content IS NOT NULL THEN
        NEW.content_hash = generate_content_hash(NEW.content);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_post_content_hash
    BEFORE INSERT OR UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_post_content_hash();

-- Update existing posts with content hashes
UPDATE posts SET content_hash = generate_content_hash(content) WHERE content_hash IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN posts.masked_content IS 'Content with sensitive parts masked/redacted';
COMMENT ON COLUMN posts.moderation_layer IS 'Which moderation system flagged this content (AI_PRIMARY, AI_SECONDARY, HUMAN, etc.)';
COMMENT ON COLUMN posts.moderation_confidence IS 'Confidence score of moderation decision (0.0-1.0)';
COMMENT ON COLUMN posts.moderation_details IS 'JSON details about moderation decision';
COMMENT ON COLUMN posts.content_hash IS 'SHA-256 hash of content for caching and deduplication';
COMMENT ON COLUMN posts.original_content_backup IS 'Backup of original content before any modifications';

COMMENT ON COLUMN users.user_consent_given IS 'Whether user has given consent for content moderation';
COMMENT ON COLUMN users.consent_given_at IS 'When user gave consent for moderation';
COMMENT ON COLUMN users.consent_version IS 'Version of consent terms user agreed to';
COMMENT ON COLUMN users.moderation_preferences IS 'User preferences for content moderation';
COMMENT ON COLUMN users.content_filtering_level IS 'User preferred content filtering level';

COMMENT ON TABLE moderation_logs IS 'Comprehensive log of all moderation actions taken';
COMMENT ON TABLE content_safety_cache IS 'Cache for content safety analysis results';
