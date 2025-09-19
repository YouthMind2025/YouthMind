-- Create moderation_audit table for tracking moderation decisions and transparency
CREATE TABLE moderation_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id UUID NOT NULL,
    content_type VARCHAR(50) NOT NULL CHECK (content_type IN ('post', 'comment')),
    user_id UUID,
    audit_type VARCHAR(100) NOT NULL,
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    confidence DECIMAL(3,2) NOT NULL CHECK (confidence >= 0.0 AND confidence <= 1.0),
    reason VARCHAR(1000),
    timestamp TIMESTAMP NOT NULL,
    moderation_result_json TEXT,
    context_hash VARCHAR(100),
    escalation_level INTEGER CHECK (escalation_level >= 0 AND escalation_level <= 3),
    action_taken VARCHAR(50) CHECK (action_taken IN ('ALLOW', 'HIDE', 'SHADOW_BAN', 'RATE_LIMIT', 'ESCALATE')),
    processing_time_ms BIGINT,
    ai_model_used VARCHAR(100),
    cache_hit BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_moderation_audit_content_id_type ON moderation_audit(content_id, content_type);
CREATE INDEX idx_moderation_audit_user_id_timestamp ON moderation_audit(user_id, timestamp);
CREATE INDEX idx_moderation_audit_audit_type_timestamp ON moderation_audit(audit_type, timestamp);
CREATE INDEX idx_moderation_audit_decision_timestamp ON moderation_audit(decision, timestamp);
CREATE INDEX idx_moderation_audit_escalation_level ON moderation_audit(escalation_level);
CREATE INDEX idx_moderation_audit_action_taken ON moderation_audit(action_taken);
CREATE INDEX idx_moderation_audit_ai_model_used ON moderation_audit(ai_model_used);
CREATE INDEX idx_moderation_audit_cache_hit ON moderation_audit(cache_hit);
CREATE INDEX idx_moderation_audit_timestamp ON moderation_audit(timestamp);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_moderation_audit_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_moderation_audit_updated_at
    BEFORE UPDATE ON moderation_audit
    FOR EACH ROW
    EXECUTE FUNCTION update_moderation_audit_updated_at();

-- Add comments for documentation
COMMENT ON TABLE moderation_audit IS 'Audit trail for all moderation decisions to ensure transparency and accountability';
COMMENT ON COLUMN moderation_audit.content_id IS 'ID of the content being moderated (post or comment)';
COMMENT ON COLUMN moderation_audit.content_type IS 'Type of content: post or comment';
COMMENT ON COLUMN moderation_audit.user_id IS 'ID of the user who created the content';
COMMENT ON COLUMN moderation_audit.audit_type IS 'Type of moderation audit: CONTEXT_WINDOW, ESCALATION_LEVEL_1, etc.';
COMMENT ON COLUMN moderation_audit.decision IS 'Final moderation decision: APPROVED or REJECTED';
COMMENT ON COLUMN moderation_audit.confidence IS 'Confidence score of the moderation decision (0.0 to 1.0)';
COMMENT ON COLUMN moderation_audit.reason IS 'Human-readable reason for the moderation decision';
COMMENT ON COLUMN moderation_audit.timestamp IS 'When the moderation decision was made';
COMMENT ON COLUMN moderation_audit.moderation_result_json IS 'Serialized ModerationResult object for detailed analysis';
COMMENT ON COLUMN moderation_audit.context_hash IS 'Hash of the context window used for caching';
COMMENT ON COLUMN moderation_audit.escalation_level IS 'Escalation level (0-3) for complex moderation cases';
COMMENT ON COLUMN moderation_audit.action_taken IS 'Action taken based on moderation decision';
COMMENT ON COLUMN moderation_audit.processing_time_ms IS 'Time taken to process the moderation request';
COMMENT ON COLUMN moderation_audit.ai_model_used IS 'AI model used for moderation (if applicable)';
COMMENT ON COLUMN moderation_audit.cache_hit IS 'Whether the result was retrieved from cache';
