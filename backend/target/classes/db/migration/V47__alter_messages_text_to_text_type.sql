-- V47: Alter messages text column from VARCHAR(255) to TEXT
-- Description: Changes the messages.text column to support longer AI-generated responses
-- Issue: VARCHAR(255) was limiting message length to 255 characters, causing failures for longer AI responses

-- Step 1: Drop the old constraint if it exists (from V17 - may have max length limit)
ALTER TABLE messages DROP CONSTRAINT IF EXISTS chk_messages_text_length;

-- Step 2: Change column type from VARCHAR(255) to TEXT
-- PostgreSQL will automatically convert VARCHAR to TEXT without data loss
ALTER TABLE messages 
ALTER COLUMN text TYPE TEXT;

-- Step 3: Re-add constraint without max length (only check minimum length >= 1)
ALTER TABLE messages ADD CONSTRAINT chk_messages_text_length CHECK (LENGTH(text) >= 1);

-- Step 4: Add comment for documentation
COMMENT ON COLUMN messages.text IS 'Message content - supports unlimited length for AI-generated responses';

