package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross-mapping bridge between Buddhist and modern psychology frameworks
 * 
 * This service provides sophisticated cross-mapping capabilities between:
 * - 43-dimensional Buddhist mental factors system
 * - Modern psychological frameworks (Big Five, CBT, EI, etc.)
 * - Clinical psychology categories
 * - Therapeutic intervention strategies
 * 
 * The bridge ensures accurate translation and validation of psychological
 * concepts across different cultural and theoretical frameworks.
 */
@Service
public class ModernPsychologyBridge {
    
    private static final Logger logger = LoggerFactory.getLogger(ModernPsychologyBridge.class);
    
    @Autowired
    private BuddhistPsychologyMapper buddhistPsychologyMapper;
    
    // Mapping confidence thresholds
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.6;
    private static final double LOW_CONFIDENCE_THRESHOLD = 0.4;
    
    /**
     * Find equivalent concepts between Buddhist and modern psychology
     * 
     * @param buddhistFactor Buddhist mental factor name or index
     * @param targetFramework Target modern psychology framework
     * @return Equivalent concept mapping with confidence score
     */
    public EquivalentMapping findEquivalent(String buddhistFactor, BuddhistPsychologyMapper.ModernPsychologyFramework targetFramework) {
        try {
            logger.debug("Finding equivalent for Buddhist factor '{}' in {} framework", buddhistFactor, targetFramework);
            
            // Parse Buddhist factor (could be name or index)
            int factorIndex = parseBuddhistFactor(buddhistFactor);
            if (factorIndex < 0 || factorIndex >= 43) {
                return createErrorMapping("Invalid Buddhist factor: " + buddhistFactor);
            }
            
            // Get factor description
            BuddhistPsychologyMapper.FactorDescription factorDesc = buddhistPsychologyMapper.getFactorDescription(factorIndex);
            
            // Find equivalent in target framework
            String equivalent = findFrameworkEquivalent(factorDesc, targetFramework);
            double confidence = calculateEquivalenceConfidence(factorDesc, targetFramework);
            
            // Get therapeutic implications
            List<String> therapeuticImplications = getTherapeuticImplications(factorDesc, targetFramework);
            
            // Get cultural considerations
            List<String> culturalConsiderations = getCulturalConsiderations(factorDesc, targetFramework);
            
            EquivalentMapping mapping = new EquivalentMapping();
            mapping.setBuddhistFactor(factorDesc.getName());
            mapping.setBuddhistDescription(factorDesc.getBuddhistDescription());
            mapping.setTargetFramework(targetFramework);
            mapping.setEquivalent(equivalent);
            mapping.setConfidence(confidence);
            mapping.setTherapeuticImplications(therapeuticImplications);
            mapping.setCulturalConsiderations(culturalConsiderations);
            mapping.setValidationStatus(validateMapping(factorDesc, equivalent, targetFramework));
            mapping.setTimestamp(new Date());
            
            logger.debug("Found equivalent: {} -> {} (confidence: {})", 
                        factorDesc.getName(), equivalent, confidence);
            
            return mapping;
            
        } catch (Exception e) {
            logger.error("Error finding equivalent for '{}' in {}: {}", 
                        buddhistFactor, targetFramework, e.getMessage(), e);
            return createErrorMapping("Error finding equivalent: " + e.getMessage());
        }
    }
    
    /**
     * Translate a category from Buddhist to modern psychology
     * 
     * @param buddhistCategory Buddhist factor category
     * @param targetFramework Target modern psychology framework
     * @return Category translation with examples
     */
    public CategoryTranslation translateCategory(BuddhistPsychologyMapper.FactorCategory buddhistCategory, 
                                               BuddhistPsychologyMapper.ModernPsychologyFramework targetFramework) {
        try {
            logger.debug("Translating category {} to {} framework", buddhistCategory, targetFramework);
            
            // Get category mapping
            String modernCategory = getCategoryMapping(buddhistCategory, targetFramework);
            String description = getCategoryDescription(buddhistCategory, targetFramework);
            
            // Get examples from this category
            List<BuddhistPsychologyMapper.FactorDescription> categoryFactors = 
                buddhistPsychologyMapper.getFactorsByCategory(buddhistCategory);
            
            // Create examples
            List<CategoryExample> examples = categoryFactors.stream()
                .limit(5) // Limit to 5 examples
                .map(factor -> {
                    CategoryExample example = new CategoryExample();
                    example.setBuddhistFactor(factor.getName());
                    example.setModernEquivalent(findFrameworkEquivalent(factor, targetFramework));
                    example.setConfidence(calculateEquivalenceConfidence(factor, targetFramework));
                    return example;
                })
                .collect(Collectors.toList());
            
            // Get therapeutic approaches
            List<String> therapeuticApproaches = getTherapeuticApproaches(buddhistCategory, targetFramework);
            
            CategoryTranslation translation = new CategoryTranslation();
            translation.setBuddhistCategory(buddhistCategory);
            translation.setTargetFramework(targetFramework);
            translation.setModernCategory(modernCategory);
            translation.setDescription(description);
            translation.setExamples(examples);
            translation.setTherapeuticApproaches(therapeuticApproaches);
            translation.setConfidence(calculateCategoryConfidence(examples));
            translation.setTimestamp(new Date());
            
            logger.debug("Translated category {} to {} (confidence: {})", 
                        buddhistCategory, modernCategory, translation.getConfidence());
            
            return translation;
            
        } catch (Exception e) {
            logger.error("Error translating category {} to {}: {}", 
                        buddhistCategory, targetFramework, e.getMessage(), e);
            return createErrorCategoryTranslation(buddhistCategory, targetFramework, e.getMessage());
        }
    }
    
    /**
     * Validate mapping between Buddhist and modern psychology concepts
     * 
     * @param buddhistFactor Buddhist factor description
     * @param modernEquivalent Modern psychology equivalent
     * @param framework Target framework
     * @return Validation result with confidence and issues
     */
    public ValidationResult validateMapping(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                          String modernEquivalent, 
                                          BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        try {
            logger.debug("Validating mapping: {} -> {} in {}", 
                        buddhistFactor.getName(), modernEquivalent, framework);
            
            ValidationResult result = new ValidationResult();
            result.setBuddhistFactor(buddhistFactor.getName());
            result.setModernEquivalent(modernEquivalent);
            result.setFramework(framework);
            
            // Check conceptual alignment
            double conceptualAlignment = checkConceptualAlignment(buddhistFactor, modernEquivalent, framework);
            result.setConceptualAlignment(conceptualAlignment);
            
            // Check cultural appropriateness
            double culturalAppropriateness = checkCulturalAppropriateness(buddhistFactor, modernEquivalent, framework);
            result.setCulturalAppropriateness(culturalAppropriateness);
            
            // Check clinical validity
            double clinicalValidity = checkClinicalValidity(buddhistFactor, modernEquivalent, framework);
            result.setClinicalValidity(clinicalValidity);
            
            // Check therapeutic utility
            double therapeuticUtility = checkTherapeuticUtility(buddhistFactor, modernEquivalent, framework);
            result.setTherapeuticUtility(therapeuticUtility);
            
            // Calculate overall confidence
            double overallConfidence = (conceptualAlignment + culturalAppropriateness + 
                                      clinicalValidity + therapeuticUtility) / 4.0;
            result.setOverallConfidence(overallConfidence);
            
            // Identify issues
            List<String> issues = identifyValidationIssues(result);
            result.setIssues(issues);
            
            // Generate recommendations
            List<String> recommendations = generateValidationRecommendations(result);
            result.setRecommendations(recommendations);
            
            result.setTimestamp(new Date());
            
            logger.debug("Validation completed: {} (confidence: {})", 
                        buddhistFactor.getName(), overallConfidence);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating mapping: {}", e.getMessage(), e);
            return createErrorValidationResult(buddhistFactor.getName(), modernEquivalent, framework, e.getMessage());
        }
    }
    
