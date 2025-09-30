CREATE TABLE ai_memory (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  label VARCHAR(255),
  content TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE user_preferences (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  pref_key VARCHAR(255) NOT NULL,
  pref_value TEXT NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX idx_pref_user_key ON user_preferences(user_id, pref_key);
