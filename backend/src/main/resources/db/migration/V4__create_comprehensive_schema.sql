-- Update existing tables with new fields

-- Update users table
ALTER TABLE users 
ADD COLUMN username VARCHAR(255) UNIQUE,
ADD COLUMN bio TEXT,
ADD COLUMN avatar VARCHAR(500),
ADD COLUMN posts_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN likes_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN bookmarks_count INTEGER NOT NULL DEFAULT 0;

-- Update posts table
ALTER TABLE posts 
ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT 'Untitled Post',
ADD COLUMN topics TEXT,
ADD COLUMN pre_img VARCHAR(500),
ADD COLUMN likes INTEGER NOT NULL DEFAULT 0,
ADD COLUMN bookmarks INTEGER NOT NULL DEFAULT 0,
ADD COLUMN views INTEGER NOT NULL DEFAULT 0;

-- Create new tables

-- Comments table
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    likes INTEGER NOT NULL DEFAULT 0,
    parent_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- Drafts table
CREATE TABLE drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    emotion VARCHAR(100),
    topics TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    text TEXT NOT NULL,
    sender VARCHAR(10) NOT NULL CHECK (sender IN ('user', 'ai')),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- User engagements table
CREATE TABLE user_engagements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('RESONANCE', 'COLLECT', 'VIEW')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    UNIQUE(user_id, post_id, type)
);

-- Comment engagements table
CREATE TABLE comment_engagements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    comment_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('LIKE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    UNIQUE(user_id, comment_id, type)
);

-- Create indexes for performance
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);

CREATE INDEX idx_drafts_user_id ON drafts(user_id);
CREATE INDEX idx_drafts_created_at ON drafts(created_at);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_is_pinned ON conversations(is_pinned);
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);

CREATE INDEX idx_user_engagements_user_id ON user_engagements(user_id);
CREATE INDEX idx_user_engagements_post_id ON user_engagements(post_id);
CREATE INDEX idx_user_engagements_type ON user_engagements(type);

CREATE INDEX idx_comment_engagements_user_id ON comment_engagements(user_id);
CREATE INDEX idx_comment_engagements_comment_id ON comment_engagements(comment_id);

-- Update existing indexes for posts
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_emotion_tag ON posts(emotion_tag);
CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_posts_likes ON posts(likes);
CREATE INDEX idx_posts_views ON posts(views);

-- Update existing indexes for users
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
