-- Create articles table for website content management
CREATE TABLE articles (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    intro VARCHAR(500),
    cover_image VARCHAR(500),
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_articles_published ON articles(published);
CREATE INDEX IF NOT EXISTS idx_articles_created_at ON articles(created_at);
CREATE INDEX IF NOT EXISTS idx_articles_title ON articles(title);

-- Create full-text search index for content search
CREATE INDEX IF NOT EXISTS idx_articles_content_search ON articles USING GIN (to_tsvector('english', title || ' ' || content || ' ' || COALESCE(intro, '')));
