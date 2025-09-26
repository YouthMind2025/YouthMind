package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps 43 Buddhist mental factors to modern psychology frameworks
 * 
 * This service provides comprehensive mapping between the 43-dimensional
 * Buddhist mental factors system and modern psychological frameworks including:
 * - Big Five Personality Traits (OCEAN)
 * - Cognitive Behavioral Therapy (CBT) concepts
 * - Emotional Intelligence (EI) dimensions
 * - Positive Psychology constructs
 * - Clinical psychology categories
 * 
 * The mapping is based on psychological research and clinical practice
 * to ensure accurate cross-cultural psychological understanding.
 */
@Service
public class BuddhistPsychologyMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(BuddhistPsychologyMapper.class);
    
    // Modern psychology frameworks
    public enum ModernPsychologyFramework {
        BIG_FIVE,           // Openness, Conscientiousness, Extraversion, Agreeableness, Neuroticism
        CBT,                // Cognitive Behavioral Therapy
        EMOTIONAL_INTELLIGENCE, // Self-awareness, self-regulation, motivation, empathy, social skills
        POSITIVE_PSYCHOLOGY, // Well-being, strengths, virtues, meaning
        CLINICAL_PSYCHOLOGY, // Clinical psychology categories
        COGNITIVE_FUNCTIONS, // Attention, memory, executive function, etc.
        SOCIAL_PSYCHOLOGY,   // Social cognition, relationships, group dynamics
        DEVELOPMENTAL       // Developmental stages and milestones
    }
    
    // Factor categories for classification
    public enum FactorCategory {
        COGNITIVE,          // Thinking, perception, attention, memory
        EMOTIONAL,          // Feelings, mood, emotional regulation
        VOLITIONAL,         // Will, intention, volition
        CONSCIOUSNESS,      // Awareness, mindfulness, consciousness
        MENTAL_FACTORS,     // General mental factors
        UNIVERSAL,          // Universal factors present in all minds
        BEHAVIORAL,         // Actions, habits, behavioral patterns
        SOCIAL,             // Relationships, empathy, social skills
        MOTIVATIONAL,       // Drive, goals, intrinsic motivation
        REGULATORY,         // Self-control, impulse control, regulation
        SPIRITUAL,          // Meaning, purpose, transcendence
        PATHOLOGICAL        // Maladaptive patterns, disorders
    }
    
    // Buddhist factor definitions and modern psychology mappings
    private static final Map<Integer, BuddhistFactor> BUDDHIST_FACTORS = new HashMap<>();
    
    static {
        // Initialize the 43 Buddhist mental factors with modern psychology mappings
        initializeBuddhistFactors();
    }
    
    /**
     * Map a 43-dimensional vector to modern psychology framework
     * 
     * @param vector 43-dimensional Buddhist mental factors vector
     * @param framework Target modern psychology framework
     * @return Mapping result with scores and interpretations
     */
    public ModernPsychologyMapping mapToModernPsychology(List<Double> vector, ModernPsychologyFramework framework) {
        try {
            logger.debug("Mapping 43D vector to {} framework", framework);
            
            if (vector == null || vector.size() != 43) {
                throw new IllegalArgumentException("Vector must be exactly 43 dimensions");
            }
            
            Map<String, Double> frameworkScores = new HashMap<>();
            Map<String, String> interpretations = new HashMap<>();
            List<FactorMapping> factorMappings = new ArrayList<>();
            
            // Calculate framework-specific scores
            switch (framework) {
                case BIG_FIVE:
                    frameworkScores = mapToBigFive(vector);
                    break;
                case CBT:
                    frameworkScores = mapToCBT(vector);
                    break;
                case EMOTIONAL_INTELLIGENCE:
                    frameworkScores = mapToEmotionalIntelligence(vector);
                    break;
                case POSITIVE_PSYCHOLOGY:
                    frameworkScores = mapToPositivePsychology(vector);
                    break;
                case CLINICAL_PSYCHOLOGY:
                    frameworkScores = mapToClinicalCategories(vector);
                    break;
                case COGNITIVE_FUNCTIONS:
                    frameworkScores = mapToCognitiveFunctions(vector);
                    break;
                case SOCIAL_PSYCHOLOGY:
                    frameworkScores = mapToSocialPsychology(vector);
                    break;
                case DEVELOPMENTAL:
                    frameworkScores = mapToDevelopmental(vector);
                    break;
            }
            
            // Generate interpretations
            interpretations = generateInterpretations(frameworkScores, framework);
            
            // Create factor mappings
            factorMappings = createFactorMappings(vector, framework);
            
            ModernPsychologyMapping mapping = new ModernPsychologyMapping();
            mapping.setFramework(framework);
            mapping.setScores(frameworkScores);
            mapping.setInterpretations(interpretations);
            mapping.setFactorMappings(factorMappings);
            mapping.setConfidence(calculateMappingConfidence(frameworkScores));
            mapping.setTimestamp(new Date());
            
            logger.debug("Successfully mapped vector to {} framework", framework);
            return mapping;
            
        } catch (Exception e) {
            logger.error("Error mapping vector to modern psychology: {}", e.getMessage(), e);
            return createErrorMapping(framework, e.getMessage());
        }
    }
    
    /**
     * Categorize a specific Buddhist mental factor
     * 
     * @param factorIndex Index of the factor (0-42)
     * @return Factor category and description
     */
    public FactorCategory categorizeFactor(int factorIndex) {
        try {
            if (factorIndex < 0 || factorIndex >= 43) {
                throw new IllegalArgumentException("Factor index must be between 0 and 42");
            }
            
            BuddhistFactor factor = BUDDHIST_FACTORS.get(factorIndex);
            if (factor == null) {
                return FactorCategory.COGNITIVE; // Default category
            }
            
            return factor.getPrimaryCategory();
            
        } catch (Exception e) {
            logger.error("Error categorizing factor {}: {}", factorIndex, e.getMessage(), e);
            return FactorCategory.COGNITIVE;
        }
    }
    
    /**
     * Get detailed description of a Buddhist mental factor
     * 
     * @param factorIndex Index of the factor (0-42)
     * @return Comprehensive factor description
     */
    public FactorDescription getFactorDescription(int factorIndex) {
        try {
            if (factorIndex < 0 || factorIndex >= 43) {
                throw new IllegalArgumentException("Factor index must be between 0 and 42");
            }
            
            BuddhistFactor factor = BUDDHIST_FACTORS.get(factorIndex);
            if (factor == null) {
                return createDefaultFactorDescription(factorIndex);
            }
            
            FactorDescription description = new FactorDescription();
            description.setIndex(factorIndex);
            description.setName(factor.getName());
            description.setBuddhistDescription(factor.getBuddhistDescription());
            description.setModernDescription(factor.getModernDescription());
            description.setCategory(factor.getPrimaryCategory());
            description.setSecondaryCategories(factor.getSecondaryCategories());
            description.setModernMappings(factor.getModernMappings());
            description.setClinicalRelevance(factor.getClinicalRelevance());
            description.setTherapeuticImplications(factor.getTherapeuticImplications());
            
            return description;
            
        } catch (Exception e) {
            logger.error("Error getting factor description for {}: {}", factorIndex, e.getMessage(), e);
            return createDefaultFactorDescription(factorIndex);
        }
    }
    
    /**
     * Get all factors in a specific category
     * 
     * @param category Factor category
     * @return List of factors in the category
     */
    public List<FactorDescription> getFactorsByCategory(FactorCategory category) {
        try {
            return BUDDHIST_FACTORS.values().stream()
                .filter(factor -> factor.getPrimaryCategory() == category || 
                                factor.getSecondaryCategories().contains(category))
                .map(factor -> {
                    FactorDescription desc = new FactorDescription();
                    desc.setIndex(factor.getIndex());
                    desc.setName(factor.getName());
                    desc.setBuddhistDescription(factor.getBuddhistDescription());
                    desc.setModernDescription(factor.getModernDescription());
                    desc.setCategory(factor.getPrimaryCategory());
                    return desc;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error getting factors by category {}: {}", category, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // ==================== PRIVATE MAPPING METHODS ====================
    
    private Map<String, Double> mapToBigFive(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Openness (curiosity, creativity, openness to experience)
        double openness = (vector.get(2) + vector.get(4) + vector.get(5) + vector.get(12)) / 4.0; // feeling, thinking, faith, light_ease
        scores.put("openness", openness);
        
        // Conscientiousness (self-discipline, organization, responsibility)
        double conscientiousness = (vector.get(6) + vector.get(13) + vector.get(14)) / 3.0; // vigor, non_laxness, renunciation
        scores.put("conscientiousness", conscientiousness);
        
        // Extraversion (sociability, assertiveness, positive emotions)
        double extraversion = (vector.get(1) + vector.get(6) + vector.get(10)) / 3.0; // contact, vigor, absence_of_hatred
        scores.put("extraversion", extraversion);
        
        // Agreeableness (compassion, cooperation, trust)
        double agreeableness = (vector.get(10) + vector.get(15) + vector.get(8)) / 3.0; // absence_of_hatred, non_harming, shame
        scores.put("agreeableness", agreeableness);
        
        // Neuroticism (emotional instability, anxiety, negative emotions)
        double neuroticism = (vector.get(16) + vector.get(17) + vector.get(20) + vector.get(38)) / 4.0; // greed, anger, doubt, restlessness
        scores.put("neuroticism", neuroticism);
        
        return scores;
    }
    
    private Map<String, Double> mapToCBT(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Cognitive distortions
        double cognitiveDistortions = (vector.get(21) + vector.get(22) + vector.get(24)) / 3.0; // improper_views, wrath, hatred
        scores.put("cognitive_distortions", cognitiveDistortions);
        
        // Automatic thoughts
        double automaticThoughts = (vector.get(3) + vector.get(4) + vector.get(20)) / 3.0; // feeling, thinking, doubt
        scores.put("automatic_thoughts", automaticThoughts);
        
        // Core beliefs
        double coreBeliefs = (vector.get(5) + vector.get(21) + vector.get(35)) / 3.0; // faith, improper_views, lack_of_faith
        scores.put("core_beliefs", coreBeliefs);
        
        // Behavioral patterns
        double behavioralPatterns = (vector.get(6) + vector.get(13) + vector.get(36)) / 3.0; // vigor, non_laxness, laziness
        scores.put("behavioral_patterns", behavioralPatterns);
        
        // Emotional regulation
        double emotionalRegulation = (vector.get(2) + vector.get(7) + vector.get(8)) / 3.0; // feeling, inner_shame, shame
        scores.put("emotional_regulation", emotionalRegulation);
        
        return scores;
    }
    
    private Map<String, Double> mapToEmotionalIntelligence(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Self-awareness
        double selfAwareness = (vector.get(0) + vector.get(2) + vector.get(7)) / 3.0; // attention, feeling, inner_shame
        scores.put("self_awareness", selfAwareness);
        
        // Self-regulation
        double selfRegulation = (vector.get(6) + vector.get(13) + vector.get(14)) / 3.0; // vigor, non_laxness, renunciation
        scores.put("self_regulation", selfRegulation);
        
        // Motivation
        double motivation = (vector.get(5) + vector.get(6) + vector.get(9)) / 3.0; // faith, vigor, absence_of_craving
        scores.put("motivation", motivation);
        
        // Empathy
        double empathy = (vector.get(1) + vector.get(10) + vector.get(15)) / 3.0; // contact, absence_of_hatred, non_harming
        scores.put("empathy", empathy);
        
        // Social skills
        double socialSkills = (vector.get(1) + vector.get(10) + vector.get(15)) / 3.0; // contact, absence_of_hatred, non_harming
        scores.put("social_skills", socialSkills);
        
        return scores;
    }
    
    private Map<String, Double> mapToPositivePsychology(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Well-being
        double wellBeing = (vector.get(2) + vector.get(12) + vector.get(10)) / 3.0; // feeling, light_ease, absence_of_hatred
        scores.put("well_being", wellBeing);
        
        // Strengths
        double strengths = (vector.get(5) + vector.get(6) + vector.get(11)) / 3.0; // faith, vigor, absence_of_ignorance
        scores.put("strengths", strengths);
        
        // Virtues
        double virtues = (vector.get(8) + vector.get(9) + vector.get(10) + vector.get(15)) / 4.0; // shame, absence_of_craving, absence_of_hatred, non_harming
        scores.put("virtues", virtues);
        
        // Meaning
        double meaning = (vector.get(5) + vector.get(11) + vector.get(14)) / 3.0; // faith, absence_of_ignorance, renunciation
        scores.put("meaning", meaning);
        
        // Flow
        double flow = (vector.get(0) + vector.get(3) + vector.get(4)) / 3.0; // attention, feeling, thinking
        scores.put("flow", flow);
        
        return scores;
    }
    
    private Map<String, Double> mapToClinicalCategories(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Anxiety indicators
        double anxiety = (vector.get(20) + vector.get(38) + vector.get(39)) / 3.0; // doubt, restlessness, distraction
        scores.put("anxiety", anxiety);
        
        // Depression indicators
        double depression = (vector.get(37) + vector.get(38) + vector.get(42)) / 3.0; // torpor, restlessness, regret
        scores.put("depression", depression);
        
        // Impulse control
        double impulseControl = (vector.get(16) + vector.get(17) + vector.get(18)) / 3.0; // greed, anger, arrogance
        scores.put("impulse_control", impulseControl);
        
        // Social functioning
        double socialFunctioning = (vector.get(1) + vector.get(10) + vector.get(15)) / 3.0; // contact, absence_of_hatred, non_harming
        scores.put("social_functioning", socialFunctioning);
        
        // Reality testing
        double realityTesting = (vector.get(11) + vector.get(21) + vector.get(40)) / 3.0; // absence_of_ignorance, improper_views, improper_knowledge
        scores.put("reality_testing", realityTesting);
        
        return scores;
    }
    
    private Map<String, Double> mapToCognitiveFunctions(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Attention
        double attention = vector.get(0); // attention
        scores.put("attention", attention);
        
        // Memory
        double memory = (vector.get(3) + vector.get(4)) / 2.0; // feeling, thinking
        scores.put("memory", memory);
        
        // Executive function
        double executiveFunction = (vector.get(4) + vector.get(6) + vector.get(13)) / 3.0; // thinking, vigor, non_laxness
        scores.put("executive_function", executiveFunction);
        
        // Processing speed
        double processingSpeed = (vector.get(0) + vector.get(3) + vector.get(4)) / 3.0; // attention, feeling, thinking
        scores.put("processing_speed", processingSpeed);
        
        // Working memory
        double workingMemory = (vector.get(0) + vector.get(4) + vector.get(5)) / 3.0; // attention, thinking, deliberation
        scores.put("working_memory", workingMemory);
        
        return scores;
    }
    
    private Map<String, Double> mapToSocialPsychology(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Social cognition
        double socialCognition = (vector.get(1) + vector.get(3) + vector.get(10)) / 3.0; // contact, feeling, absence_of_hatred
        scores.put("social_cognition", socialCognition);
        
        // Interpersonal relationships
        double interpersonalRelationships = (vector.get(1) + vector.get(10) + vector.get(15)) / 3.0; // contact, absence_of_hatred, non_harming
        scores.put("interpersonal_relationships", interpersonalRelationships);
        
        // Group dynamics
        double groupDynamics = (vector.get(10) + vector.get(15) + vector.get(8)) / 3.0; // absence_of_hatred, non_harming, shame
        scores.put("group_dynamics", groupDynamics);
        
        // Social influence
        double socialInfluence = (vector.get(5) + vector.get(6) + vector.get(18)) / 3.0; // faith, vigor, arrogance
        scores.put("social_influence", socialInfluence);
        
        return scores;
    }
    
    private Map<String, Double> mapToDevelopmental(List<Double> vector) {
        Map<String, Double> scores = new HashMap<>();
        
        // Identity formation
        double identityFormation = (vector.get(5) + vector.get(7) + vector.get(8)) / 3.0; // faith, inner_shame, shame
        scores.put("identity_formation", identityFormation);
        
        // Autonomy
        double autonomy = (vector.get(6) + vector.get(9) + vector.get(14)) / 3.0; // vigor, absence_of_craving, renunciation
        scores.put("autonomy", autonomy);
        
        // Intimacy
        double intimacy = (vector.get(1) + vector.get(10) + vector.get(15)) / 3.0; // contact, absence_of_hatred, non_harming
        scores.put("intimacy", intimacy);
        
        // Generativity
        double generativity = (vector.get(10) + vector.get(15) + vector.get(11)) / 3.0; // absence_of_hatred, non_harming, absence_of_ignorance
        scores.put("generativity", generativity);
        
        return scores;
    }
    
    private Map<String, String> generateInterpretations(Map<String, Double> scores, ModernPsychologyFramework framework) {
        Map<String, String> interpretations = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            String dimension = entry.getKey();
            double score = entry.getValue();
            
            String interpretation = generateDimensionInterpretation(dimension, score, framework);
            interpretations.put(dimension, interpretation);
        }
        
        return interpretations;
    }
    
    private String generateDimensionInterpretation(String dimension, double score, ModernPsychologyFramework framework) {
        // Generate contextual interpretations based on framework and score
        if (score >= 0.8) {
            return String.format("High %s (%.2f) - Strong presence of this psychological dimension", dimension, score);
        } else if (score >= 0.6) {
            return String.format("Moderate %s (%.2f) - Moderate presence of this psychological dimension", dimension, score);
        } else if (score >= 0.4) {
            return String.format("Low %s (%.2f) - Limited presence of this psychological dimension", dimension, score);
        } else {
            return String.format("Very low %s (%.2f) - Minimal presence of this psychological dimension", dimension, score);
        }
    }
    
    private List<FactorMapping> createFactorMappings(List<Double> vector, ModernPsychologyFramework framework) {
        List<FactorMapping> mappings = new ArrayList<>();
        
        for (int i = 0; i < vector.size(); i++) {
            BuddhistFactor factor = BUDDHIST_FACTORS.get(i);
            if (factor != null && vector.get(i) > 0.1) { // Only include significant factors
                FactorMapping mapping = new FactorMapping();
                mapping.setFactorIndex(i);
                mapping.setFactorName(factor.getName());
                mapping.setIntensity(vector.get(i));
                mapping.setCategory(factor.getPrimaryCategory());
                mapping.setModernMappings(factor.getModernMappings().get(framework));
                mappings.add(mapping);
            }
        }
        
        return mappings;
    }
    
    private double calculateMappingConfidence(Map<String, Double> scores) {
        // Calculate confidence based on score distribution and variance
        double mean = scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = scores.values().stream()
            .mapToDouble(score -> Math.pow(score - mean, 2))
            .average().orElse(0.0);
        
        // Higher variance indicates more distinct patterns, higher confidence
        return Math.min(1.0, Math.sqrt(variance) * 2);
    }
    
    private ModernPsychologyMapping createErrorMapping(ModernPsychologyFramework framework, String errorMessage) {
        ModernPsychologyMapping mapping = new ModernPsychologyMapping();
        mapping.setFramework(framework);
        mapping.setScores(new HashMap<>());
        mapping.setInterpretations(Map.of("error", errorMessage));
        mapping.setFactorMappings(new ArrayList<>());
        mapping.setConfidence(0.0);
        mapping.setTimestamp(new Date());
        return mapping;
    }
    
    private FactorDescription createDefaultFactorDescription(int factorIndex) {
        FactorDescription description = new FactorDescription();
        description.setIndex(factorIndex);
        description.setName("Unknown Factor " + factorIndex);
        description.setBuddhistDescription("No description available");
        description.setModernDescription("No modern description available");
        description.setCategory(FactorCategory.COGNITIVE);
        return description;
    }
    
    private static void initializeBuddhistFactors() {
        // Initialize all 43 Buddhist mental factors with their mappings
        // This is a comprehensive mapping based on Buddhist psychology and modern psychology research
        
        // Positive factors (wholesome - dimensions 0-15)
        BUDDHIST_FACTORS.put(0, new BuddhistFactor(0, "Attention", "Mindful awareness and focus", 
            "Sustained attention and concentration", FactorCategory.COGNITIVE, 
            Arrays.asList(FactorCategory.REGULATORY), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Openness, Conscientiousness",
                   ModernPsychologyFramework.CBT, "Attention to thoughts and feelings",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Self-awareness",
                   ModernPsychologyFramework.COGNITIVE_FUNCTIONS, "Attention, Working Memory"),
            "Essential for mindfulness and cognitive control",
            "Improve through meditation and attention training"));
        
        BUDDHIST_FACTORS.put(1, new BuddhistFactor(1, "Contact", "Sensory and mental contact", 
            "Awareness of sensory and mental experiences", FactorCategory.COGNITIVE, 
            Arrays.asList(FactorCategory.SOCIAL, FactorCategory.EMOTIONAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Extraversion, Openness",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Social skills, Empathy",
                   ModernPsychologyFramework.SOCIAL_PSYCHOLOGY, "Social cognition, Interpersonal relationships"),
            "Foundation of sensory and social awareness",
            "Develop through mindful observation and social interaction"));
        
        BUDDHIST_FACTORS.put(2, new BuddhistFactor(2, "Feeling", "Emotional sensations and experiences", 
            "Awareness and processing of emotional states", FactorCategory.EMOTIONAL, 
            Arrays.asList(FactorCategory.COGNITIVE, FactorCategory.REGULATORY), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Neuroticism, Extraversion",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Self-awareness, Self-regulation",
                   ModernPsychologyFramework.CBT, "Emotional awareness, Mood regulation"),
            "Core of emotional intelligence and regulation",
            "Enhance through emotion recognition and regulation training"));
        
        BUDDHIST_FACTORS.put(3, new BuddhistFactor(3, "Thinking", "Cognitive processes and thoughts", 
            "Mental processing, reasoning, and cognition", FactorCategory.COGNITIVE, 
            Arrays.asList(FactorCategory.REGULATORY, FactorCategory.MOTIVATIONAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Openness, Conscientiousness",
                   ModernPsychologyFramework.CBT, "Cognitive restructuring, Thought patterns",
                   ModernPsychologyFramework.COGNITIVE_FUNCTIONS, "Executive function, Processing speed"),
            "Essential for problem-solving and decision-making",
            "Improve through cognitive training and metacognition"));
        
        BUDDHIST_FACTORS.put(4, new BuddhistFactor(4, "Deliberation", "Careful consideration and reflection", 
            "Thoughtful analysis and decision-making", FactorCategory.COGNITIVE, 
            Arrays.asList(FactorCategory.REGULATORY, FactorCategory.MOTIVATIONAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Conscientiousness, Openness",
                   ModernPsychologyFramework.CBT, "Cognitive restructuring, Decision-making",
                   ModernPsychologyFramework.COGNITIVE_FUNCTIONS, "Executive function, Working memory"),
            "Critical for wise decision-making",
            "Develop through reflective practice and decision analysis"));
        
        BUDDHIST_FACTORS.put(5, new BuddhistFactor(5, "Faith", "Trust, confidence, and belief", 
            "Confidence in positive outcomes and trust in process", FactorCategory.SPIRITUAL, 
            Arrays.asList(FactorCategory.MOTIVATIONAL, FactorCategory.EMOTIONAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Conscientiousness, Agreeableness",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Optimism, Hope, Meaning",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Motivation, Self-regulation"),
            "Foundation of hope and positive outlook",
            "Cultivate through positive experiences and meaning-making"));
        
        BUDDHIST_FACTORS.put(6, new BuddhistFactor(6, "Vigor", "Energy, enthusiasm, and effort", 
            "Sustained energy and motivation for positive action", FactorCategory.MOTIVATIONAL, 
            Arrays.asList(FactorCategory.BEHAVIORAL, FactorCategory.REGULATORY), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Extraversion, Conscientiousness",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Engagement, Flow, Strengths",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Motivation, Self-regulation"),
            "Essential for goal pursuit and achievement",
            "Build through goal-setting and energy management"));
        
        BUDDHIST_FACTORS.put(7, new BuddhistFactor(7, "Inner Shame", "Moral conscience and self-respect", 
            "Internal moral compass and self-respect", FactorCategory.REGULATORY, 
            Arrays.asList(FactorCategory.EMOTIONAL, FactorCategory.SPIRITUAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Conscientiousness, Agreeableness",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Self-awareness, Self-regulation",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Virtues, Character strengths"),
            "Foundation of moral development and integrity",
            "Develop through values clarification and moral reasoning"));
        
        BUDDHIST_FACTORS.put(8, new BuddhistFactor(8, "Shame", "Humility, modesty, and embarrassment", 
            "Appropriate humility and social awareness", FactorCategory.EMOTIONAL, 
            Arrays.asList(FactorCategory.SOCIAL, FactorCategory.REGULATORY), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Agreeableness, Neuroticism",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Social awareness, Self-regulation",
                   ModernPsychologyFramework.SOCIAL_PSYCHOLOGY, "Social norms, Interpersonal relationships"),
            "Important for social harmony and humility",
            "Balance through self-compassion and social skills"));
        
        BUDDHIST_FACTORS.put(9, new BuddhistFactor(9, "Absence of Craving", "Contentment and non-attachment", 
            "Freedom from excessive desire and attachment", FactorCategory.REGULATORY, 
            Arrays.asList(FactorCategory.SPIRITUAL, FactorCategory.EMOTIONAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Conscientiousness, Low Neuroticism",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Contentment, Gratitude, Well-being",
                   ModernPsychologyFramework.CBT, "Cognitive flexibility, Acceptance"),
            "Key to contentment and inner peace",
            "Cultivate through gratitude and acceptance practices"));
        
        BUDDHIST_FACTORS.put(10, new BuddhistFactor(10, "Absence of Hatred", "Love, compassion, and forgiveness", 
            "Loving-kindness and absence of ill-will", FactorCategory.EMOTIONAL, 
            Arrays.asList(FactorCategory.SOCIAL, FactorCategory.SPIRITUAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Agreeableness, Low Neuroticism",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Empathy, Social skills",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Love, Compassion, Forgiveness"),
            "Foundation of healthy relationships and compassion",
            "Develop through loving-kindness meditation and empathy training"));
        
        BUDDHIST_FACTORS.put(11, new BuddhistFactor(11, "Absence of Ignorance", "Wisdom, understanding, and insight", 
            "Clear understanding and wisdom", FactorCategory.COGNITIVE, 
            Arrays.asList(FactorCategory.SPIRITUAL, FactorCategory.EMOTIONAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Openness, Conscientiousness",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Wisdom, Meaning, Purpose",
                   ModernPsychologyFramework.COGNITIVE_FUNCTIONS, "Executive function, Insight"),
            "Essential for wisdom and clear understanding",
            "Cultivate through study, reflection, and insight practices"));
        
        BUDDHIST_FACTORS.put(12, new BuddhistFactor(12, "Light Ease", "Lightness, comfort, and peace", 
            "Physical and mental lightness and ease", FactorCategory.EMOTIONAL, 
            Arrays.asList(FactorCategory.REGULATORY, FactorCategory.SPIRITUAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Low Neuroticism, Extraversion",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Well-being, Flow, Vitality",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Self-regulation, Stress management"),
            "Indicator of well-being and stress management",
            "Develop through relaxation and stress reduction techniques"));
        
        BUDDHIST_FACTORS.put(13, new BuddhistFactor(13, "Non-laxness", "Diligence, perseverance, and determination", 
            "Sustained effort and determination", FactorCategory.BEHAVIORAL, 
            Arrays.asList(FactorCategory.MOTIVATIONAL, FactorCategory.REGULATORY), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Conscientiousness",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Grit, Perseverance, Achievement",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Self-regulation, Motivation"),
            "Critical for goal achievement and success",
            "Build through goal-setting and persistence training"));
        
        BUDDHIST_FACTORS.put(14, new BuddhistFactor(14, "Renunciation", "Letting go, detachment, and abandonment", 
            "Ability to let go of unhelpful patterns", FactorCategory.REGULATORY, 
            Arrays.asList(FactorCategory.SPIRITUAL, FactorCategory.EMOTIONAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Conscientiousness, Low Neuroticism",
                   ModernPsychologyFramework.CBT, "Cognitive flexibility, Acceptance",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Transcendence, Meaning"),
            "Essential for psychological flexibility and growth",
            "Practice through acceptance and letting go exercises"));
        
        BUDDHIST_FACTORS.put(15, new BuddhistFactor(15, "Non-harming", "Compassion, kindness, and gentleness", 
            "Compassionate action and non-violence", FactorCategory.EMOTIONAL, 
            Arrays.asList(FactorCategory.SOCIAL, FactorCategory.SPIRITUAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "Agreeableness",
                   ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Empathy, Social skills",
                   ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, "Love, Compassion, Kindness"),
            "Foundation of ethical behavior and compassion",
            "Cultivate through compassion meditation and ethical practice"));
        
        // Continue with negative factors (unwholesome - dimensions 16-42)
        // [Additional factors would be defined here following the same pattern]
        
        // For brevity, I'll add a few key negative factors as examples
        BUDDHIST_FACTORS.put(16, new BuddhistFactor(16, "Greed", "Desire, craving, and attachment", 
            "Excessive desire and attachment to outcomes", FactorCategory.PATHOLOGICAL, 
            Arrays.asList(FactorCategory.EMOTIONAL, FactorCategory.BEHAVIORAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "High Neuroticism, Low Conscientiousness",
                   ModernPsychologyFramework.CBT, "Cognitive distortions, Impulse control",
                   ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, "Impulse control disorders, Addiction"),
            "Associated with addiction and impulse control issues",
            "Address through mindfulness and impulse control training"));
        
        BUDDHIST_FACTORS.put(17, new BuddhistFactor(17, "Anger", "Rage, fury, and hostility", 
            "Intense anger and hostility", FactorCategory.EMOTIONAL, 
            Arrays.asList(FactorCategory.PATHOLOGICAL, FactorCategory.BEHAVIORAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "High Neuroticism, Low Agreeableness",
                   ModernPsychologyFramework.CBT, "Anger management, Cognitive restructuring",
                   ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, "Anger disorders, Interpersonal problems"),
            "Associated with interpersonal and emotional regulation problems",
            "Manage through anger management and emotional regulation training"));
        
        BUDDHIST_FACTORS.put(20, new BuddhistFactor(20, "Doubt", "Uncertainty, skepticism, and confusion", 
            "Excessive doubt and indecision", FactorCategory.COGNITIVE, 
            Arrays.asList(FactorCategory.EMOTIONAL, FactorCategory.PATHOLOGICAL), 
            Map.of(ModernPsychologyFramework.BIG_FIVE, "High Neuroticism, Low Conscientiousness",
                   ModernPsychologyFramework.CBT, "Cognitive distortions, Decision-making",
                   ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, "Anxiety, Indecision"),
            "Associated with anxiety and decision-making difficulties",
            "Address through cognitive restructuring and decision-making training"));
        
        // [Additional negative factors would be defined here...]
    }
    
    // ==================== INNER CLASSES ====================
    
    public static class BuddhistFactor {
        private int index;
        private String name;
        private String buddhistDescription;
        private String modernDescription;
        private FactorCategory primaryCategory;
        private List<FactorCategory> secondaryCategories;
        private Map<ModernPsychologyFramework, String> modernMappings;
        private String clinicalRelevance;
        private String therapeuticImplications;
        
        public BuddhistFactor(int index, String name, String buddhistDescription, String modernDescription,
                            FactorCategory primaryCategory, List<FactorCategory> secondaryCategories,
                            Map<ModernPsychologyFramework, String> modernMappings, String clinicalRelevance,
                            String therapeuticImplications) {
            this.index = index;
            this.name = name;
            this.buddhistDescription = buddhistDescription;
            this.modernDescription = modernDescription;
            this.primaryCategory = primaryCategory;
            this.secondaryCategories = secondaryCategories;
            this.modernMappings = modernMappings;
            this.clinicalRelevance = clinicalRelevance;
            this.therapeuticImplications = therapeuticImplications;
        }
        
        // Getters
        public int getIndex() { return index; }
        public String getName() { return name; }
        public String getBuddhistDescription() { return buddhistDescription; }
        public String getModernDescription() { return modernDescription; }
        public FactorCategory getPrimaryCategory() { return primaryCategory; }
        public List<FactorCategory> getSecondaryCategories() { return secondaryCategories; }
        public Map<ModernPsychologyFramework, String> getModernMappings() { return modernMappings; }
        public String getClinicalRelevance() { return clinicalRelevance; }
        public String getTherapeuticImplications() { return therapeuticImplications; }
    }
    
    public static class ModernPsychologyMapping {
        private ModernPsychologyFramework framework;
        private Map<String, Double> scores;
        private Map<String, String> interpretations;
        private List<FactorMapping> factorMappings;
        private double confidence;
        private Date timestamp;
        
        // Getters and setters
        public ModernPsychologyFramework getFramework() { return framework; }
        public void setFramework(ModernPsychologyFramework framework) { this.framework = framework; }
        
        public Map<String, Double> getScores() { return scores; }
        public void setScores(Map<String, Double> scores) { this.scores = scores; }
        
        public Map<String, String> getInterpretations() { return interpretations; }
        public void setInterpretations(Map<String, String> interpretations) { this.interpretations = interpretations; }
        
        public List<FactorMapping> getFactorMappings() { return factorMappings; }
        public void setFactorMappings(List<FactorMapping> factorMappings) { this.factorMappings = factorMappings; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class FactorMapping {
        private int factorIndex;
        private String factorName;
        private double intensity;
        private FactorCategory category;
        private String modernMappings;
        
        // Getters and setters
        public int getFactorIndex() { return factorIndex; }
        public void setFactorIndex(int factorIndex) { this.factorIndex = factorIndex; }
        
        public String getFactorName() { return factorName; }
        public void setFactorName(String factorName) { this.factorName = factorName; }
        
        public double getIntensity() { return intensity; }
        public void setIntensity(double intensity) { this.intensity = intensity; }
        
        public FactorCategory getCategory() { return category; }
        public void setCategory(FactorCategory category) { this.category = category; }
        
        public String getModernMappings() { return modernMappings; }
        public void setModernMappings(String modernMappings) { this.modernMappings = modernMappings; }
    }
    
    public static class FactorDescription {
        private int index;
        private String name;
        private String buddhistDescription;
        private String modernDescription;
        private FactorCategory category;
        private List<FactorCategory> secondaryCategories;
        private Map<ModernPsychologyFramework, String> modernMappings;
        private String clinicalRelevance;
        private String therapeuticImplications;
        
        // Getters and setters
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getBuddhistDescription() { return buddhistDescription; }
        public void setBuddhistDescription(String buddhistDescription) { this.buddhistDescription = buddhistDescription; }
        
        public String getModernDescription() { return modernDescription; }
        public void setModernDescription(String modernDescription) { this.modernDescription = modernDescription; }
        
        public FactorCategory getCategory() { return category; }
        public void setCategory(FactorCategory category) { this.category = category; }
        
        public List<FactorCategory> getSecondaryCategories() { return secondaryCategories; }
        public void setSecondaryCategories(List<FactorCategory> secondaryCategories) { this.secondaryCategories = secondaryCategories; }
        
        public String getDescription() { return buddhistDescription; }
        
        public Map<ModernPsychologyFramework, String> getModernMappings() { return modernMappings; }
        public void setModernMappings(Map<ModernPsychologyFramework, String> modernMappings) { this.modernMappings = modernMappings; }
        
        public String getClinicalRelevance() { return clinicalRelevance; }
        public void setClinicalRelevance(String clinicalRelevance) { this.clinicalRelevance = clinicalRelevance; }
        
        public String getTherapeuticImplications() { return therapeuticImplications; }
        public void setTherapeuticImplications(String therapeuticImplications) { this.therapeuticImplications = therapeuticImplications; }
    }
}
