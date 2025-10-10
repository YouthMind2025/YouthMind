CREATE TABLE user_behavior_analytics (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  post_id UUID,
  view_duration_seconds BIGINT,
  interaction_type VARCHAR(50),
  metadata_json TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_behavior_user_created ON user_behavior_analytics(user_id, created_at);
