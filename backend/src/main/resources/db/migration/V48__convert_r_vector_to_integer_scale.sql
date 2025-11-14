-- V48: Convert user_recommendation_vectors from DECIMAL [0.0000, 1.0000] to INTEGER [2, 40]
-- Description: Migrates emotion weights from normalized decimal percentages to integer scale matching specification
-- Created: 2024-12-XX
-- 
-- Conversion Formula: INTEGER = ROUND((DECIMAL * 38) + 2)
-- This maps: 0.0000 → 2, 1.0000 → 40, 0.1000 → 6
-- 
-- Migration Strategy:
-- 1. Add temporary INTEGER columns
-- 2. Convert and populate INTEGER columns from DECIMAL columns
-- 3. Drop old DECIMAL columns and constraints
-- 4. Rename INTEGER columns to original names
-- 5. Add new constraints for INTEGER range [2, 40]
-- 6. Update default values to 10

BEGIN;

-- ============================================================================
-- STEP 1: Add temporary INTEGER columns for all emotion weights
-- ============================================================================
-- These columns will hold the converted integer values during migration

ALTER TABLE user_recommendation_vectors
ADD COLUMN sad_weight_int INTEGER,
ADD COLUMN fear_weight_int INTEGER,
ADD COLUMN angry_weight_int INTEGER,
ADD COLUMN happy_weight_int INTEGER,
ADD COLUMN conflicted_weight_int INTEGER,
ADD COLUMN surprised_weight_int INTEGER;

COMMENT ON COLUMN user_recommendation_vectors.sad_weight_int IS 'Temporary column for integer conversion - will be renamed to sad_weight';
COMMENT ON COLUMN user_recommendation_vectors.fear_weight_int IS 'Temporary column for integer conversion - will be renamed to fear_weight';
COMMENT ON COLUMN user_recommendation_vectors.angry_weight_int IS 'Temporary column for integer conversion - will be renamed to angry_weight';
COMMENT ON COLUMN user_recommendation_vectors.happy_weight_int IS 'Temporary column for integer conversion - will be renamed to happy_weight';
COMMENT ON COLUMN user_recommendation_vectors.conflicted_weight_int IS 'Temporary column for integer conversion - will be renamed to conflicted_weight';
COMMENT ON COLUMN user_recommendation_vectors.surprised_weight_int IS 'Temporary column for integer conversion - will be renamed to surprised_weight';

-- ============================================================================
-- STEP 2: Convert existing DECIMAL data to INTEGER [2, 40] scale
-- ============================================================================
-- Conversion formula: INTEGER = ROUND((DECIMAL * 38) + 2)
-- Handles NULL values by setting to 10 (spec default)
-- Clamps out-of-range values to [2, 40]

UPDATE user_recommendation_vectors
SET 
    -- Convert DECIMAL [0.0000, 1.0000] to INTEGER [2, 40]
    -- Formula: ROUND((decimal_value * 38) + 2)
    -- Clamp to [2, 40] range for safety
    sad_weight_int = GREATEST(2, LEAST(40, 
        COALESCE(ROUND((sad_weight * 38) + 2), 10)
    )),
    fear_weight_int = GREATEST(2, LEAST(40, 
        COALESCE(ROUND((fear_weight * 38) + 2), 10)
    )),
    angry_weight_int = GREATEST(2, LEAST(40, 
        COALESCE(ROUND((angry_weight * 38) + 2), 10)
    )),
    happy_weight_int = GREATEST(2, LEAST(40, 
        COALESCE(ROUND((happy_weight * 38) + 2), 10)
    )),
    conflicted_weight_int = GREATEST(2, LEAST(40, 
        COALESCE(ROUND((conflicted_weight * 38) + 2), 10)
    )),
    surprised_weight_int = GREATEST(2, LEAST(40, 
        COALESCE(ROUND((surprised_weight * 38) + 2), 10)
    ))
WHERE 
    -- Update all existing rows
    true;