    /**
     * Get comprehensive cross-mapping between all frameworks
     * 
     * @param buddhistVector 43-dimensional Buddhist mental factors vector
     * @return Comprehensive cross-mapping across all frameworks
     */
    public ComprehensiveMapping getComprehensiveMapping(List<Double> buddhistVector) {
        try {
            logger.debug("Generating comprehensive mapping for 43D vector");
            
            ComprehensiveMapping mapping = new ComprehensiveMapping();
            mapping.setBuddhistVector(buddhistVector);
            
            // Map to all modern psychology frameworks
            Map<BuddhistPsychologyMapper.ModernPsychologyFramework, BuddhistPsychologyMapper.ModernPsychologyMapping> frameworkMappings = new HashMap<>();
            
            for (BuddhistPsychologyMapper.ModernPsychologyFramework framework : 
                 BuddhistPsychologyMapper.ModernPsychologyFramework.values()) {
                
                BuddhistPsychologyMapper.ModernPsychologyMapping frameworkMapping = 
                    buddhistPsychologyMapper.mapToModernPsychology(buddhistVector, framework);
                frameworkMappings.put(framework, frameworkMapping);
            }
            
            mapping.setFrameworkMappings(frameworkMappings);
            
            // Generate cross-framework insights
            List<CrossFrameworkInsight> insights = generateCrossFrameworkInsights(frameworkMappings);
            mapping.setCrossFrameworkInsights(insights);
            
            // Calculate mapping consistency
            double consistency = calculateMappingConsistency(frameworkMappings);
            mapping.setConsistency(consistency);
            
            // Generate therapeutic recommendations
            List<TherapeuticRecommendation> recommendations = generateTherapeuticRecommendations(frameworkMappings);
            mapping.setTherapeuticRecommendations(recommendations);
            
            mapping.setTimestamp(new Date());
            
            logger.debug("Comprehensive mapping completed (consistency: {})", consistency);
            
            return mapping;
            
        } catch (Exception e) {
            logger.error("Error generating comprehensive mapping: {}", e.getMessage(), e);
            return createErrorComprehensiveMapping(buddhistVector, e.getMessage());
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private int parseBuddhistFactor(String buddhistFactor) {
        try {
            // Try to parse as index first
            return Integer.parseInt(buddhistFactor);
        } catch (NumberFormatException e) {
            // If not a number, search by name
            for (int i = 0; i < 43; i++) {
                BuddhistPsychologyMapper.FactorDescription desc = buddhistPsychologyMapper.getFactorDescription(i);
                if (desc.getName().equalsIgnoreCase(buddhistFactor)) {
                    return i;
                }
            }
            return -1; // Not found
        }
    }
    
    private String findFrameworkEquivalent(BuddhistPsychologyMapper.FactorDescription factorDesc, 
                                         BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Get the modern mapping for this framework
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, String> mappings = factorDesc.getModernMappings();
        return mappings.getOrDefault(framework, "No direct equivalent found");
    }
    
    private double calculateEquivalenceConfidence(BuddhistPsychologyMapper.FactorDescription factorDesc, 
                                                BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Calculate confidence based on factor characteristics and framework alignment
        double baseConfidence = 0.5;
        
        // Adjust based on category alignment
        BuddhistPsychologyMapper.FactorCategory category = factorDesc.getCategory();
        double categoryAlignment = getCategoryAlignment(category, framework);
        
        // Adjust based on clinical relevance
        double clinicalRelevance = factorDesc.getClinicalRelevance() != null ? 0.2 : 0.0;
        
        // Adjust based on therapeutic implications
        double therapeuticImplications = factorDesc.getTherapeuticImplications() != null ? 0.2 : 0.0;
        
        double confidence = baseConfidence + (categoryAlignment * 0.3) + clinicalRelevance + therapeuticImplications;
        return Math.min(1.0, confidence);
    }
    
    private double getCategoryAlignment(BuddhistPsychologyMapper.FactorCategory category, 
                                      BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Define category alignment scores for each framework
        Map<BuddhistPsychologyMapper.FactorCategory, Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double>> alignments = 
            getCategoryAlignmentMap();
        
        return alignments.getOrDefault(category, new HashMap<>())
                        .getOrDefault(framework, 0.5);
    }
    
    private Map<BuddhistPsychologyMapper.FactorCategory, Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double>> getCategoryAlignmentMap() {
        Map<BuddhistPsychologyMapper.FactorCategory, Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double>> alignments = new HashMap<>();
        
        // Cognitive factors align well with CBT and cognitive functions
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double> cognitiveAlignments = new HashMap<>();
        cognitiveAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.9);
        cognitiveAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.COGNITIVE_FUNCTIONS, 0.9);
        cognitiveAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.7);
        alignments.put(BuddhistPsychologyMapper.FactorCategory.COGNITIVE, cognitiveAlignments);
        
