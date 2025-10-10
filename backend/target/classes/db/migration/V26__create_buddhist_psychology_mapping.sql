-- V26: Create buddhist_psychology_factors table with 43 mental factors and their categories
-- This migration creates the reference table for the 43 Buddhist mental factors used in psychological analysis

-- Table for Buddhist psychology factors mapping
CREATE TABLE buddhist_psychology_factors (
    id SERIAL PRIMARY KEY,
    factor_name VARCHAR(100) NOT NULL UNIQUE,
    factor_name_chinese VARCHAR(100), -- Chinese name for reference
    dimension_index INTEGER NOT NULL UNIQUE CHECK (dimension_index >= 0 AND dimension_index <= 42), -- 0-42 for 43 dimensions
    category VARCHAR(50) NOT NULL CHECK (category IN ('UNIVERSAL', 'SPECIFIC', 'POSITIVE', 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'VARIABLE')),
    subcategory VARCHAR(50), -- More specific categorization
    polarity VARCHAR(20) NOT NULL CHECK (polarity IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    modern_psychology_mapping TEXT, -- Mapping to modern psychology concepts
    description TEXT, -- Detailed description of the mental factor
    therapeutic_implications TEXT, -- How this factor relates to mental health
    is_core_factor BOOLEAN DEFAULT FALSE, -- Whether this is one of the 5 core factors
    is_secondary_affliction BOOLEAN DEFAULT FALSE, -- Whether this is a secondary affliction
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert the 5 core mental factors (dimensions 0-4)
INSERT INTO buddhist_psychology_factors (factor_name, factor_name_chinese, dimension_index, category, subcategory, polarity, modern_psychology_mapping, description, is_core_factor) VALUES
('Attention', '作意', 0, 'UNIVERSAL', 'CORE_FACTORS', 'NEUTRAL', 'Attention, Focus, Mindfulness', 'The mental factor that directs consciousness toward an object', true),
('Contact', '触', 1, 'UNIVERSAL', 'CORE_FACTORS', 'NEUTRAL', 'Sensory Contact, Perception-Response', 'The coming together of sense organ, object, and consciousness', true),
('Feeling', '受', 2, 'UNIVERSAL', 'CORE_FACTORS', 'NEUTRAL', 'Affect, Emotional Tone, Hedonic Tone', 'The mental factor that experiences the pleasant, unpleasant, or neutral tone of an object', true),
('Perception', '想', 3, 'UNIVERSAL', 'CORE_FACTORS', 'NEUTRAL', 'Cognition, Recognition, Conceptualization', 'The mental factor that recognizes and labels objects', true),
('Volition', '思', 4, 'UNIVERSAL', 'CORE_FACTORS', 'NEUTRAL', 'Intention, Will, Mental Formation', 'The mental factor that coordinates and directs mental activity', true);

-- Insert the 38 additional mental factors (dimensions 5-42)
INSERT INTO buddhist_psychology_factors (factor_name, factor_name_chinese, dimension_index, category, subcategory, polarity, modern_psychology_mapping, description) VALUES
-- Positive Mental Factors
('Faith', '信', 5, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Trust, Confidence, Belief', 'Confidence in the truth of the teachings and the path'),
('Vigor', '精进', 6, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Effort, Energy, Perseverance', 'The energy and effort applied to wholesome activities'),
('Inner Shame', '惭', 7, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Moral Shame, Conscience', 'The sense of shame that prevents unwholesome actions'),
('Shame', '愧', 8, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Social Shame, Fear of Blame', 'The fear of blame that prevents unwholesome actions'),
('Absence of Craving', '无贪', 9, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Non-attachment, Contentment', 'Freedom from craving and attachment'),
('Absence of Hatred', '无瞋', 10, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Non-aversion, Loving-kindness', 'Freedom from hatred and aversion'),
('Absence of Ignorance', '无痴', 11, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Wisdom, Understanding', 'Freedom from ignorance and delusion'),
('Light Ease', '轻安', 12, 'POSITIVE', 'CONCENTRATION', 'POSITIVE', 'Tranquility, Serenity', 'Mental and physical lightness and ease'),
('Non-laxness', '不放逸', 13, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Diligence, Carefulness', 'The careful attention to wholesome activities'),
('Equanimity', '舍', 14, 'POSITIVE', 'CONCENTRATION', 'POSITIVE', 'Equanimity, Balance', 'Mental balance and impartiality'),

-- Negative Mental Factors (Primary Afflictions)
('Non-harming', '不害', 15, 'POSITIVE', 'VIRTUE', 'POSITIVE', 'Non-violence, Compassion', 'The intention not to harm others'),
('Greed', '贪', 16, 'NEGATIVE', 'PRIMARY_AFFLICTIONS', 'NEGATIVE', 'Attachment, Clinging, Avarice', 'The desire to possess and hold onto objects'),
('Anger', '瞋', 17, 'NEGATIVE', 'PRIMARY_AFFLICTIONS', 'NEGATIVE', 'Hatred, Aversion, Hostility', 'The wish to harm or destroy what is disliked'),
('Arrogance', '慢', 18, 'NEGATIVE', 'PRIMARY_AFFLICTIONS', 'NEGATIVE', 'Pride, Conceit, Superiority', 'The inflated sense of self-importance'),
('Foolishness', '痴', 19, 'NEGATIVE', 'PRIMARY_AFFLICTIONS', 'NEGATIVE', 'Ignorance, Delusion, Confusion', 'The lack of understanding of reality'),
('Doubt', '疑', 20, 'NEGATIVE', 'PRIMARY_AFFLICTIONS', 'NEGATIVE', 'Skeptical Doubt, Indecision', 'The wavering between alternatives'),
('Wrong Views', '邪见', 21, 'NEGATIVE', 'PRIMARY_AFFLICTIONS', 'NEGATIVE', 'Misconception, False Beliefs', 'Incorrect understanding of reality'),

-- Secondary Afflictions
('Wrath', '忿', 22, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Rage, Fury', 'Intense anger that seeks to harm', true),
('Resentment', '恨', 23, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Resentment, Grudge', 'The holding onto anger and ill-will', true),
('Rage', '恼', 24, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Fury, Rage', 'Extreme anger that causes mental disturbance', true),
('Concealment', '覆', 25, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Concealment, Hiding', 'The tendency to hide one''s faults', true),
('Deceit', '诳', 26, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Deception, Dishonesty', 'The intention to deceive others', true),
('Hypocrisy', '谄', 27, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Hypocrisy, Flattery', 'The tendency to flatter and deceive', true),
('Conceit', '憍', 28, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Conceit, Vanity', 'The inflated sense of self based on possessions', true),
('Malice', '害', 29, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Malice, Cruelty', 'The intention to harm others', true),
('Jealousy', '嫉', 30, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Jealousy, Envy', 'The inability to bear others'' good fortune', true),
('Stinginess', '悭', 31, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Stinginess, Miserliness', 'The unwillingness to share with others', true),
('Shamelessness', '无惭', 32, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Shamelessness, Lack of Conscience', 'The lack of moral shame', true),
('Immodesty', '无愧', 33, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Immodesty, Lack of Social Shame', 'The lack of fear of blame', true),
('Lack of Faith', '不信', 34, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Lack of Faith, Skepticism', 'The lack of confidence in the teachings', true),
('Laziness', '懈怠', 35, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Laziness, Sloth', 'The lack of effort in wholesome activities', true),
('Heedlessness', '放逸', 36, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Heedlessness, Carelessness', 'The lack of careful attention', true),
('Torpor', '昏沉', 37, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Torpor, Dullness', 'Mental heaviness and dullness', true),
('Restlessness', '掉举', 38, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Restlessness, Agitation', 'Mental agitation and distraction', true),
('Lost Mindfulness', '失念', 39, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Lost Mindfulness, Forgetfulness', 'The loss of present-moment awareness', true),
('Improper Knowledge', '不正知', 40, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Improper Knowledge, Misunderstanding', 'Incorrect understanding of phenomena', true),
('Scatteredness', '散乱', 41, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Scatteredness, Distraction', 'Mental scattering and distraction', true),
('Regret', '恶作', 42, 'NEGATIVE', 'SECONDARY_AFFLICTIONS', 'NEGATIVE', 'Regret, Remorse', 'The mental factor of regret and remorse', true);

-- Update the is_secondary_affliction flag for secondary afflictions
UPDATE buddhist_psychology_factors 
SET is_secondary_affliction = true 
WHERE category = 'SECONDARY_AFFLICTIONS';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_buddhist_factors_category ON buddhist_psychology_factors(category);
CREATE INDEX IF NOT EXISTS idx_buddhist_factors_polarity ON buddhist_psychology_factors(polarity);
CREATE INDEX IF NOT EXISTS idx_buddhist_factors_dimension ON buddhist_psychology_factors(dimension_index);
CREATE INDEX IF NOT EXISTS idx_buddhist_factors_core ON buddhist_psychology_factors(is_core_factor);
CREATE INDEX IF NOT EXISTS idx_buddhist_factors_secondary ON buddhist_psychology_factors(is_secondary_affliction);

-- Comments for documentation
COMMENT ON TABLE buddhist_psychology_factors IS 'Reference table for 43 Buddhist mental factors used in psychological analysis';
COMMENT ON COLUMN buddhist_psychology_factors.dimension_index IS 'Zero-based index (0-42) corresponding to vector dimensions';
COMMENT ON COLUMN buddhist_psychology_factors.category IS 'Main category: UNIVERSAL, SPECIFIC, POSITIVE, NEGATIVE, SECONDARY_AFFLICTIONS, VARIABLE';
COMMENT ON COLUMN buddhist_psychology_factors.polarity IS 'Whether the factor is generally positive, negative, or neutral';
COMMENT ON COLUMN buddhist_psychology_factors.modern_psychology_mapping IS 'Corresponding concepts in modern psychology';
COMMENT ON COLUMN buddhist_psychology_factors.is_core_factor IS 'Whether this is one of the 5 core universal mental factors';
COMMENT ON COLUMN buddhist_psychology_factors.is_secondary_affliction IS 'Whether this is a secondary affliction (derived from primary afflictions)';