-- ============================================================================
-- STEP 3: Drop old DECIMAL columns and their constraints
-- ============================================================================
-- First, drop the CHECK constraints on DECIMAL columns

ALTER TABLE user_recommendation_vectors
DROP CONSTRAINT IF EXISTS chk_user_recommendation_vectors_sad_weight_range,
DROP CONSTRAINT IF EXISTS chk_user_recommendation_vectors_fear_weight_range,
DROP CONSTRAINT IF EXISTS chk_user_recommendation_vectors_angry_weight_range,
DROP CONSTRAINT IF EXISTS chk_user_recommendation_vectors_happy_weight_range,
DROP CONSTRAINT IF EXISTS chk_user_recommendation_vectors_conflicted_weight_range,
DROP CONSTRAINT IF EXISTS chk_user_recommendation_vectors_surprised_weight_range;

-- Drop the old DECIMAL columns
ALTER TABLE user_recommendation_vectors
DROP COLUMN sad_weight,
DROP COLUMN fear_weight,
DROP COLUMN angry_weight,
DROP COLUMN happy_weight,
DROP COLUMN conflicted_weight,
DROP COLUMN surprised_weight;

-- ============================================================================
-- STEP 4: Rename INTEGER columns to original names
-- ============================================================================

ALTER TABLE user_recommendation_vectors
RENAME COLUMN sad_weight_int TO sad_weight;

ALTER TABLE user_recommendation_vectors
RENAME COLUMN fear_weight_int TO fear_weight;

ALTER TABLE user_recommendation_vectors
RENAME COLUMN angry_weight_int TO angry_weight;

ALTER TABLE user_recommendation_vectors
RENAME COLUMN happy_weight_int TO happy_weight;

ALTER TABLE user_recommendation_vectors
RENAME COLUMN conflicted_weight_int TO conflicted_weight;

ALTER TABLE user_recommendation_vectors
RENAME COLUMN surprised_weight_int TO surprised_weight;

-- ============================================================================
-- STEP 5: Add NOT NULL constraints and set defaults
-- ============================================================================
-- Set default value to 10 (spec initialization) for all emotion weights

ALTER TABLE user_recommendation_vectors
ALTER COLUMN sad_weight SET DEFAULT 10,
ALTER COLUMN fear_weight SET DEFAULT 10,
ALTER COLUMN angry_weight SET DEFAULT 10,
ALTER COLUMN happy_weight SET DEFAULT 10,
ALTER COLUMN conflicted_weight SET DEFAULT 10,
ALTER COLUMN surprised_weight SET DEFAULT 10;

-- Add NOT NULL constraints
ALTER TABLE user_recommendation_vectors
ALTER COLUMN sad_weight SET NOT NULL,
ALTER COLUMN fear_weight SET NOT NULL,
ALTER COLUMN angry_weight SET NOT NULL,
ALTER COLUMN happy_weight SET NOT NULL,
ALTER COLUMN conflicted_weight SET NOT NULL,
ALTER COLUMN surprised_weight SET NOT NULL;

-- ============================================================================
-- STEP 6: Add new CHECK constraints for INTEGER range [2, 40]
-- ============================================================================
-- Enforce the specification range: each emotion weight must be between 2 and 40

ALTER TABLE user_recommendation_vectors
ADD CONSTRAINT chk_user_recommendation_vectors_sad_weight_range 
    CHECK (sad_weight >= 2 AND sad_weight <= 40);

ALTER TABLE user_recommendation_vectors
ADD CONSTRAINT chk_user_recommendation_vectors_fear_weight_range 
    CHECK (fear_weight >= 2 AND fear_weight <= 40);

ALTER TABLE user_recommendation_vectors
ADD CONSTRAINT chk_user_recommendation_vectors_angry_weight_range 
    CHECK (angry_weight >= 2 AND angry_weight <= 40);

ALTER TABLE user_recommendation_vectors
ADD CONSTRAINT chk_user_recommendation_vectors_happy_weight_range 
    CHECK (happy_weight >= 2 AND happy_weight <= 40);

