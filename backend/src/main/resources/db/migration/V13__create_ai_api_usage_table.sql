-- Create ai_api_usage table for tracking AI API costs and usage
CREATE TABLE ai_api_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    session_id VARCHAR(255),
    request_type VARCHAR(50) NOT NULL, -- 'MODERATION', 'CONTENT_GENERATION', etc.
    model_name VARCHAR(100) NOT NULL, -- 'deepseek-chat', 'gpt-4', etc.
    prompt_tokens INTEGER NOT NULL DEFAULT 0,
    completion_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    cost_per_token DECIMAL(10, 8) NOT NULL DEFAULT 0.00000000,
    total_cost DECIMAL(10, 6) NOT NULL DEFAULT 0.000000,
    request_duration_ms INTEGER NOT NULL DEFAULT 0,
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,
    request_payload_size INTEGER DEFAULT 0,
    response_payload_size INTEGER DEFAULT 0,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_ai_api_usage_user_id ON ai_api_usage(user_id);
CREATE INDEX idx_ai_api_usage_created_at ON ai_api_usage(created_at);
CREATE INDEX idx_ai_api_usage_request_type ON ai_api_usage(request_type);
CREATE INDEX idx_ai_api_usage_model_name ON ai_api_usage(model_name);
CREATE INDEX idx_ai_api_usage_success ON ai_api_usage(success);
CREATE INDEX idx_ai_api_usage_session_id ON ai_api_usage(session_id);

-- Create composite indexes for common queries
CREATE INDEX idx_ai_api_usage_user_date ON ai_api_usage(user_id, created_at);
CREATE INDEX idx_ai_api_usage_type_date ON ai_api_usage(request_type, created_at);
CREATE INDEX idx_ai_api_usage_model_date ON ai_api_usage(model_name, created_at);

-- Add comments for documentation
COMMENT ON TABLE ai_api_usage IS 'Tracks AI API usage, costs, and performance metrics for billing and monitoring';
COMMENT ON COLUMN ai_api_usage.user_id IS 'User who made the request (nullable for system requests)';
COMMENT ON COLUMN ai_api_usage.session_id IS 'Session identifier for grouping related requests';
COMMENT ON COLUMN ai_api_usage.request_type IS 'Type of AI request (MODERATION, CONTENT_GENERATION, etc.)';
COMMENT ON COLUMN ai_api_usage.model_name IS 'AI model used for the request';
COMMENT ON COLUMN ai_api_usage.prompt_tokens IS 'Number of tokens in the input prompt';
COMMENT ON COLUMN ai_api_usage.completion_tokens IS 'Number of tokens in the AI response';
COMMENT ON COLUMN ai_api_usage.total_tokens IS 'Total tokens used (prompt + completion)';
COMMENT ON COLUMN ai_api_usage.cost_per_token IS 'Cost per token for this model';
COMMENT ON COLUMN ai_api_usage.total_cost IS 'Total cost for this request';
COMMENT ON COLUMN ai_api_usage.request_duration_ms IS 'Request duration in milliseconds';
COMMENT ON COLUMN ai_api_usage.success IS 'Whether the request was successful';
COMMENT ON COLUMN ai_api_usage.error_message IS 'Error message if request failed';
COMMENT ON COLUMN ai_api_usage.request_payload_size IS 'Size of request payload in bytes';
COMMENT ON COLUMN ai_api_usage.response_payload_size IS 'Size of response payload in bytes';
COMMENT ON COLUMN ai_api_usage.ip_address IS 'IP address of the requester';
COMMENT ON COLUMN ai_api_usage.user_agent IS 'User agent string of the requester';
