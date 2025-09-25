-- Migration V17: Add data integrity constraints and final database optimizations
-- This migration adds constraints to ensure data integrity and consistency

-- Add check constraints for data validation
-- These constraints ensure data quality and prevent invalid data entry

-- Users table constraints
ALTER TABLE users ADD CONSTRAINT chk_users_posts_count CHECK (posts_count >= 0);
ALTER TABLE users ADD CONSTRAINT chk_users_likes_count CHECK (likes_count >= 0);
ALTER TABLE users ADD CONSTRAINT chk_users_bookmarks_count CHECK (bookmarks_count >= 0);
ALTER TABLE users ADD CONSTRAINT chk_users_warnings_count CHECK (warnings_count >= 0);
ALTER TABLE users ADD CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
ALTER TABLE users ADD CONSTRAINT chk_users_username_length CHECK (LENGTH(username) >= 3 AND LENGTH(username) <= 50);
ALTER TABLE users ADD CONSTRAINT chk_users_first_name_length CHECK (LENGTH(first_name) <= 100);
ALTER TABLE users ADD CONSTRAINT chk_users_last_name_length CHECK (LENGTH(last_name) <= 100);
ALTER TABLE users ADD CONSTRAINT chk_users_bio_length CHECK (LENGTH(bio) <= 1000);

-- Posts table constraints
ALTER TABLE posts ADD CONSTRAINT chk_posts_resonance CHECK (resonance >= 0);
ALTER TABLE posts ADD CONSTRAINT chk_posts_view_count CHECK (view_count >= 0);
ALTER TABLE posts ADD CONSTRAINT chk_posts_collect_count CHECK (collect_count >= 0);
ALTER TABLE posts ADD CONSTRAINT chk_posts_title_length CHECK (LENGTH(title) >= 1 AND LENGTH(title) <= 255);
ALTER TABLE posts ADD CONSTRAINT chk_posts_content_length CHECK (LENGTH(content) >= 1 AND LENGTH(content) <= 10000);
ALTER TABLE posts ADD CONSTRAINT chk_posts_emotion_length CHECK (LENGTH(emotion) <= 100);
ALTER TABLE posts ADD CONSTRAINT chk_posts_topics_length CHECK (LENGTH(topics) <= 1000);

-- Comments table constraints
ALTER TABLE comments ADD CONSTRAINT chk_comments_likes CHECK (likes >= 0);
ALTER TABLE comments ADD CONSTRAINT chk_comments_content_length CHECK (LENGTH(content) >= 1 AND LENGTH(content) <= 5000);

-- Drafts table constraints
ALTER TABLE drafts ADD CONSTRAINT chk_drafts_title_length CHECK (LENGTH(title) >= 1 AND LENGTH(title) <= 255);
ALTER TABLE drafts ADD CONSTRAINT chk_drafts_content_length CHECK (LENGTH(content) >= 1 AND LENGTH(content) <= 10000);
ALTER TABLE drafts ADD CONSTRAINT chk_drafts_emotion_length CHECK (LENGTH(emotion) <= 100);
ALTER TABLE drafts ADD CONSTRAINT chk_drafts_topics_length CHECK (LENGTH(topics) <= 1000);

-- Conversations table constraints
ALTER TABLE conversations ADD CONSTRAINT chk_conversations_title_length CHECK (LENGTH(title) >= 1 AND LENGTH(title) <= 255);

-- Messages table constraints
ALTER TABLE messages ADD CONSTRAINT chk_messages_text_length CHECK (LENGTH(text) >= 1 AND LENGTH(text) <= 5000);

-- Articles table constraints
ALTER TABLE articles ADD CONSTRAINT chk_articles_title_length CHECK (LENGTH(title) >= 1 AND LENGTH(title) <= 255);
ALTER TABLE articles ADD CONSTRAINT chk_articles_content_length CHECK (LENGTH(content) >= 1 AND LENGTH(content) <= 50000);
ALTER TABLE articles ADD CONSTRAINT chk_articles_intro_length CHECK (LENGTH(intro) <= 1000);

