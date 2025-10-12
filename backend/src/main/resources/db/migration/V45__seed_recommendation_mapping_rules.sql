-- V45: Seed recommendation mapping rules
-- Description: Inserts all 20 emotion combination to percentage distribution rules

-- Clear existing rules (in case of re-run)
DELETE FROM recommendation_mapping_rules;

-- ==================== SINGLE EMOTION RULES (6 rules) ====================

-- Rule 1: User primarily ANGRY
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'angry', 5, 5, 50, 25, 10, 5, NOW(), NOW());

-- Rule 2: User primarily CONFLICTED
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'conflicted', 10, 10, 15, 25, 30, 10, NOW(), NOW());

-- Rule 3: User primarily FEAR
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'fear', 15, 40, 5, 20, 10, 10, NOW(), NOW());

-- Rule 4: User primarily HAPPY
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'happy', 5, 5, 5, 50, 20, 15, NOW(), NOW());

-- Rule 5: User primarily SAD
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'sad', 45, 10, 5, 20, 15, 5, NOW(), NOW());

-- Rule 6: User primarily SURPRISED
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'surprised', 5, 10, 5, 35, 15, 30, NOW(), NOW());

-- ==================== TWO EMOTION COMBINATIONS (10 rules) ====================

-- Rule 7: ANGRY + FEAR
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'angry,fear', 10, 30, 30, 15, 10, 5, NOW(), NOW());

-- Rule 8: ANGRY + SAD
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'angry,sad', 30, 5, 30, 20, 10, 5, NOW(), NOW());

-- Rule 9: CONFLICTED + SURPRISED
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'conflicted,surprised', 10, 10, 10, 25, 25, 20, NOW(), NOW());

-- Rule 10: FEAR + SAD
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'fear,sad', 30, 30, 5, 20, 10, 5, NOW(), NOW());

-- Rule 11: HAPPY + SURPRISED
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'happy,surprised', 5, 5, 5, 40, 20, 25, NOW(), NOW());

-- Rule 12: ANGRY + HAPPY
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'angry,happy', 5, 5, 35, 35, 15, 5, NOW(), NOW());

-- Rule 13: CONFLICTED + FEAR
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'conflicted,fear', 10, 25, 10, 20, 25, 10, NOW(), NOW());

-- Rule 14: CONFLICTED + SAD
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'conflicted,sad', 25, 10, 10, 20, 25, 10, NOW(), NOW());

-- Rule 15: HAPPY + SAD
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'happy,sad', 25, 10, 5, 35, 15, 10, NOW(), NOW());

-- Rule 16: FEAR + SURPRISED
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'fear,surprised', 10, 30, 5, 25, 10, 20, NOW(), NOW());

-- ==================== THREE EMOTION COMBINATIONS (4 rules) ====================

-- Rule 17: ANGRY + FEAR + SAD (negative triad)
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'angry,fear,sad', 25, 25, 25, 15, 5, 5, NOW(), NOW());

-- Rule 18: CONFLICTED + HAPPY + SURPRISED (mixed emotions)
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'conflicted,happy,surprised', 10, 5, 5, 30, 25, 25, NOW(), NOW());

-- Rule 19: ANGRY + CONFLICTED + SAD
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'angry,conflicted,sad', 25, 10, 25, 15, 20, 5, NOW(), NOW());

-- Rule 20: FEAR + HAPPY + SAD (emotional complexity)
INSERT INTO recommendation_mapping_rules (id, emotion_combination, sad_percentage, fear_percentage, angry_percentage, happy_percentage, conflicted_percentage, surprised_percentage, created_at, updated_at)
VALUES (gen_random_uuid(), 'fear,happy,sad', 25, 25, 5, 25, 15, 5, NOW(), NOW());

-- ==================== VALIDATION ====================

-- Verify exactly 20 rules were inserted
DO $$
DECLARE
    rule_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO rule_count FROM recommendation_mapping_rules;
    IF rule_count <> 20 THEN
        RAISE EXCEPTION 'Expected 20 recommendation mapping rules, but found %', rule_count;
    END IF;
    RAISE NOTICE 'Successfully seeded % recommendation mapping rules', rule_count;
END $$;

-- Verify all percentages sum to 100
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count 
    FROM recommendation_mapping_rules
    WHERE (sad_percentage + fear_percentage + angry_percentage + 
           happy_percentage + conflicted_percentage + surprised_percentage) <> 100;
    
    IF invalid_count > 0 THEN
        RAISE EXCEPTION '% rules have invalid percentage totals (must sum to 100)', invalid_count;
    END IF;
    RAISE NOTICE 'All rules validated - percentages sum to 100';
END $$;

-- Display summary
SELECT 
    emotion_combination,
    sad_percentage as sad,
    fear_percentage as fear,
    angry_percentage as angry,
    happy_percentage as happy,
    conflicted_percentage as conf,
    surprised_percentage as surp,
    (sad_percentage + fear_percentage + angry_percentage + happy_percentage + conflicted_percentage + surprised_percentage) as total
FROM recommendation_mapping_rules
ORDER BY emotion_combination;

