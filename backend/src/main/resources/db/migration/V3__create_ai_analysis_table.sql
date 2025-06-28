-- Create ai_analysis table
CREATE TABLE ai_analysis (
    post_id UUID PRIMARY KEY,
    sentiment_score DECIMAL(3,2),
    empathy_score DECIMAL(3,2),
    generated_questions JSONB,
    processing_time_ms INTEGER,
    CONSTRAINT fk_ai_analysis_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- Create index for performant retrieval queries
CREATE INDEX idx_ai_analysis_post_id ON ai_analysis(post_id); 