ALTER TABLE user_recommendation_vectors
ADD CONSTRAINT chk_user_recommendation_vectors_conflicted_weight_range 
    CHECK (conflicted_weight >= 2 AND conflicted_weight <= 40);

ALTER TABLE user_recommendation_vectors
ADD CONSTRAINT chk_user_recommendation_vectors_surprised_weight_range 
    CHECK (surprised_weight >= 2 AND surprised_weight <= 40);

-- ============================================================================
-- STEP 7: Update column comments to reflect INTEGER type
-- ============================================================================

COMMENT ON COLUMN user_recommendation_vectors.sad_weight IS 'Weight for sad emotion content (2 to 40) - integer scale matching specification';
COMMENT ON COLUMN user_recommendation_vectors.fear_weight IS 'Weight for fear emotion content (2 to 40) - integer scale matching specification';
COMMENT ON COLUMN user_recommendation_vectors.angry_weight IS 'Weight for angry emotion content (2 to 40) - integer scale matching specification';
COMMENT ON COLUMN user_recommendation_vectors.happy_weight IS 'Weight for happy emotion content (2 to 40) - integer scale matching specification';
COMMENT ON COLUMN user_recommendation_vectors.conflicted_weight IS 'Weight for conflicted emotion content (2 to 40) - integer scale matching specification';
COMMENT ON COLUMN user_recommendation_vectors.surprised_weight IS 'Weight for surprised emotion content (2 to 40) - integer scale matching specification';

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these queries after migration to verify conversion

-- Check that all columns are INTEGER type
DO $$
DECLARE
    sad_type TEXT;
    fear_type TEXT;
    angry_type TEXT;
    happy_type TEXT;
    conflicted_type TEXT;
    surprised_type TEXT;
BEGIN
    SELECT data_type INTO sad_type 
    FROM information_schema.columns 
    WHERE table_name = 'user_recommendation_vectors' AND column_name = 'sad_weight';
    
    SELECT data_type INTO fear_type 
    FROM information_schema.columns 
    WHERE table_name = 'user_recommendation_vectors' AND column_name = 'fear_weight';
    
    SELECT data_type INTO angry_type 
    FROM information_schema.columns 
    WHERE table_name = 'user_recommendation_vectors' AND column_name = 'angry_weight';
    
    SELECT data_type INTO happy_type 
    FROM information_schema.columns 
    WHERE table_name = 'user_recommendation_vectors' AND column_name = 'happy_weight';
    
    SELECT data_type INTO conflicted_type 
    FROM information_schema.columns 
    WHERE table_name = 'user_recommendation_vectors' AND column_name = 'conflicted_weight';
    
    SELECT data_type INTO surprised_type 
    FROM information_schema.columns 
    WHERE table_name = 'user_recommendation_vectors' AND column_name = 'surprised_weight';
    
    IF sad_type = 'integer' AND fear_type = 'integer' AND angry_type = 'integer' 
       AND happy_type = 'integer' AND conflicted_type = 'integer' AND surprised_type = 'integer' THEN
        RAISE NOTICE '✅ All emotion weight columns are INTEGER type';
    ELSE
        RAISE EXCEPTION '❌ Column type verification failed. Expected INTEGER, got: sad=%, fear=%, angry=%, happy=%, conflicted=%, surprised=%', 
            sad_type, fear_type, angry_type, happy_type, conflicted_type, surprised_type;
    END IF;
END $$;

-- Check that all values are within [2, 40] range
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM user_recommendation_vectors
    WHERE sad_weight < 2 OR sad_weight > 40
       OR fear_weight < 2 OR fear_weight > 40
       OR angry_weight < 2 OR angry_weight > 40
       OR happy_weight < 2 OR happy_weight > 40
       OR conflicted_weight < 2 OR conflicted_weight > 40
       OR surprised_weight < 2 OR surprised_weight > 40;
    
    IF invalid_count = 0 THEN
        RAISE NOTICE '✅ All emotion weight values are within [2, 40] range';
    ELSE
        RAISE EXCEPTION '❌ Found % rows with values outside [2, 40] range', invalid_count;
    END IF;
