-- Create user_consent table for tracking user consent for masked content
CREATE TABLE user_consent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content_id UUID NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    original_content TEXT,
    masked_content TEXT,
    final_content TEXT,
    masking_strategy VARCHAR(50),
    masking_options TEXT, -- JSON string of masking options
    requested_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_user_consent_user_id ON user_consent(user_id);
CREATE INDEX idx_user_consent_content_id ON user_consent(content_id);
CREATE INDEX idx_user_consent_status ON user_consent(status);
CREATE INDEX idx_user_consent_expires_at ON user_consent(expires_at);
CREATE INDEX idx_user_consent_user_content ON user_consent(user_id, content_id);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_user_consent_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_update_user_consent_updated_at
    BEFORE UPDATE ON user_consent
    FOR EACH ROW
    EXECUTE FUNCTION update_user_consent_updated_at();
