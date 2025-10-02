-- Add moderation fields to comments table
-- This migration adds comprehensive moderation tracking to comments

-- Add moderation fields to comments table
ALTER TABLE comments 
ADD COLUMN is_flagged BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN flag_reason TEXT,
ADD COLUMN flagged_at TIMESTAMP,
ADD COLUMN moderated_at TIMESTAMP,
ADD COLUMN moderation_confidence DOUBLE PRECISION,
ADD COLUMN moderation_reason TEXT,
ADD COLUMN moderation_status VARCHAR(50) DEFAULT 'APPROVED';

-- Create index for flagged comments for efficient querying
CREATE INDEX idx_comments_is_flagged ON comments(is_flagged);

-- Create index for moderation status for efficient querying
CREATE INDEX idx_comments_moderation_status ON comments(moderation_status);

-- Create index for flagged_at for time-based queries
CREATE INDEX idx_comments_flagged_at ON comments(flagged_at);

-- Add comments to the migration tracking
COMMENT ON COLUMN comments.is_flagged IS 'Whether the comment has been flagged by moderation';
COMMENT ON COLUMN comments.flag_reason IS 'Reason why the comment was flagged';
COMMENT ON COLUMN comments.flagged_at IS 'When the comment was flagged';
COMMENT ON COLUMN comments.moderated_at IS 'When the comment was last moderated';
COMMENT ON COLUMN comments.moderation_confidence IS 'Confidence score of moderation decision (0.0-1.0)';
COMMENT ON COLUMN comments.moderation_reason IS 'Reason for moderation decision';
COMMENT ON COLUMN comments.moderation_status IS 'Current moderation status: APPROVED, FLAGGED, PENDING_REVIEW';
