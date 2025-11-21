-- V23: Create memory system tables for AI personalization

-- Create memory_items table
CREATE TABLE memory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    memory_type VARCHAR(50) NOT NULL CHECK (memory_type IN ('CHAT_CONTEXT', 'USER_PREFERENCE', 'ENGAGEMENT_PATTERN', 'PERSONALITY_TRAIT', 'CONVERSATION_TOPIC', 'EMOTIONAL_STATE', 'BEHAVIORAL_PATTERN', 'INTERACTION_HISTORY')),
    key_identifier VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    compressed_content TEXT,
    context_tags JSONB,
    weight_score DECIMAL(10,6) NOT NULL DEFAULT 0.000000,
    base_weight DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
    time_decay_factor DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
    engagement_multiplier DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
    importance_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (importance_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    access_count INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP,
    expires_at TIMESTAMP,
    related_entity_id UUID,
    related_entity_type VARCHAR(50),
    source_context TEXT,
    embedding_vector TEXT, -- For future semantic search
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, key_identifier)
);

-- Create indexes for memory performance
CREATE INDEX IF NOT EXISTS idx_memory_items_user_id ON memory_items(user_id);
CREATE INDEX IF NOT EXISTS idx_memory_items_type ON memory_items(memory_type);
CREATE INDEX IF NOT EXISTS idx_memory_items_weight ON memory_items(weight_score DESC);
CREATE INDEX IF NOT EXISTS idx_memory_items_archived ON memory_items(archived_at);
CREATE INDEX IF NOT EXISTS idx_memory_items_expires ON memory_items(expires_at);
CREATE INDEX IF NOT EXISTS idx_memory_items_last_accessed ON memory_items(last_accessed_at DESC);
CREATE INDEX IF NOT EXISTS idx_memory_items_importance ON memory_items(importance_level);
CREATE INDEX IF NOT EXISTS idx_memory_items_user_type_weight ON memory_items(user_id, memory_type, weight_score DESC);

-- Create GIN index for context search
CREATE INDEX IF NOT EXISTS idx_memory_items_context_tags ON memory_items USING gin(context_tags);

-- Create full-text search index for content
CREATE INDEX IF NOT EXISTS idx_memory_items_content_search ON memory_items USING gin(to_tsvector('english', content));

-- Add memory management functions
CREATE OR REPLACE FUNCTION calculate_memory_weight(
    p_base_weight DECIMAL,
    p_created_at TIMESTAMP,
    p_last_accessed_at TIMESTAMP,
    p_access_count INTEGER,
    p_importance_level VARCHAR,
    p_engagement_multiplier DECIMAL
) RETURNS DECIMAL AS $$
DECLARE
    days_since_creation DECIMAL;
    days_since_access DECIMAL;
    time_decay_factor DECIMAL;
    access_boost DECIMAL;
    importance_multiplier DECIMAL;
    final_weight DECIMAL;
BEGIN
    -- Calculate time differences
    days_since_creation := EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - p_created_at)) / 86400.0;
    days_since_access := CASE 
        WHEN p_last_accessed_at IS NOT NULL THEN 
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - p_last_accessed_at)) / 86400.0
        ELSE days_since_creation
    END;
    
    -- Apply exponential decay (τ = 30 days)
    time_decay_factor := EXP(-LEAST(days_since_creation, days_since_access) / 30.0);
    
    -- Access frequency boost
    access_boost := 1.0 + LN(1 + p_access_count) * 0.1;
    
    -- Importance multiplier
    importance_multiplier := CASE p_importance_level
        WHEN 'CRITICAL' THEN 2.0
        WHEN 'HIGH' THEN 1.5
        WHEN 'MEDIUM' THEN 1.0
        WHEN 'LOW' THEN 0.7
        ELSE 1.0
    END;
    
    -- Calculate final weight
    final_weight := p_base_weight * time_decay_factor * access_boost * 
                   importance_multiplier * p_engagement_multiplier;
    
    RETURN ROUND(final_weight, 6);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Create function to update all memory weights