-- AI API Usage table constraints
ALTER TABLE ai_api_usage ADD CONSTRAINT chk_ai_api_usage_prompt_tokens CHECK (prompt_tokens >= 0);
ALTER TABLE ai_api_usage ADD CONSTRAINT chk_ai_api_usage_completion_tokens CHECK (completion_tokens >= 0);
ALTER TABLE ai_api_usage ADD CONSTRAINT chk_ai_api_usage_total_tokens CHECK (total_tokens >= 0);
ALTER TABLE ai_api_usage ADD CONSTRAINT chk_ai_api_usage_cost_per_token CHECK (cost_per_token >= 0);
ALTER TABLE ai_api_usage ADD CONSTRAINT chk_ai_api_usage_total_cost CHECK (total_cost >= 0);
ALTER TABLE ai_api_usage ADD CONSTRAINT chk_ai_api_usage_request_duration CHECK (request_duration_ms >= 0);
ALTER TABLE ai_api_usage ADD CONSTRAINT chk_ai_api_usage_payload_sizes CHECK (request_payload_size >= 0 AND response_payload_size >= 0);

-- Warning History table constraints
ALTER TABLE warning_history ADD CONSTRAINT chk_warning_history_reason_length CHECK (LENGTH(reason) >= 1 AND LENGTH(reason) <= 1000);

-- User Moderation Status table constraints
ALTER TABLE user_moderation_status ADD CONSTRAINT chk_user_moderation_warning_level CHECK (warning_level >= 0 AND warning_level <= 5);
ALTER TABLE user_moderation_status ADD CONSTRAINT chk_user_moderation_total_warnings CHECK (total_warnings >= 0);
ALTER TABLE user_moderation_status ADD CONSTRAINT chk_user_moderation_total_bans CHECK (total_bans >= 0);
ALTER TABLE user_moderation_status ADD CONSTRAINT chk_user_moderation_notes_length CHECK (LENGTH(notes) <= 2000);

-- Add unique constraints to prevent duplicates
-- These constraints ensure data uniqueness where required

-- Ensure unique usernames
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);

