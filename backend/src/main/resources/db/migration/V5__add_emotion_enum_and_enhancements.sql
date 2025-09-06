-- V5: Add emotion enum, user preferences, and additional schema enhancements

-- Create emotion enum type
CREATE TYPE emotion_type AS ENUM (
    'HAPPY', 'SAD', 'ANGRY', 'ANXIOUS', 'EXCITED', 'CALM', 
    'FRUSTRATED', 'HOPEFUL', 'LONELY', 'GRATEFUL', 'CONFUSED', 
    'OVERWHELMED', 'CONTENT', 'WORRIED', 'PROUD', 'DISAPPOINTED'
);

-- Update posts table to use enum and add missing columns
ALTER TABLE posts 
ADD COLUMN emotion emotion_type,
ADD COLUMN resonance_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN collect_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN ai_metadata JSONB,
ADD COLUMN flagged BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN flag_reason VARCHAR(500),
ADD COLUMN flagged_at TIMESTAMP,
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE;

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

-- Add soft delete support to users and comments
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE comments ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE drafts ADD COLUMN deleted_at TIMESTAMP;

-- Add audit trail columns
ALTER TABLE posts 
ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
ADD COLUMN last_modified_by UUID,
ADD CONSTRAINT fk_posts_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES users(id);

ALTER TABLE users 
ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
ADD COLUMN last_login_at TIMESTAMP,
ADD COLUMN login_count INTEGER NOT NULL DEFAULT 0;

-- Create audit_logs table for critical changes
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'RESTORE')),
    user_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create search optimization table for full-text search
CREATE TABLE search_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('POST', 'USER', 'COMMENT')),
    entity_id UUID NOT NULL,
    search_vector tsvector,
    content_hash VARCHAR(64), -- For cache invalidation
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id)
);

-- Performance indexes
CREATE INDEX idx_posts_emotion ON posts(emotion);
CREATE INDEX idx_posts_resonance_count ON posts(resonance_count);
CREATE INDEX idx_posts_collect_count ON posts(collect_count);
CREATE INDEX idx_posts_flagged ON posts(flagged);
CREATE INDEX idx_posts_deleted_at ON posts(deleted_at);
CREATE INDEX idx_posts_is_anonymous ON posts(is_anonymous);

CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);

CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);

CREATE INDEX idx_audit_logs_entity_type_id ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Full-text search indexes
CREATE INDEX idx_search_vector ON search_index USING gin(search_vector);
CREATE INDEX idx_search_entity_type ON search_index(entity_type);

-- Composite indexes for common queries
CREATE INDEX idx_posts_user_emotion_date ON posts(user_id, emotion, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_posts_trending ON posts(created_at, resonance_count DESC, views DESC) WHERE deleted_at IS NULL AND flagged = FALSE;
CREATE INDEX idx_user_engagements_user_post_type ON user_engagements(user_id, post_id, type);

-- Add constraints
ALTER TABLE posts ADD CONSTRAINT chk_posts_counts_positive CHECK (
    likes >= 0 AND bookmarks >= 0 AND views >= 0 AND 
    resonance_count >= 0 AND collect_count >= 0
);

ALTER TABLE users ADD CONSTRAINT chk_users_counts_positive CHECK (
    posts_count >= 0 AND likes_count >= 0 AND bookmarks_count >= 0 AND login_count >= 0
);

-- Migrate existing emotion_tag data to emotion enum
UPDATE posts 
SET emotion = CASE 
    WHEN UPPER(emotion_tag) = 'HAPPY' THEN 'HAPPY'::emotion_type
    WHEN UPPER(emotion_tag) = 'SAD' THEN 'SAD'::emotion_type
    WHEN UPPER(emotion_tag) = 'ANGRY' THEN 'ANGRY'::emotion_type
    WHEN UPPER(emotion_tag) = 'ANXIOUS' THEN 'ANXIOUS'::emotion_type
    WHEN UPPER(emotion_tag) = 'EXCITED' THEN 'EXCITED'::emotion_type
    WHEN UPPER(emotion_tag) = 'CALM' THEN 'CALM'::emotion_type
    WHEN UPPER(emotion_tag) = 'FRUSTRATED' THEN 'FRUSTRATED'::emotion_type
    WHEN UPPER(emotion_tag) = 'HOPEFUL' THEN 'HOPEFUL'::emotion_type
    WHEN UPPER(emotion_tag) = 'LONELY' THEN 'LONELY'::emotion_type
    WHEN UPPER(emotion_tag) = 'GRATEFUL' THEN 'GRATEFUL'::emotion_type
    WHEN UPPER(emotion_tag) = 'CONFUSED' THEN 'CONFUSED'::emotion_type
    WHEN UPPER(emotion_tag) = 'OVERWHELMED' THEN 'OVERWHELMED'::emotion_type
    WHEN UPPER(emotion_tag) = 'CONTENT' THEN 'CONTENT'::emotion_type
    WHEN UPPER(emotion_tag) = 'WORRIED' THEN 'WORRIED'::emotion_type
    WHEN UPPER(emotion_tag) = 'PROUD' THEN 'PROUD'::emotion_type
    WHEN UPPER(emotion_tag) = 'DISAPPOINTED' THEN 'DISAPPOINTED'::emotion_type
    ELSE 'CONTENT'::emotion_type  -- Default for unmapped emotions
END
WHERE emotion_tag IS NOT NULL;

-- Create function for updating search vectors
CREATE OR REPLACE FUNCTION update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_TABLE_NAME = 'posts' THEN
        INSERT INTO search_index (entity_type, entity_id, search_vector, content_hash)
        VALUES (
            'POST', 
            NEW.id, 
            to_tsvector('english', COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.content, '') || ' ' || COALESCE(NEW.topics, '')),
            md5(COALESCE(NEW.title, '') || COALESCE(NEW.content, '') || COALESCE(NEW.topics, ''))
        )
        ON CONFLICT (entity_type, entity_id) 
        DO UPDATE SET 
            search_vector = to_tsvector('english', COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.content, '') || ' ' || COALESCE(NEW.topics, '')),
            content_hash = md5(COALESCE(NEW.title, '') || COALESCE(NEW.content, '') || COALESCE(NEW.topics, '')),
            updated_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for search vector updates
CREATE TRIGGER trigger_posts_search_vector
    AFTER INSERT OR UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

-- Create function for audit logging
CREATE OR REPLACE FUNCTION audit_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_logs (entity_type, entity_id, action, new_values)
        VALUES (TG_TABLE_NAME, NEW.id, 'CREATE', to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_logs (entity_type, entity_id, action, old_values, new_values)
        VALUES (TG_TABLE_NAME, NEW.id, 'UPDATE', to_jsonb(OLD), to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_logs (entity_type, entity_id, action, old_values)
        VALUES (TG_TABLE_NAME, OLD.id, 'DELETE', to_jsonb(OLD));
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create audit triggers for critical tables
CREATE TRIGGER trigger_posts_audit
    AFTER INSERT OR UPDATE OR DELETE ON posts
    FOR EACH ROW EXECUTE FUNCTION audit_changes();

CREATE TRIGGER trigger_users_audit
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_changes();
