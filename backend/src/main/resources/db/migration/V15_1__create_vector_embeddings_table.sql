-- Create vector_embeddings table for storing vector embedding metadata
CREATE TABLE vector_embeddings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content_id UUID NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    qdrant_point_id VARCHAR(255) NOT NULL UNIQUE,
    collection_name VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    vector_dimension INTEGER NOT NULL,
    embedding_text TEXT,
    metadata JSONB,
    quality_score DOUBLE PRECISION,
    processing_time_ms BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_vector_content_type ON vector_embeddings(content_type);
CREATE INDEX IF NOT EXISTS idx_vector_content_id ON vector_embeddings(content_id);
CREATE INDEX IF NOT EXISTS idx_vector_model ON vector_embeddings(model);
CREATE INDEX IF NOT EXISTS idx_vector_created_at ON vector_embeddings(created_at);
CREATE INDEX IF NOT EXISTS idx_vector_collection_name ON vector_embeddings(collection_name);
CREATE INDEX IF NOT EXISTS idx_vector_qdrant_point_id ON vector_embeddings(qdrant_point_id);
CREATE INDEX IF NOT EXISTS idx_vector_is_active ON vector_embeddings(is_active);
CREATE INDEX IF NOT EXISTS idx_vector_quality_score ON vector_embeddings(quality_score);
CREATE INDEX IF NOT EXISTS idx_vector_expires_at ON vector_embeddings(expires_at);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_vector_content_type_active ON vector_embeddings(content_type, is_active);
CREATE INDEX IF NOT EXISTS idx_vector_collection_active ON vector_embeddings(collection_name, is_active);
CREATE INDEX IF NOT EXISTS idx_vector_model_active ON vector_embeddings(model, is_active);
CREATE INDEX IF NOT EXISTS idx_vector_content_id_type ON vector_embeddings(content_id, content_type);

-- Partial indexes for active embeddings
CREATE INDEX IF NOT EXISTS idx_vector_active_quality ON vector_embeddings(quality_score) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_vector_active_created ON vector_embeddings(created_at) WHERE is_active = true;

-- GIN index for JSONB metadata
CREATE INDEX IF NOT EXISTS idx_vector_metadata_gin ON vector_embeddings USING GIN (metadata);

-- Add comments for documentation
COMMENT ON TABLE vector_embeddings IS 'Stores metadata for vector embeddings stored in Qdrant vector database';
COMMENT ON COLUMN vector_embeddings.content_id IS 'Reference to the original content (post, comment, etc.)';
COMMENT ON COLUMN vector_embeddings.content_type IS 'Type of content: POST, COMMENT, MODERATION_RESULT, etc.';
COMMENT ON COLUMN vector_embeddings.qdrant_point_id IS 'Unique identifier for the vector point in Qdrant';
COMMENT ON COLUMN vector_embeddings.collection_name IS 'Qdrant collection name where the vector is stored';
COMMENT ON COLUMN vector_embeddings.model IS 'AI model used to generate the embedding';
COMMENT ON COLUMN vector_embeddings.vector_dimension IS 'Dimension of the vector embedding';
COMMENT ON COLUMN vector_embeddings.embedding_text IS 'Original text that was embedded';
COMMENT ON COLUMN vector_embeddings.metadata IS 'Additional metadata as JSON';
COMMENT ON COLUMN vector_embeddings.quality_score IS 'Quality score of the embedding (0.0 to 1.0)';
COMMENT ON COLUMN vector_embeddings.processing_time_ms IS 'Time taken to generate the embedding in milliseconds';
COMMENT ON COLUMN vector_embeddings.is_active IS 'Whether the embedding is currently active';
COMMENT ON COLUMN vector_embeddings.expires_at IS 'When the embedding expires (for temporary embeddings)';