END $$;

-- Check that no NULL values exist
DO $$
DECLARE
    null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count
    FROM user_recommendation_vectors
    WHERE sad_weight IS NULL
       OR fear_weight IS NULL
       OR angry_weight IS NULL
       OR happy_weight IS NULL
       OR conflicted_weight IS NULL
       OR surprised_weight IS NULL;
    
    IF null_count = 0 THEN
        RAISE NOTICE '✅ No NULL values found in emotion weight columns';
    ELSE
        RAISE EXCEPTION '❌ Found % rows with NULL values in emotion weight columns', null_count;
    END IF;
END $$;

-- Display sample conversion results for verification
DO $$
DECLARE
    total_rows INTEGER;
    min_sad INTEGER;
    max_sad INTEGER;
    avg_sad NUMERIC;
BEGIN
    SELECT COUNT(*), MIN(sad_weight), MAX(sad_weight), AVG(sad_weight)::NUMERIC(10,2)
    INTO total_rows, min_sad, max_sad, avg_sad
    FROM user_recommendation_vectors;
    
    RAISE NOTICE '📊 Conversion Summary:';
    RAISE NOTICE '   Total rows: %', total_rows;
    RAISE NOTICE '   Sad weight range: [%, %], Average: %', min_sad, max_sad, avg_sad;
    RAISE NOTICE '   (Expected range: [2, 40], Expected default: 10)';
END $$;

-- ============================================================================
-- ROLLBACK CONSIDERATIONS
-- ============================================================================
-- 
-- If rollback is needed, this migration cannot be easily reversed because:
-- 1. Original DECIMAL values are lost after conversion
-- 2. Conversion formula is not perfectly reversible (rounding loss)
-- 
-- Rollback strategy (if absolutely necessary):
-- 1. Restore from database backup taken before migration
-- 2. OR manually convert back using approximate formula:
--    DECIMAL ≈ (INTEGER - 2) / 38.0
--    (Note: This is approximate due to rounding in original conversion)
-- 
-- RECOMMENDATION: Test migration on staging environment first
-- ============================================================================

COMMIT;

-- ============================================================================
-- POST-MIGRATION VERIFICATION QUERIES (Run manually after migration)
-- ============================================================================
-- 
-- 1. Check column types:
--    SELECT column_name, data_type, column_default, is_nullable
--    FROM information_schema.columns
--    WHERE table_name = 'user_recommendation_vectors'
--    AND column_name LIKE '%_weight';
-- 
-- 2. Check constraints:
--    SELECT constraint_name, constraint_type
--    FROM information_schema.table_constraints
--    WHERE table_name = 'user_recommendation_vectors'
--    AND constraint_name LIKE '%weight%';
-- 
-- 3. Sample data verification:
--    SELECT 
--        user_id,
--        version,
--        sad_weight,
--        fear_weight,
--        angry_weight,
--        happy_weight,
--        conflicted_weight,
--        surprised_weight,
--        (sad_weight + fear_weight + angry_weight + happy_weight + conflicted_weight + surprised_weight) as total
--    FROM user_recommendation_vectors
--    ORDER BY created_at DESC
--    LIMIT 10;
-- 
-- 4. Check for any values outside expected range:
--    SELECT COUNT(*) as out_of_range_count
--    FROM user_recommendation_vectors
--    WHERE sad_weight < 2 OR sad_weight > 40
--       OR fear_weight < 2 OR fear_weight > 40
--       OR angry_weight < 2 OR angry_weight > 40
--       OR happy_weight < 2 OR happy_weight > 40
--       OR conflicted_weight < 2 OR conflicted_weight > 40
--       OR surprised_weight < 2 OR surprised_weight > 40;
-- 
-- 5. Verify default values for new inserts:
--    INSERT INTO user_recommendation_vectors (user_id, version)
--    VALUES (gen_random_uuid(), 1)
--    RETURNING *;
--    -- Then DELETE the test row
-- 
-- ============================================================================