-- Ensure unique email addresses (already exists but ensuring it's there)
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

-- Ensure unique user-post engagements
ALTER TABLE user_engagements ADD CONSTRAINT uk_user_engagements_user_post_type UNIQUE (user_id, post_id, type);

-- Ensure unique user-comment engagements
ALTER TABLE comment_engagements ADD CONSTRAINT uk_comment_engagements_user_comment_type UNIQUE (user_id, comment_id, type);

-- Ensure unique user-post collections
ALTER TABLE post_collections ADD CONSTRAINT uk_post_collections_user_post UNIQUE (user_id, post_id);

-- Ensure unique user-post resonances
ALTER TABLE post_resonances ADD CONSTRAINT uk_post_resonances_user_post UNIQUE (user_id, post_id);

-- Ensure unique user-post views
ALTER TABLE post_views ADD CONSTRAINT uk_post_views_user_post UNIQUE (user_id, post_id);

-- Ensure unique user moderation status (one per user)
ALTER TABLE user_moderation_status ADD CONSTRAINT uk_user_moderation_status_user UNIQUE (user_id);

-- Add foreign key constraints to ensure referential integrity
-- These constraints ensure that all foreign key references are valid

-- Posts foreign key constraints
ALTER TABLE posts ADD CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Comments foreign key constraints
ALTER TABLE comments ADD CONSTRAINT fk_comments_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;
ALTER TABLE comments ADD CONSTRAINT fk_comments_author_id FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE comments ADD CONSTRAINT fk_comments_parent_id FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE;

-- Drafts foreign key constraints
ALTER TABLE drafts ADD CONSTRAINT fk_drafts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Conversations foreign key constraints
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Messages foreign key constraints
ALTER TABLE messages ADD CONSTRAINT fk_messages_conversation_id FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE;

-- User engagements foreign key constraints
ALTER TABLE user_engagements ADD CONSTRAINT fk_user_engagements_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_engagements ADD CONSTRAINT fk_user_engagements_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- Comment engagements foreign key constraints
ALTER TABLE comment_engagements ADD CONSTRAINT fk_comment_engagements_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE comment_engagements ADD CONSTRAINT fk_comment_engagements_comment_id FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE;

-- Post collections foreign key constraints
ALTER TABLE post_collections ADD CONSTRAINT fk_post_collections_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE post_collections ADD CONSTRAINT fk_post_collections_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- Post resonances foreign key constraints
ALTER TABLE post_resonances ADD CONSTRAINT fk_post_resonances_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE post_resonances ADD CONSTRAINT fk_post_resonances_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- Post views foreign key constraints
ALTER TABLE post_views ADD CONSTRAINT fk_post_views_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE post_views ADD CONSTRAINT fk_post_views_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- Warning history foreign key constraints
ALTER TABLE warning_history ADD CONSTRAINT fk_warning_history_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE warning_history ADD CONSTRAINT fk_warning_history_moderator_id FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE warning_history ADD CONSTRAINT fk_warning_history_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE SET NULL;
ALTER TABLE warning_history ADD CONSTRAINT fk_warning_history_acknowledged_by FOREIGN KEY (acknowledged_by) REFERENCES users(id) ON DELETE SET NULL;

-- User moderation status foreign key constraints
ALTER TABLE user_moderation_status ADD CONSTRAINT fk_user_moderation_status_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- AI API usage foreign key constraints
ALTER TABLE ai_api_usage ADD CONSTRAINT fk_ai_api_usage_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- AI analysis foreign key constraints
ALTER TABLE ai_analysis ADD CONSTRAINT fk_ai_analysis_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- Add triggers for automatic timestamp updates
-- These triggers ensure that updated_at fields are automatically maintained

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Add triggers for tables with updated_at fields
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_posts_updated_at BEFORE UPDATE ON posts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_drafts_updated_at BEFORE UPDATE ON drafts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_articles_updated_at BEFORE UPDATE ON articles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ai_api_usage_updated_at BEFORE UPDATE ON ai_api_usage FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add triggers for automatic count updates
-- These triggers ensure that count fields are automatically maintained

-- Function to update user post count
CREATE OR REPLACE FUNCTION update_user_posts_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE users SET posts_count = posts_count + 1 WHERE id = NEW.user_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE users SET posts_count = posts_count - 1 WHERE id = OLD.user_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

-- Add triggers for post count updates
CREATE TRIGGER update_user_posts_count_insert AFTER INSERT ON posts FOR EACH ROW EXECUTE FUNCTION update_user_posts_count();
CREATE TRIGGER update_user_posts_count_delete AFTER DELETE ON posts FOR EACH ROW EXECUTE FUNCTION update_user_posts_count();

-- Add comments for documentation
COMMENT ON CONSTRAINT chk_users_email_format ON users IS 'Ensures email addresses are in valid format';
COMMENT ON CONSTRAINT chk_users_username_length ON users IS 'Ensures usernames are between 3 and 50 characters';
COMMENT ON CONSTRAINT chk_posts_title_length ON posts IS 'Ensures post titles are between 1 and 255 characters';
COMMENT ON CONSTRAINT chk_posts_content_length ON posts IS 'Ensures post content is between 1 and 10000 characters';
COMMENT ON CONSTRAINT chk_comments_content_length ON comments IS 'Ensures comment content is between 1 and 5000 characters';
COMMENT ON CONSTRAINT chk_ai_api_usage_total_cost ON ai_api_usage IS 'Ensures AI API costs are non-negative';
COMMENT ON CONSTRAINT uk_users_username ON users IS 'Ensures usernames are unique';
COMMENT ON CONSTRAINT uk_user_engagements_user_post_type ON user_engagements IS 'Prevents duplicate user-post engagements';
COMMENT ON CONSTRAINT uk_post_views_user_post ON post_views IS 'Prevents duplicate user-post views';
COMMENT ON CONSTRAINT fk_posts_user_id ON posts IS 'Ensures post user references are valid';
COMMENT ON CONSTRAINT fk_comments_post_id ON comments IS 'Ensures comment post references are valid';
COMMENT ON CONSTRAINT fk_warning_history_user_id ON warning_history IS 'Ensures warning user references are valid';
COMMENT ON CONSTRAINT fk_ai_api_usage_user_id ON ai_api_usage IS 'Ensures AI API usage user references are valid';