CREATE OR REPLACE FUNCTION update_user_memory_weights(p_user_id UUID)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER := 0;
BEGIN
    UPDATE memory_items 
    SET weight_score = calculate_memory_weight(
        base_weight, 
        created_at, 
        last_accessed_at, 
        access_count, 
        importance_level, 
        engagement_multiplier
    ),
    updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_user_id AND archived_at IS NULL;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Create function for memory cleanup
CREATE OR REPLACE FUNCTION cleanup_user_memories(p_user_id UUID, p_max_memories INTEGER DEFAULT 1000)
RETURNS INTEGER AS $$
DECLARE
    current_count INTEGER;
    archived_count INTEGER := 0;
    memory_record RECORD;
BEGIN
    -- Count current active memories
    SELECT COUNT(*) INTO current_count 
    FROM memory_items 
    WHERE user_id = p_user_id AND archived_at IS NULL;
    
    -- If under limit, just archive expired memories
    IF current_count <= p_max_memories THEN
        UPDATE memory_items 
        SET archived_at = CURRENT_TIMESTAMP 
        WHERE user_id = p_user_id AND expires_at < CURRENT_TIMESTAMP AND archived_at IS NULL;
        
        GET DIAGNOSTICS archived_count = ROW_COUNT;
        RETURN archived_count;
    END IF;
    
    -- Archive expired memories first
    UPDATE memory_items 
    SET archived_at = CURRENT_TIMESTAMP 
    WHERE user_id = p_user_id AND expires_at < CURRENT_TIMESTAMP AND archived_at IS NULL;
    
    -- Archive lowest weight memories if still over limit
    UPDATE memory_items 
    SET archived_at = CURRENT_TIMESTAMP 
    WHERE id IN (
        SELECT id FROM memory_items 
        WHERE user_id = p_user_id AND archived_at IS NULL 
        ORDER BY weight_score ASC, last_accessed_at ASC 
        LIMIT (current_count - p_max_memories)
    );
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

-- Create scheduled cleanup job function
CREATE OR REPLACE FUNCTION scheduled_memory_maintenance()
RETURNS INTEGER AS $$
DECLARE
    total_cleaned INTEGER := 0;
    user_record RECORD;
BEGIN
    -- Update weights for all active memories
    FOR user_record IN 
        SELECT DISTINCT user_id FROM memory_items WHERE archived_at IS NULL
    LOOP
        PERFORM update_user_memory_weights(user_record.user_id);
        total_cleaned := total_cleaned + cleanup_user_memories(user_record.user_id);
    END LOOP;
    
    -- Clean up very old archived memories (older than 1 year)
    DELETE FROM memory_items 
    WHERE archived_at < CURRENT_TIMESTAMP - INTERVAL '365 days';
    
    RETURN total_cleaned;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE memory_items IS 'AI memory system for personalized user interactions with time decay algorithms';
COMMENT ON COLUMN memory_items.weight_score IS 'Current calculated weight using time decay: baseWeight * exp(-Δt/τ) * accessBoost * importanceMultiplier';
COMMENT ON COLUMN memory_items.time_decay_factor IS 'Time decay factor applied to base weight';
COMMENT ON COLUMN memory_items.engagement_multiplier IS 'Multiplier based on user engagement with this memory';
COMMENT ON FUNCTION calculate_memory_weight IS 'Calculate memory weight with exponential time decay (τ=30 days)';
COMMENT ON FUNCTION update_user_memory_weights IS 'Update all memory weights for a user using time decay algorithm';
COMMENT ON FUNCTION cleanup_user_memories IS 'Archive old and low-weight memories to maintain performance';
COMMENT ON FUNCTION scheduled_memory_maintenance IS 'Run this daily to maintain memory system performance';
