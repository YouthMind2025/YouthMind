CREATE TABLE user_interests (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  topic VARCHAR(255) NOT NULL,
  score DOUBLE PRECISION NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX idx_user_topic ON user_interests(user_id, topic);