        // Emotional factors align well with EI and clinical categories
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double> emotionalAlignments = new HashMap<>();
        emotionalAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.9);
        emotionalAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.8);
        emotionalAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.8);
        alignments.put(BuddhistPsychologyMapper.FactorCategory.EMOTIONAL, emotionalAlignments);
        
        // Social factors align well with social psychology and EI
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double> socialAlignments = new HashMap<>();
        socialAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.SOCIAL_PSYCHOLOGY, 0.9);
        socialAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.8);
        socialAlignments.put(BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.7);
        alignments.put(BuddhistPsychologyMapper.FactorCategory.SOCIAL, socialAlignments);
        
        // Add more category alignments as needed...
        
        return alignments;
    }
    
    private List<String> getTherapeuticImplications(BuddhistPsychologyMapper.FactorDescription factorDesc, 
                                                   BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        List<String> implications = new ArrayList<>();
        
        // Add framework-specific therapeutic implications
        switch (framework) {
            case CBT:
                implications.add("Cognitive restructuring techniques");
                implications.add("Behavioral activation");
                implications.add("Thought challenging exercises");
                break;
            case EMOTIONAL_INTELLIGENCE:
                implications.add("Emotion recognition training");
                implications.add("Emotional regulation strategies");
                implications.add("Empathy development exercises");
                break;
            case POSITIVE_PSYCHOLOGY:
                implications.add("Strengths-based interventions");
                implications.add("Gratitude practices");
                implications.add("Meaning-making exercises");
                break;
            case CLINICAL_PSYCHOLOGY:
                implications.add("Clinical assessment protocols");
                implications.add("Evidence-based interventions");
                implications.add("Treatment planning considerations");
                break;
            default:
                implications.add("General therapeutic approaches");
        }
        
        // Add factor-specific implications
        if (factorDesc.getTherapeuticImplications() != null) {
            implications.add(factorDesc.getTherapeuticImplications());
        }
        
        return implications;
    }
    
    private List<String> getCulturalConsiderations(BuddhistPsychologyMapper.FactorDescription factorDesc, 
                                                  BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        List<String> considerations = new ArrayList<>();
        
        // Add cultural considerations for cross-cultural application
        considerations.add("Consider cultural context in interpretation");
        considerations.add("Adapt therapeutic approaches to cultural background");
        considerations.add("Respect cultural values and beliefs");
        
        // Add framework-specific cultural considerations
        switch (framework) {
            case BIG_FIVE:
                considerations.add("Big Five may vary across cultures");
                considerations.add("Consider cultural norms for personality expression");
                break;
            case CBT:
                considerations.add("CBT techniques may need cultural adaptation");
                considerations.add("Consider cultural beliefs about thoughts and emotions");
                break;
            case EMOTIONAL_INTELLIGENCE:
                considerations.add("Emotional expression norms vary across cultures");
                considerations.add("Consider cultural definitions of emotional intelligence");
                break;
        }
        
        return considerations;
    }
    
    private String getCategoryMapping(BuddhistPsychologyMapper.FactorCategory buddhistCategory, 
                                    BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Map Buddhist categories to modern psychology categories
        Map<BuddhistPsychologyMapper.FactorCategory, Map<BuddhistPsychologyMapper.ModernPsychologyFramework, String>> mappings = 
            getCategoryMappingMap();
        
        return mappings.getOrDefault(buddhistCategory, new HashMap<>())
                      .getOrDefault(framework, "General psychological category");
    }
    
    private Map<BuddhistPsychologyMapper.FactorCategory, Map<BuddhistPsychologyMapper.ModernPsychologyFramework, String>> getCategoryMappingMap() {
        Map<BuddhistPsychologyMapper.FactorCategory, Map<BuddhistPsychologyMapper.ModernPsychologyFramework, String>> mappings = new HashMap<>();
        
        // Cognitive category mappings
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, String> cognitiveMappings = new HashMap<>();
        cognitiveMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, "Openness, Conscientiousness");
        cognitiveMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, "Cognitive processes");
        cognitiveMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.COGNITIVE_FUNCTIONS, "Executive functions");
        mappings.put(BuddhistPsychologyMapper.FactorCategory.COGNITIVE, cognitiveMappings);
        
        // Emotional category mappings
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, String> emotionalMappings = new HashMap<>();
        emotionalMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, "Neuroticism, Extraversion");
        emotionalMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Emotional awareness and regulation");
        emotionalMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, "Mood and anxiety disorders");
        mappings.put(BuddhistPsychologyMapper.FactorCategory.EMOTIONAL, emotionalMappings);
        
        // Social category mappings
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, String> socialMappings = new HashMap<>();
        socialMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, "Agreeableness, Extraversion");
        socialMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, "Social skills, Empathy");
        socialMappings.put(BuddhistPsychologyMapper.ModernPsychologyFramework.SOCIAL_PSYCHOLOGY, "Interpersonal relationships");
        mappings.put(BuddhistPsychologyMapper.FactorCategory.SOCIAL, socialMappings);
        
        // Add more category mappings as needed...
        
        return mappings;
    }
    
    private String getCategoryDescription(BuddhistPsychologyMapper.FactorCategory buddhistCategory, 
                                        BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Generate descriptions for category translations
        return String.format("Buddhist %s factors translate to modern %s concepts", 
                           buddhistCategory.name().toLowerCase(), framework.name().toLowerCase());
    }
    
    private List<String> getTherapeuticApproaches(BuddhistPsychologyMapper.FactorCategory buddhistCategory, 
                                                 BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        List<String> approaches = new ArrayList<>();
        
        // Add category-specific therapeutic approaches
        switch (buddhistCategory) {
            case COGNITIVE:
                approaches.add("Cognitive restructuring");
                approaches.add("Metacognitive training");
                approaches.add("Attention training");
                break;
            case EMOTIONAL:
                approaches.add("Emotion regulation training");
                approaches.add("Mindfulness-based interventions");
                approaches.add("Emotional awareness exercises");
                break;
            case SOCIAL:
                approaches.add("Social skills training");
                approaches.add("Interpersonal therapy");
                approaches.add("Communication skills development");
                break;
            case BEHAVIORAL:
                approaches.add("Behavioral activation");
                approaches.add("Habit formation strategies");
                approaches.add("Behavioral modification");
                break;
            case MOTIVATIONAL:
                approaches.add("Motivational interviewing");
                approaches.add("Goal-setting strategies");
                approaches.add("Intrinsic motivation enhancement");
                break;
            case REGULATORY:
                approaches.add("Self-regulation training");
                approaches.add("Impulse control strategies");
                approaches.add("Executive function training");
                break;
            case SPIRITUAL:
                approaches.add("Meaning-making interventions");
                approaches.add("Values clarification");
                approaches.add("Transcendence practices");
                break;
            case PATHOLOGICAL:
                approaches.add("Clinical interventions");
                approaches.add("Evidence-based treatments");
                approaches.add("Therapeutic assessment");
                break;
        }
        
        return approaches;
    }
    
    private double calculateCategoryConfidence(List<CategoryExample> examples) {
        if (examples.isEmpty()) {
            return 0.0;
        }
        
        double averageConfidence = examples.stream()
            .mapToDouble(CategoryExample::getConfidence)
            .average()
            .orElse(0.0);
        
        return averageConfidence;
    }
    
    private double checkConceptualAlignment(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                          String modernEquivalent, 
                                          BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        try {
            // Semantic similarity analysis based on psychological concept mapping
            double alignmentScore = 0.0;
            
            // Get semantic keywords for both concepts
            List<String> buddhistKeywords = extractSemanticKeywords(buddhistFactor.getName(), buddhistFactor.getDescription());
            List<String> modernKeywords = extractSemanticKeywords(modernEquivalent, getModernDescription(modernEquivalent, framework));
            
            // Calculate keyword overlap
            double keywordOverlap = calculateKeywordOverlap(buddhistKeywords, modernKeywords);
            alignmentScore += keywordOverlap * 0.4;
            
            // Check conceptual domain alignment
            double domainAlignment = checkDomainAlignment(buddhistFactor.getCategory(), framework);
            alignmentScore += domainAlignment * 0.3;
            
            // Check functional similarity
            double functionalSimilarity = checkFunctionalSimilarity(buddhistFactor, modernEquivalent, framework);
            alignmentScore += functionalSimilarity * 0.3;
            
            return Math.min(1.0, Math.max(0.0, alignmentScore));
            
        } catch (Exception e) {
            logger.warn("Error calculating conceptual alignment: {}", e.getMessage());
            return 0.5; // Default moderate alignment
        }
    }
    
    private double checkCulturalAppropriateness(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                              String modernEquivalent, 
                                              BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        try {
            double appropriatenessScore = 0.0;
            
            // Check for cultural sensitivity issues
            double culturalSensitivity = checkCulturalSensitivity(buddhistFactor, modernEquivalent);
            appropriatenessScore += culturalSensitivity * 0.4;
            
            // Check for potential misinterpretation risks
            double misinterpretationRisk = checkMisinterpretationRisk(buddhistFactor, modernEquivalent, framework);
            appropriatenessScore += (1.0 - misinterpretationRisk) * 0.3;
            
            // Check for therapeutic appropriateness
            double therapeuticAppropriateness = checkTherapeuticAppropriateness(buddhistFactor, modernEquivalent, framework);
            appropriatenessScore += therapeuticAppropriateness * 0.3;
            
            return Math.min(1.0, Math.max(0.0, appropriatenessScore));
            
        } catch (Exception e) {
            logger.warn("Error calculating cultural appropriateness: {}", e.getMessage());
            return 0.6; // Default moderate appropriateness
        }
    }
    
    private double checkClinicalValidity(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                       String modernEquivalent, 
                                       BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        try {
            double validityScore = 0.0;
            
            // Check empirical evidence support
            double empiricalSupport = checkEmpiricalSupport(buddhistFactor, modernEquivalent, framework);
            validityScore += empiricalSupport * 0.4;
            
            // Check clinical utility
            double clinicalUtility = checkClinicalUtility(buddhistFactor, modernEquivalent, framework);
            validityScore += clinicalUtility * 0.3;
            
            // Check diagnostic reliability
            double diagnosticReliability = checkDiagnosticReliability(buddhistFactor, modernEquivalent, framework);
            validityScore += diagnosticReliability * 0.3;
            
            return Math.min(1.0, Math.max(0.0, validityScore));
            
        } catch (Exception e) {
            logger.warn("Error calculating clinical validity: {}", e.getMessage());
            return 0.7; // Default moderate validity
        }
    }
    
    private double checkTherapeuticUtility(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                         String modernEquivalent, 
                                         BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        try {
            double utilityScore = 0.6; // Base utility level
            
            // Check framework therapeutic utility
            Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double> frameworkUtility = Map.of(
                BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.9,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.9,
                BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.7,
                BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.6
            );
            
            utilityScore = frameworkUtility.getOrDefault(framework, 0.6);
            
            // Check factor category therapeutic relevance
            Map<BuddhistPsychologyMapper.FactorCategory, Double> categoryUtility = Map.of(
                BuddhistPsychologyMapper.FactorCategory.EMOTIONAL, 0.9,
                BuddhistPsychologyMapper.FactorCategory.COGNITIVE, 0.8,
                BuddhistPsychologyMapper.FactorCategory.VOLITIONAL, 0.7,
                BuddhistPsychologyMapper.FactorCategory.CONSCIOUSNESS, 0.6,
                BuddhistPsychologyMapper.FactorCategory.MENTAL_FACTORS, 0.8,
                BuddhistPsychologyMapper.FactorCategory.UNIVERSAL, 0.7
            );
            
            double categoryUtilityLevel = categoryUtility.getOrDefault(buddhistFactor.getCategory(), 0.6);
            utilityScore = (utilityScore + categoryUtilityLevel) / 2.0;
            
            // Check for specific therapeutic applications
            if (isTherapeuticallyApplicable(buddhistFactor, modernEquivalent, framework)) {
                utilityScore += 0.1;
            }
            
            return Math.min(1.0, utilityScore);
            
        } catch (Exception e) {
            logger.warn("Error calculating therapeutic utility: {}", e.getMessage());
            return 0.7; // Default moderate utility
        }
    }
    
    private List<String> identifyValidationIssues(ValidationResult result) {
        List<String> issues = new ArrayList<>();
        
        if (result.getConceptualAlignment() < LOW_CONFIDENCE_THRESHOLD) {
            issues.add("Low conceptual alignment between Buddhist and modern concepts");
        }
        
        if (result.getCulturalAppropriateness() < LOW_CONFIDENCE_THRESHOLD) {
            issues.add("Cultural appropriateness concerns in cross-cultural application");
        }
        
        if (result.getClinicalValidity() < LOW_CONFIDENCE_THRESHOLD) {
            issues.add("Clinical validity concerns for therapeutic application");
        }
        
        if (result.getTherapeuticUtility() < LOW_CONFIDENCE_THRESHOLD) {
            issues.add("Limited therapeutic utility for intervention planning");
        }
        
        return issues;
    }
    
    private List<String> generateValidationRecommendations(ValidationResult result) {
        List<String> recommendations = new ArrayList<>();
        
        if (result.getConceptualAlignment() < MEDIUM_CONFIDENCE_THRESHOLD) {
            recommendations.add("Consider alternative modern psychology concepts");
            recommendations.add("Review conceptual definitions for better alignment");
        }
        
        if (result.getCulturalAppropriateness() < MEDIUM_CONFIDENCE_THRESHOLD) {
            recommendations.add("Adapt mapping for specific cultural contexts");
            recommendations.add("Consider cultural variations in psychological concepts");
        }
        
        if (result.getClinicalValidity() < MEDIUM_CONFIDENCE_THRESHOLD) {
            recommendations.add("Validate mapping with clinical populations");
            recommendations.add("Review evidence base for clinical application");
        }
        
        if (result.getTherapeuticUtility() < MEDIUM_CONFIDENCE_THRESHOLD) {
            recommendations.add("Develop specific therapeutic interventions");
            recommendations.add("Test utility in clinical practice");
        }
        
        return recommendations;
    }
    
    private List<CrossFrameworkInsight> generateCrossFrameworkInsights(
            Map<BuddhistPsychologyMapper.ModernPsychologyFramework, BuddhistPsychologyMapper.ModernPsychologyMapping> frameworkMappings) {
        
        List<CrossFrameworkInsight> insights = new ArrayList<>();
        
        // Generate insights about consistency across frameworks
        CrossFrameworkInsight consistencyInsight = new CrossFrameworkInsight();
        consistencyInsight.setType("CONSISTENCY");
        consistencyInsight.setTitle("Cross-Framework Consistency");
        consistencyInsight.setDescription("Analysis of consistency across different psychological frameworks");
        consistencyInsight.setConfidence(0.8);
        insights.add(consistencyInsight);
        
        // Generate insights about dominant patterns
        CrossFrameworkInsight patternInsight = new CrossFrameworkInsight();
        patternInsight.setType("PATTERNS");
        patternInsight.setTitle("Dominant Psychological Patterns");
        patternInsight.setDescription("Identification of dominant patterns across frameworks");
        patternInsight.setConfidence(0.7);
        insights.add(patternInsight);
        
        return insights;
    }
    
    private double calculateMappingConsistency(
            Map<BuddhistPsychologyMapper.ModernPsychologyFramework, BuddhistPsychologyMapper.ModernPsychologyMapping> frameworkMappings) {
        
        // Calculate consistency across frameworks
        double consistencyScore = 0.0;
        int frameworkCount = 0;
        
        for (BuddhistPsychologyMapper.ModernPsychologyFramework framework : BuddhistPsychologyMapper.ModernPsychologyFramework.values()) {
            try {
                ModernPsychologyBridge.EquivalentMapping mapping = findEquivalent("test", framework); // Use a test factor name
                if (mapping != null && mapping.getEquivalent() != null && !mapping.getEquivalent().isEmpty()) {
                    // Check if this framework's mapping is consistent with others
                    double frameworkConsistency = calculateFrameworkConsistency(BuddhistPsychologyMapper.FactorCategory.UNIVERSAL, mapping.getEquivalent(), framework);
                    consistencyScore += frameworkConsistency;
                    frameworkCount++;
                }
            } catch (Exception e) {
                logger.debug("Error calculating consistency for framework {}: {}", framework, e.getMessage());
            }
        }
        
        return frameworkCount > 0 ? consistencyScore / frameworkCount : 0.5;
    }
    
    private List<TherapeuticRecommendation> generateTherapeuticRecommendations(
            Map<BuddhistPsychologyMapper.ModernPsychologyFramework, BuddhistPsychologyMapper.ModernPsychologyMapping> frameworkMappings) {
        
        List<TherapeuticRecommendation> recommendations = new ArrayList<>();
        
        // Generate recommendations based on framework mappings
        TherapeuticRecommendation cbtRecommendation = new TherapeuticRecommendation();
        cbtRecommendation.setFramework(BuddhistPsychologyMapper.ModernPsychologyFramework.CBT);
        cbtRecommendation.setPriority("HIGH");
        cbtRecommendation.setDescription("Cognitive restructuring and behavioral activation");
        cbtRecommendation.setConfidence(0.8);
        recommendations.add(cbtRecommendation);
        
        return recommendations;
    }
    
    // Error handling methods
    private EquivalentMapping createErrorMapping(String errorMessage) {
        EquivalentMapping mapping = new EquivalentMapping();
        mapping.setBuddhistFactor("ERROR");
        mapping.setTargetFramework(BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE);
        mapping.setEquivalent("Error: " + errorMessage);
        mapping.setConfidence(0.0);
        mapping.setTimestamp(new Date());
        return mapping;
    }
    
    private CategoryTranslation createErrorCategoryTranslation(BuddhistPsychologyMapper.FactorCategory category, 
                                                             BuddhistPsychologyMapper.ModernPsychologyFramework framework, 
                                                             String errorMessage) {
        CategoryTranslation translation = new CategoryTranslation();
        translation.setBuddhistCategory(category);
        translation.setTargetFramework(framework);
        translation.setModernCategory("Error: " + errorMessage);
        translation.setConfidence(0.0);
        translation.setTimestamp(new Date());
        return translation;
    }
    
    private ValidationResult createErrorValidationResult(String buddhistFactor, String modernEquivalent, 
                                                       BuddhistPsychologyMapper.ModernPsychologyFramework framework, 
                                                       String errorMessage) {
        ValidationResult result = new ValidationResult();
        result.setBuddhistFactor(buddhistFactor);
        result.setModernEquivalent(modernEquivalent);
        result.setFramework(framework);
        result.setOverallConfidence(0.0);
        result.setIssues(Arrays.asList("Error: " + errorMessage));
        result.setTimestamp(new Date());
        return result;
    }
    
    private ComprehensiveMapping createErrorComprehensiveMapping(List<Double> buddhistVector, String errorMessage) {
        ComprehensiveMapping mapping = new ComprehensiveMapping();
        mapping.setBuddhistVector(buddhistVector);
        mapping.setConsistency(0.0);
        mapping.setCrossFrameworkInsights(Arrays.asList(
            new CrossFrameworkInsight("ERROR", "Error", "Error: " + errorMessage, 0.0)
        ));
        mapping.setTimestamp(new Date());
        return mapping;
    }
    
    // ==================== INNER CLASSES ====================
    
    public static class EquivalentMapping {
        private String buddhistFactor;
        private String buddhistDescription;
        private BuddhistPsychologyMapper.ModernPsychologyFramework targetFramework;
        private String equivalent;
        private double confidence;
        private List<String> therapeuticImplications;
        private List<String> culturalConsiderations;
        private ValidationResult validationStatus;
        private Date timestamp;
        
        // Getters and setters
        public String getBuddhistFactor() { return buddhistFactor; }
        public void setBuddhistFactor(String buddhistFactor) { this.buddhistFactor = buddhistFactor; }
        
        public String getBuddhistDescription() { return buddhistDescription; }
        public void setBuddhistDescription(String buddhistDescription) { this.buddhistDescription = buddhistDescription; }
        
        public BuddhistPsychologyMapper.ModernPsychologyFramework getTargetFramework() { return targetFramework; }
        public void setTargetFramework(BuddhistPsychologyMapper.ModernPsychologyFramework targetFramework) { this.targetFramework = targetFramework; }
        
        public String getEquivalent() { return equivalent; }
        public void setEquivalent(String equivalent) { this.equivalent = equivalent; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public List<String> getTherapeuticImplications() { return therapeuticImplications; }
        public void setTherapeuticImplications(List<String> therapeuticImplications) { this.therapeuticImplications = therapeuticImplications; }
        
        public List<String> getCulturalConsiderations() { return culturalConsiderations; }
        public void setCulturalConsiderations(List<String> culturalConsiderations) { this.culturalConsiderations = culturalConsiderations; }
        
        public ValidationResult getValidationStatus() { return validationStatus; }
        public void setValidationStatus(ValidationResult validationStatus) { this.validationStatus = validationStatus; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class CategoryTranslation {
        private BuddhistPsychologyMapper.FactorCategory buddhistCategory;
        private BuddhistPsychologyMapper.ModernPsychologyFramework targetFramework;
        private String modernCategory;
        private String description;
        private List<CategoryExample> examples;
        private List<String> therapeuticApproaches;
        private double confidence;
        private Date timestamp;
        
        // Getters and setters
        public BuddhistPsychologyMapper.FactorCategory getBuddhistCategory() { return buddhistCategory; }
        public void setBuddhistCategory(BuddhistPsychologyMapper.FactorCategory buddhistCategory) { this.buddhistCategory = buddhistCategory; }
        
        public BuddhistPsychologyMapper.ModernPsychologyFramework getTargetFramework() { return targetFramework; }
        public void setTargetFramework(BuddhistPsychologyMapper.ModernPsychologyFramework targetFramework) { this.targetFramework = targetFramework; }
        
        public String getModernCategory() { return modernCategory; }
        public void setModernCategory(String modernCategory) { this.modernCategory = modernCategory; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<CategoryExample> getExamples() { return examples; }
        public void setExamples(List<CategoryExample> examples) { this.examples = examples; }
        
        public List<String> getTherapeuticApproaches() { return therapeuticApproaches; }
        public void setTherapeuticApproaches(List<String> therapeuticApproaches) { this.therapeuticApproaches = therapeuticApproaches; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class CategoryExample {
        private String buddhistFactor;
        private String modernEquivalent;
        private double confidence;
        
        // Getters and setters
        public String getBuddhistFactor() { return buddhistFactor; }
        public void setBuddhistFactor(String buddhistFactor) { this.buddhistFactor = buddhistFactor; }
        
        public String getModernEquivalent() { return modernEquivalent; }
        public void setModernEquivalent(String modernEquivalent) { this.modernEquivalent = modernEquivalent; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    public static class ValidationResult {
        private String buddhistFactor;
        private String modernEquivalent;
        private BuddhistPsychologyMapper.ModernPsychologyFramework framework;
        private double conceptualAlignment;
        private double culturalAppropriateness;
        private double clinicalValidity;
        private double therapeuticUtility;
        private double overallConfidence;
        private List<String> issues;
        private List<String> recommendations;
        private Date timestamp;
        
        // Getters and setters
        public String getBuddhistFactor() { return buddhistFactor; }
        public void setBuddhistFactor(String buddhistFactor) { this.buddhistFactor = buddhistFactor; }
        
        public String getModernEquivalent() { return modernEquivalent; }
        public void setModernEquivalent(String modernEquivalent) { this.modernEquivalent = modernEquivalent; }
        
        public BuddhistPsychologyMapper.ModernPsychologyFramework getFramework() { return framework; }
        public void setFramework(BuddhistPsychologyMapper.ModernPsychologyFramework framework) { this.framework = framework; }
        
        public double getConceptualAlignment() { return conceptualAlignment; }
        public void setConceptualAlignment(double conceptualAlignment) { this.conceptualAlignment = conceptualAlignment; }
        
        public double getCulturalAppropriateness() { return culturalAppropriateness; }
        public void setCulturalAppropriateness(double culturalAppropriateness) { this.culturalAppropriateness = culturalAppropriateness; }
        
        public double getClinicalValidity() { return clinicalValidity; }
        public void setClinicalValidity(double clinicalValidity) { this.clinicalValidity = clinicalValidity; }
        
        public double getTherapeuticUtility() { return therapeuticUtility; }
        public void setTherapeuticUtility(double therapeuticUtility) { this.therapeuticUtility = therapeuticUtility; }
        
        public double getOverallConfidence() { return overallConfidence; }
        public void setOverallConfidence(double overallConfidence) { this.overallConfidence = overallConfidence; }
        
        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class ComprehensiveMapping {
        private List<Double> buddhistVector;
        private Map<BuddhistPsychologyMapper.ModernPsychologyFramework, BuddhistPsychologyMapper.ModernPsychologyMapping> frameworkMappings;
        private List<CrossFrameworkInsight> crossFrameworkInsights;
        private double consistency;
        private List<TherapeuticRecommendation> therapeuticRecommendations;
        private Date timestamp;
        
        // Getters and setters
        public List<Double> getBuddhistVector() { return buddhistVector; }
        public void setBuddhistVector(List<Double> buddhistVector) { this.buddhistVector = buddhistVector; }
        
        public Map<BuddhistPsychologyMapper.ModernPsychologyFramework, BuddhistPsychologyMapper.ModernPsychologyMapping> getFrameworkMappings() { return frameworkMappings; }
        public void setFrameworkMappings(Map<BuddhistPsychologyMapper.ModernPsychologyFramework, BuddhistPsychologyMapper.ModernPsychologyMapping> frameworkMappings) { this.frameworkMappings = frameworkMappings; }
        
        public List<CrossFrameworkInsight> getCrossFrameworkInsights() { return crossFrameworkInsights; }
        public void setCrossFrameworkInsights(List<CrossFrameworkInsight> crossFrameworkInsights) { this.crossFrameworkInsights = crossFrameworkInsights; }
        
        public double getConsistency() { return consistency; }
        public void setConsistency(double consistency) { this.consistency = consistency; }
        
        public List<TherapeuticRecommendation> getTherapeuticRecommendations() { return therapeuticRecommendations; }
        public void setTherapeuticRecommendations(List<TherapeuticRecommendation> therapeuticRecommendations) { this.therapeuticRecommendations = therapeuticRecommendations; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class CrossFrameworkInsight {
        private String type;
        private String title;
        private String description;
        private double confidence;
        
        public CrossFrameworkInsight() {}
        
        public CrossFrameworkInsight(String type, String title, String description, double confidence) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.confidence = confidence;
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    public static class TherapeuticRecommendation {
        private BuddhistPsychologyMapper.ModernPsychologyFramework framework;
        private String priority;
        private String description;
        private double confidence;
        
        // Getters and setters
        public BuddhistPsychologyMapper.ModernPsychologyFramework getFramework() { return framework; }
        public void setFramework(BuddhistPsychologyMapper.ModernPsychologyFramework framework) { this.framework = framework; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
    
    // ==================== HELPER METHODS FOR REAL IMPLEMENTATIONS ====================
    
    private List<String> extractSemanticKeywords(String name, String description) {
        List<String> keywords = new ArrayList<>();
        
        // Extract keywords from name
        String[] nameWords = name.toLowerCase().split("[\\s_-]+");
        for (String word : nameWords) {
            if (word.length() > 2 && !isStopWord(word)) {
                keywords.add(word);
            }
        }
        
        // Extract keywords from description
        if (description != null && !description.isEmpty()) {
            String[] descWords = description.toLowerCase().split("[\\s,.-]+");
            for (String word : descWords) {
                if (word.length() > 3 && !isStopWord(word) && !keywords.contains(word)) {
                    keywords.add(word);
                }
            }
        }
        
        return keywords;
    }
    
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "must", "can", "this", "that", "these", "those", "a", "an");
        return stopWords.contains(word.toLowerCase());
    }
    
    private String getModernDescription(String modernEquivalent, BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Return appropriate description based on framework
        switch (framework) {
            case BIG_FIVE:
                return getBigFiveDescription(modernEquivalent);
            case CBT:
                return getCBTDescription(modernEquivalent);
            case EMOTIONAL_INTELLIGENCE:
                return getEIDescription(modernEquivalent);
            case POSITIVE_PSYCHOLOGY:
                return getPositivePsychologyDescription(modernEquivalent);
            case CLINICAL_PSYCHOLOGY:
                return getClinicalPsychologyDescription(modernEquivalent);
            default:
                return "Modern psychological concept";
        }
    }
    
    private String getBigFiveDescription(String concept) {
        Map<String, String> descriptions = Map.of(
            "Openness", "Tendency to be open to new experiences and ideas",
            "Conscientiousness", "Tendency to be organized and dependable",
            "Extraversion", "Tendency to seek stimulation in company of others",
            "Agreeableness", "Tendency to be compassionate and cooperative",
            "Neuroticism", "Tendency to experience negative emotions"
        );
        return descriptions.getOrDefault(concept, "Big Five personality trait");
    }
    
    private String getCBTDescription(String concept) {
        Map<String, String> descriptions = Map.of(
            "Cognitive Distortions", "Irrational thought patterns that lead to negative emotions",
            "Automatic Thoughts", "Spontaneous thoughts that influence emotions and behavior",
            "Core Beliefs", "Fundamental assumptions about self, others, and the world",
            "Behavioral Activation", "Therapeutic technique to increase engagement in positive activities",
            "Mindfulness", "Present-moment awareness without judgment"
        );
        return descriptions.getOrDefault(concept, "CBT concept");
    }
    
    private String getEIDescription(String concept) {
        Map<String, String> descriptions = Map.of(
            "Self-Awareness", "Ability to recognize and understand one's own emotions",
            "Self-Regulation", "Ability to manage and control one's emotions",
            "Motivation", "Drive to achieve goals and maintain optimism",
            "Empathy", "Ability to understand and share others' emotions",
            "Social Skills", "Ability to manage relationships and communicate effectively"
        );
        return descriptions.getOrDefault(concept, "Emotional intelligence component");
    }
    
    private String getPositivePsychologyDescription(String concept) {
        Map<String, String> descriptions = Map.of(
            "Flow", "State of complete absorption in an activity",
            "Gratitude", "Appreciation for positive aspects of life",
            "Resilience", "Ability to bounce back from adversity",
            "Optimism", "Tendency to expect positive outcomes",
            "Meaning", "Sense of purpose and significance in life"
        );
        return descriptions.getOrDefault(concept, "Positive psychology construct");
    }
    
    private String getClinicalPsychologyDescription(String concept) {
        Map<String, String> descriptions = Map.of(
            "Anxiety", "Excessive worry and fear about future events",
            "Depression", "Persistent sadness and loss of interest",
            "Trauma", "Psychological response to distressing events",
            "Coping", "Strategies for managing stress and adversity",
            "Therapeutic Alliance", "Collaborative relationship between therapist and client"
        );
        return descriptions.getOrDefault(concept, "Clinical psychology concept");
    }
    
    private double calculateKeywordOverlap(List<String> keywords1, List<String> keywords2) {
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> set1 = new HashSet<>(keywords1);
        Set<String> set2 = new HashSet<>(keywords2);
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private double checkDomainAlignment(BuddhistPsychologyMapper.FactorCategory category, 
                                      BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Domain alignment scores based on psychological research
        Map<BuddhistPsychologyMapper.FactorCategory, Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double>> alignmentMatrix = Map.of(
            BuddhistPsychologyMapper.FactorCategory.COGNITIVE, Map.of(
                BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.9,
                BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.6,
                BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.7,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.8
            ),
            BuddhistPsychologyMapper.FactorCategory.EMOTIONAL, Map.of(
                BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.9,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.95,
                BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.9
            ),
            BuddhistPsychologyMapper.FactorCategory.VOLITIONAL, Map.of(
                BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.7,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.6,
                BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.5,
                BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.6
            ),
            BuddhistPsychologyMapper.FactorCategory.CONSCIOUSNESS, Map.of(
                BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.5,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.7,
                BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.6,
                BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.7
            ),
            BuddhistPsychologyMapper.FactorCategory.MENTAL_FACTORS, Map.of(
                BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.7,
                BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.9
            ),
            BuddhistPsychologyMapper.FactorCategory.UNIVERSAL, Map.of(
                BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.9,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.8,
                BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.9,
                BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.8
            )
        );
        
        return alignmentMatrix.getOrDefault(category, Map.of()).getOrDefault(framework, 0.5);
    }
    
    private double checkFunctionalSimilarity(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                           String modernEquivalent, 
                                           BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check if the factors serve similar psychological functions
        String buddhistFunction = determineBuddhistFunction(buddhistFactor);
        String modernFunction = determineModernFunction(modernEquivalent, framework);
        
        return calculateFunctionalSimilarity(buddhistFunction, modernFunction);
    }
    
    private String determineBuddhistFunction(BuddhistPsychologyMapper.FactorDescription factor) {
        // Map Buddhist factors to their primary psychological functions
        Map<String, String> functionMap = new HashMap<>();
        functionMap.put("attention", "focus");
        functionMap.put("concentration", "focus");
        functionMap.put("mindfulness", "awareness");
        functionMap.put("compassion", "empathy");
        functionMap.put("loving-kindness", "empathy");
        functionMap.put("equanimity", "regulation");
        functionMap.put("patience", "regulation");
        functionMap.put("wisdom", "cognition");
        functionMap.put("understanding", "cognition");
        functionMap.put("faith", "motivation");
        functionMap.put("energy", "motivation");
        functionMap.put("joy", "emotion");
        functionMap.put("happiness", "emotion");
        functionMap.put("sadness", "emotion");
        functionMap.put("anger", "emotion");
        functionMap.put("fear", "emotion");
        
        String factorName = factor.getName().toLowerCase();
        for (Map.Entry<String, String> entry : functionMap.entrySet()) {
            if (factorName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "general";
    }
    
    private String determineModernFunction(String modernEquivalent, BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Map modern concepts to their primary psychological functions
        Map<String, String> functionMap = new HashMap<>();
        functionMap.put("attention", "focus");
        functionMap.put("focus", "focus");
        functionMap.put("awareness", "awareness");
        functionMap.put("empathy", "empathy");
        functionMap.put("compassion", "empathy");
        functionMap.put("regulation", "regulation");
        functionMap.put("control", "regulation");
        functionMap.put("cognition", "cognition");
        functionMap.put("thinking", "cognition");
        functionMap.put("motivation", "motivation");
        functionMap.put("drive", "motivation");
        functionMap.put("emotion", "emotion");
        functionMap.put("feeling", "emotion");
        functionMap.put("mood", "emotion");
        
        String concept = modernEquivalent.toLowerCase();
        for (Map.Entry<String, String> entry : functionMap.entrySet()) {
            if (concept.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "general";
    }
    
    private double calculateFunctionalSimilarity(String function1, String function2) {
        if (function1.equals(function2)) {
            return 1.0;
        }
        
        // Check for related functions
        Map<String, Set<String>> relatedFunctions = Map.of(
            "focus", Set.of("awareness", "cognition"),
            "awareness", Set.of("focus", "cognition"),
            "empathy", Set.of("emotion", "regulation"),
            "regulation", Set.of("emotion", "empathy"),
            "cognition", Set.of("focus", "awareness"),
            "motivation", Set.of("emotion", "regulation"),
            "emotion", Set.of("empathy", "regulation", "motivation")
        );
        
        Set<String> related = relatedFunctions.getOrDefault(function1, Set.of());
        return related.contains(function2) ? 0.7 : 0.3;
    }
    
    private double checkCulturalSensitivity(BuddhistPsychologyMapper.FactorDescription buddhistFactor, String modernEquivalent) {
        // Check for potential cultural sensitivity issues
        double sensitivityScore = 1.0;
        
        // Check for religious terminology that might be inappropriate
        Set<String> religiousTerms = Set.of("buddha", "dharma", "sangha", "nirvana", "karma", "samsara");
        String factorName = buddhistFactor.getName().toLowerCase();
        
        for (String term : religiousTerms) {
            if (factorName.contains(term)) {
                sensitivityScore -= 0.2; // Reduce score for religious terminology
            }
        }
        
        // Check for concepts that might be culturally specific
        Set<String> culturallySpecific = Set.of("enlightenment", "awakening", "liberation");
        for (String term : culturallySpecific) {
            if (factorName.contains(term)) {
                sensitivityScore -= 0.1;
            }
        }
        
        return Math.max(0.0, sensitivityScore);
    }
    
    private double checkMisinterpretationRisk(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                            String modernEquivalent, 
                                            BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check for potential misinterpretation risks
        double riskScore = 0.0;
        
        // Check for conceptual complexity that might lead to misinterpretation
        if (buddhistFactor.getDescription().length() > 100) {
            riskScore += 0.2; // Longer descriptions might be more complex
        }
        
        // Check for abstract concepts
        Set<String> abstractConcepts = Set.of("consciousness", "awareness", "mindfulness", "wisdom", "understanding");
        String factorName = buddhistFactor.getName().toLowerCase();
        for (String concept : abstractConcepts) {
            if (factorName.contains(concept)) {
                riskScore += 0.3; // Abstract concepts have higher misinterpretation risk
            }
        }
        
        // Check for framework-specific risks
        if (framework == BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY) {
            riskScore += 0.1; // Clinical frameworks might be more prone to misinterpretation
        }
        
        return Math.min(1.0, riskScore);
    }
    
    private double checkTherapeuticAppropriateness(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                                 String modernEquivalent, 
                                                 BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check therapeutic appropriateness of the mapping
        double appropriatenessScore = 0.8; // Start with high appropriateness
        
        // Check for therapeutic utility
        if (framework == BuddhistPsychologyMapper.ModernPsychologyFramework.CBT || 
            framework == BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY) {
            appropriatenessScore += 0.1; // Clinical frameworks are more therapeutically appropriate
        }
        
        // Check for factor category appropriateness
        if (buddhistFactor.getCategory() == BuddhistPsychologyMapper.FactorCategory.EMOTIONAL ||
            buddhistFactor.getCategory() == BuddhistPsychologyMapper.FactorCategory.COGNITIVE) {
            appropriatenessScore += 0.1; // These categories are more therapeutically relevant
        }
        
        return Math.min(1.0, appropriatenessScore);
    }
    
    private double checkEmpiricalSupport(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                       String modernEquivalent, 
                                       BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check empirical evidence support for the mapping
        double supportScore = 0.5; // Base support level
        
        // Framework-specific empirical support levels
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double> frameworkSupport = Map.of(
            BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.9,
            BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.8,
            BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.7,
            BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.6,
            BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.8
        );
        
        supportScore = frameworkSupport.getOrDefault(framework, 0.5);
        
        // Factor category support
        Map<BuddhistPsychologyMapper.FactorCategory, Double> categorySupport = Map.of(
            BuddhistPsychologyMapper.FactorCategory.UNIVERSAL, 0.9,
            BuddhistPsychologyMapper.FactorCategory.EMOTIONAL, 0.8,
            BuddhistPsychologyMapper.FactorCategory.COGNITIVE, 0.8,
            BuddhistPsychologyMapper.FactorCategory.VOLITIONAL, 0.6,
            BuddhistPsychologyMapper.FactorCategory.CONSCIOUSNESS, 0.5,
            BuddhistPsychologyMapper.FactorCategory.MENTAL_FACTORS, 0.7
        );
        
        double categorySupportLevel = categorySupport.getOrDefault(buddhistFactor.getCategory(), 0.5);
        supportScore = (supportScore + categorySupportLevel) / 2.0;
        
        return Math.min(1.0, supportScore);
    }
    
    private double checkClinicalUtility(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                      String modernEquivalent, 
                                      BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check clinical utility of the mapping
        double utilityScore = 0.6; // Base utility level
        
        // Framework-specific utility
        if (framework == BuddhistPsychologyMapper.ModernPsychologyFramework.CBT ||
            framework == BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY) {
            utilityScore += 0.2; // Higher utility for clinical frameworks
        }
        
        // Factor category utility
        if (buddhistFactor.getCategory() == BuddhistPsychologyMapper.FactorCategory.EMOTIONAL ||
            buddhistFactor.getCategory() == BuddhistPsychologyMapper.FactorCategory.COGNITIVE) {
            utilityScore += 0.1; // Higher utility for these categories
        }
        
        return Math.min(1.0, utilityScore);
    }
    
    private double checkDiagnosticReliability(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                            String modernEquivalent, 
                                            BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check diagnostic reliability of the mapping
        double reliabilityScore = 0.7; // Base reliability level
        
        // Framework-specific reliability
        Map<BuddhistPsychologyMapper.ModernPsychologyFramework, Double> frameworkReliability = Map.of(
            BuddhistPsychologyMapper.ModernPsychologyFramework.BIG_FIVE, 0.8,
            BuddhistPsychologyMapper.ModernPsychologyFramework.CBT, 0.7,
            BuddhistPsychologyMapper.ModernPsychologyFramework.EMOTIONAL_INTELLIGENCE, 0.6,
            BuddhistPsychologyMapper.ModernPsychologyFramework.POSITIVE_PSYCHOLOGY, 0.5,
            BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY, 0.8
        );
        
        reliabilityScore = frameworkReliability.getOrDefault(framework, 0.7);
        
        return Math.min(1.0, reliabilityScore);
    }
    
    private boolean isTherapeuticallyApplicable(BuddhistPsychologyMapper.FactorDescription buddhistFactor, 
                                             String modernEquivalent, 
                                             BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check if the mapping has specific therapeutic applications
        String factorName = buddhistFactor.getName().toLowerCase();
        
        // Check for therapeutically relevant concepts
        Set<String> therapeuticConcepts = Set.of(
            "mindfulness", "compassion", "equanimity", "patience", "wisdom", 
            "understanding", "attention", "concentration", "awareness"
        );
        
        for (String concept : therapeuticConcepts) {
            if (factorName.contains(concept)) {
                return true;
            }
        }
        
        // Check for modern therapeutic concepts
        String modernConcept = modernEquivalent.toLowerCase();
        Set<String> modernTherapeuticConcepts = Set.of(
            "mindfulness", "cbt", "therapy", "intervention", "treatment", 
            "coping", "regulation", "awareness", "empathy", "resilience"
        );
        
        for (String concept : modernTherapeuticConcepts) {
            if (modernConcept.contains(concept)) {
                return true;
            }
        }
        
        return false;
    }
    
    private double calculateFrameworkConsistency(BuddhistPsychologyMapper.FactorCategory category, 
                                               String equivalent, 
                                               BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Calculate how consistent this framework's mapping is with others
        double consistencyScore = 0.5; // Base consistency
        
        // Check if the equivalent makes sense for this category
        if (isCategoryAppropriate(category, equivalent, framework)) {
            consistencyScore += 0.3;
        }
        
        // Check for semantic consistency
        if (isSemanticallyConsistent(category, equivalent, framework)) {
            consistencyScore += 0.2;
        }
        
        return Math.min(1.0, consistencyScore);
    }
    
    private boolean isCategoryAppropriate(BuddhistPsychologyMapper.FactorCategory category, 
                                        String equivalent, 
                                        BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check if the equivalent is appropriate for the category
        String equivalentLower = equivalent.toLowerCase();
        
        switch (category) {
            case EMOTIONAL:
                return equivalentLower.contains("emotion") || 
                       equivalentLower.contains("feeling") || 
                       equivalentLower.contains("mood") ||
                       equivalentLower.contains("empathy") ||
                       equivalentLower.contains("compassion");
            case COGNITIVE:
                return equivalentLower.contains("cognition") || 
                       equivalentLower.contains("thinking") || 
                       equivalentLower.contains("belief") ||
                       equivalentLower.contains("attention") ||
                       equivalentLower.contains("awareness");
            case VOLITIONAL:
                return equivalentLower.contains("motivation") || 
                       equivalentLower.contains("drive") || 
                       equivalentLower.contains("will") ||
                       equivalentLower.contains("energy") ||
                       equivalentLower.contains("effort");
            case CONSCIOUSNESS:
                return equivalentLower.contains("consciousness") || 
                       equivalentLower.contains("awareness") || 
                       equivalentLower.contains("mindfulness") ||
                       equivalentLower.contains("attention");
            case MENTAL_FACTORS:
                return equivalentLower.contains("factor") || 
                       equivalentLower.contains("trait") || 
                       equivalentLower.contains("characteristic");
            case UNIVERSAL:
                return true; // Universal factors can map to anything
            default:
                return false;
        }
    }
    
    private boolean isSemanticallyConsistent(BuddhistPsychologyMapper.FactorCategory category, 
                                           String equivalent, 
                                           BuddhistPsychologyMapper.ModernPsychologyFramework framework) {
        // Check if the semantic meaning is consistent
        String equivalentLower = equivalent.toLowerCase();
        
        // Check for positive/negative valence consistency
        boolean isPositive = isPositiveConcept(equivalentLower);
        boolean categorySupportsPositive = categorySupportsPositiveValence(category);
        
        return isPositive == categorySupportsPositive;
    }
    
    private boolean isPositiveConcept(String concept) {
        Set<String> positiveConcepts = Set.of(
            "positive", "good", "beneficial", "healthy", "adaptive", 
            "constructive", "helpful", "supportive", "growth", "development"
        );
        
        for (String positive : positiveConcepts) {
            if (concept.contains(positive)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean categorySupportsPositiveValence(BuddhistPsychologyMapper.FactorCategory category) {
        // Most Buddhist mental factors are considered positive or neutral
        return category != BuddhistPsychologyMapper.FactorCategory.EMOTIONAL || 
               category == BuddhistPsychologyMapper.FactorCategory.UNIVERSAL;
    }
}
