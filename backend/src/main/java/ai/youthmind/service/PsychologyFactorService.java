package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factor analysis and interpretation service for psychological factors
 * 
 * This service provides comprehensive analysis and interpretation of the
 * 43-dimensional Buddhist mental factors system, including post analysis,
 * dominant factor extraction, and therapeutic insights generation.
 */
@Service
public class PsychologyFactorService {
    
    private static final Logger logger = LoggerFactory.getLogger(PsychologyFactorService.class);
    
    @Autowired
    private BuddhistPsychologyMapper buddhistPsychologyMapper;
    
    @Autowired
    private ModernPsychologyBridge modernPsychologyBridge;
    
    @Autowired
    private MentalFactorClassifier mentalFactorClassifier;
    
    // Analysis thresholds
    private static final double DOMINANT_FACTOR_THRESHOLD = 0.7;
    private static final double SIGNIFICANT_FACTOR_THRESHOLD = 0.4;
    private static final double MINIMAL_FACTOR_THRESHOLD = 0.1;
    
    /**
     * Analyze psychological factors in a user post
     * 
     * @param postContent The content of the user post
     * @param psychologicalVector 43-dimensional Buddhist mental factors vector
     * @return Post factor analysis result
     */
    public PostFactorAnalysis analyzePostFactors(String postContent, List<Double> psychologicalVector) {
        try {
            logger.debug("Analyzing psychological factors in post content");
            
            if (postContent == null || postContent.trim().isEmpty()) {
                throw new IllegalArgumentException("Post content cannot be null or empty");
            }
            
            if (psychologicalVector == null || psychologicalVector.size() != 43) {
                throw new IllegalArgumentException("Psychological vector must be exactly 43 dimensions");
            }
            
            PostFactorAnalysis analysis = new PostFactorAnalysis();
            analysis.setPostContent(postContent);
            analysis.setPsychologicalVector(psychologicalVector);
            
            // Classify all factors
            List<MentalFactorClassifier.FactorClassification> factorClassifications = 
                new ArrayList<>();
            for (int i = 0; i < psychologicalVector.size(); i++) {
                MentalFactorClassifier.FactorClassification classification = 
                    mentalFactorClassifier.classifyFactor(i, psychologicalVector.get(i));
                factorClassifications.add(classification);
            }
            analysis.setFactorClassifications(factorClassifications);
            
            // Extract dominant factors
            List<DominantFactor> dominantFactors = extractDominantFactors(factorClassifications);
            analysis.setDominantFactors(dominantFactors);
            
            // Generate factor insights
            List<FactorInsight> factorInsights = generateFactorInsights(factorClassifications, dominantFactors);
            analysis.setFactorInsights(factorInsights);
            
            // Analyze emotional patterns
            EmotionalPatternAnalysis emotionalAnalysis = analyzeEmotionalPatterns(factorClassifications);
            analysis.setEmotionalAnalysis(emotionalAnalysis);
            
            // Generate therapeutic recommendations
            List<TherapeuticRecommendation> recommendations = 
                generateTherapeuticRecommendations(factorClassifications, dominantFactors);
            analysis.setTherapeuticRecommendations(recommendations);
            
            // Calculate overall psychological state
            PsychologicalState psychologicalState = calculatePsychologicalState(factorClassifications);
            analysis.setPsychologicalState(psychologicalState);
            
            analysis.setTimestamp(new Date());
            
            logger.debug("Post factor analysis completed with {} dominant factors", dominantFactors.size());
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error analyzing post factors: {}", e.getMessage(), e);
            return createErrorPostAnalysis(postContent, psychologicalVector, e.getMessage());
        }
    }
    
    /**
     * Extract dominant psychological factors from classifications
     * 
     * @param factorClassifications List of factor classifications
     * @return List of dominant factors
     */
    public List<DominantFactor> extractDominantFactors(List<MentalFactorClassifier.FactorClassification> factorClassifications) {
        try {
            logger.debug("Extracting dominant factors from {} classifications", factorClassifications.size());
            
            List<DominantFactor> dominantFactors = new ArrayList<>();
            
            // Find factors above dominant threshold
            List<MentalFactorClassifier.FactorClassification> highIntensityFactors = 
                factorClassifications.stream()
                    .filter(f -> f.getIntensity() >= DOMINANT_FACTOR_THRESHOLD)
                    .sorted((a, b) -> Double.compare(b.getIntensity(), a.getIntensity()))
                    .collect(Collectors.toList());
            
            for (MentalFactorClassifier.FactorClassification classification : highIntensityFactors) {
                DominantFactor dominantFactor = new DominantFactor();
                dominantFactor.setFactorIndex(classification.getFactorIndex());
                dominantFactor.setFactorName(classification.getFactorName());
                dominantFactor.setIntensity(classification.getIntensity());
                dominantFactor.setCategory(classification.getCategory());
                dominantFactor.setSignificance(classification.getSignificance());
                dominantFactor.setTherapeuticRelevance(classification.getTherapeuticRelevance());
                
            // Map to modern psychology
            ModernPsychologyBridge.EquivalentMapping mapping = modernPsychologyBridge.findEquivalent(
                classification.getFactorName(), BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY);
            dominantFactor.setModernPsychologyMapping(mapping.getEquivalent());
                
                // Calculate therapeutic priority
                String therapeuticPriority = calculateTherapeuticPriority(classification);
                dominantFactor.setTherapeuticPriority(therapeuticPriority);
                
                dominantFactor.setTimestamp(new Date());
                dominantFactors.add(dominantFactor);
            }
            
            logger.debug("Extracted {} dominant factors", dominantFactors.size());
            
            return dominantFactors;
            
        } catch (Exception e) {
            logger.error("Error extracting dominant factors: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Generate comprehensive factor insights
     * 
     * @param factorClassifications List of factor classifications
     * @param dominantFactors List of dominant factors
     * @return List of factor insights
     */
    public List<FactorInsight> generateFactorInsights(
            List<MentalFactorClassifier.FactorClassification> factorClassifications,
            List<DominantFactor> dominantFactors) {
        
        try {
            logger.debug("Generating factor insights for {} classifications and {} dominant factors", 
                        factorClassifications.size(), dominantFactors.size());
            
            List<FactorInsight> insights = new ArrayList<>();
            
            // Generate insights for dominant factors
            for (DominantFactor dominantFactor : dominantFactors) {
                FactorInsight insight = new FactorInsight();
                insight.setFactorName(dominantFactor.getFactorName());
                insight.setInsightType("DOMINANT_FACTOR");
                insight.setDescription(generateDominantFactorInsight(dominantFactor));
                insight.setTherapeuticImplication(generateTherapeuticImplication(dominantFactor));
                insight.setModernPsychologyContext(dominantFactor.getModernPsychologyMapping());
                insight.setConfidence(calculateInsightConfidence(dominantFactor.getIntensity()));
                insight.setTimestamp(new Date());
                insights.add(insight);
            }
            
            // Generate category-based insights
            Map<BuddhistPsychologyMapper.FactorCategory, List<MentalFactorClassifier.FactorClassification>> 
                categoryGroups = factorClassifications.stream()
                    .collect(Collectors.groupingBy(MentalFactorClassifier.FactorClassification::getCategory));
            
            for (Map.Entry<BuddhistPsychologyMapper.FactorCategory, List<MentalFactorClassifier.FactorClassification>> entry : 
                 categoryGroups.entrySet()) {
                
                BuddhistPsychologyMapper.FactorCategory category = entry.getKey();
                List<MentalFactorClassifier.FactorClassification> factors = entry.getValue();
                
                FactorInsight categoryInsight = generateCategoryInsight(category, factors);
                if (categoryInsight != null) {
                    insights.add(categoryInsight);
                }
            }
            
            // Generate overall pattern insights
            FactorInsight patternInsight = generatePatternInsight(factorClassifications);
            if (patternInsight != null) {
                insights.add(patternInsight);
            }
            
            logger.debug("Generated {} factor insights", insights.size());
            
            return insights;
            
        } catch (Exception e) {
            logger.error("Error generating factor insights: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private EmotionalPatternAnalysis analyzeEmotionalPatterns(
            List<MentalFactorClassifier.FactorClassification> factorClassifications) {
        
        EmotionalPatternAnalysis analysis = new EmotionalPatternAnalysis();
        
        // Analyze emotional factors
        List<MentalFactorClassifier.FactorClassification> emotionalFactors = 
            factorClassifications.stream()
                .filter(f -> f.getCategory() == BuddhistPsychologyMapper.FactorCategory.EMOTIONAL)
                .collect(Collectors.toList());
        
        analysis.setEmotionalFactorCount(emotionalFactors.size());
        
        if (!emotionalFactors.isEmpty()) {
            double averageEmotionalIntensity = emotionalFactors.stream()
                .mapToDouble(MentalFactorClassifier.FactorClassification::getIntensity)
                .average().orElse(0.0);
            analysis.setAverageEmotionalIntensity(averageEmotionalIntensity);
            
            double maxEmotionalIntensity = emotionalFactors.stream()
                .mapToDouble(MentalFactorClassifier.FactorClassification::getIntensity)
                .max().orElse(0.0);
            analysis.setMaxEmotionalIntensity(maxEmotionalIntensity);
            
            analysis.setEmotionalState(determineEmotionalState(averageEmotionalIntensity, maxEmotionalIntensity));
        } else {
            analysis.setAverageEmotionalIntensity(0.0);
            analysis.setMaxEmotionalIntensity(0.0);
            analysis.setEmotionalState("NEUTRAL");
        }
        
        analysis.setTimestamp(new Date());
        return analysis;
    }
    
    private String determineEmotionalState(double averageIntensity, double maxIntensity) {
        if (maxIntensity >= DOMINANT_FACTOR_THRESHOLD) {
            return "HIGHLY_EMOTIONAL";
        } else if (averageIntensity >= SIGNIFICANT_FACTOR_THRESHOLD) {
            return "EMOTIONALLY_ACTIVE";
        } else if (averageIntensity >= MINIMAL_FACTOR_THRESHOLD) {
            return "MODERATELY_EMOTIONAL";
        } else {
            return "EMOTIONALLY_NEUTRAL";
        }
    }
    
    private List<TherapeuticRecommendation> generateTherapeuticRecommendations(
            List<MentalFactorClassifier.FactorClassification> factorClassifications,
            List<DominantFactor> dominantFactors) {
        
        List<TherapeuticRecommendation> recommendations = new ArrayList<>();
        
        for (DominantFactor dominantFactor : dominantFactors) {
            TherapeuticRecommendation recommendation = new TherapeuticRecommendation();
            recommendation.setFactorName(dominantFactor.getFactorName());
            recommendation.setCategory(dominantFactor.getCategory());
            recommendation.setPriority(dominantFactor.getTherapeuticPriority());
            recommendation.setInterventionType(determineInterventionType(dominantFactor));
            recommendation.setDescription(generateInterventionDescription(dominantFactor));
            recommendation.setModernPsychologyApproach(dominantFactor.getModernPsychologyMapping());
            recommendation.setTimestamp(new Date());
            recommendations.add(recommendation);
        }
        
        return recommendations;
    }
    
    private String determineInterventionType(DominantFactor dominantFactor) {
        BuddhistPsychologyMapper.FactorCategory category = dominantFactor.getCategory();
        
        switch (category) {
            case COGNITIVE:
                return "COGNITIVE_BEHAVIORAL_INTERVENTION";
            case EMOTIONAL:
                return "EMOTIONAL_REGULATION_INTERVENTION";
            case VOLITIONAL:
                return "MOTIVATIONAL_INTERVENTION";
            case CONSCIOUSNESS:
                return "MINDFULNESS_INTERVENTION";
            case MENTAL_FACTORS:
                return "PSYCHOLOGICAL_SKILLS_TRAINING";
            default:
                return "GENERAL_PSYCHOLOGICAL_INTERVENTION";
        }
    }
    
    private String generateInterventionDescription(DominantFactor dominantFactor) {
        return String.format("Focus on %s factors through %s approach. " +
                           "Target intensity level: %.2f. " +
                           "Therapeutic priority: %s",
                           dominantFactor.getFactorName(),
                           dominantFactor.getModernPsychologyMapping(),
                           dominantFactor.getIntensity(),
                           dominantFactor.getTherapeuticPriority());
    }
    
    private PsychologicalState calculatePsychologicalState(
            List<MentalFactorClassifier.FactorClassification> factorClassifications) {
        
        PsychologicalState state = new PsychologicalState();
        
        // Calculate overall intensity
        double overallIntensity = factorClassifications.stream()
            .mapToDouble(MentalFactorClassifier.FactorClassification::getIntensity)
            .average().orElse(0.0);
        state.setOverallIntensity(overallIntensity);
        
        // Determine psychological state level
        if (overallIntensity >= DOMINANT_FACTOR_THRESHOLD) {
            state.setStateLevel("HIGHLY_ACTIVE");
        } else if (overallIntensity >= SIGNIFICANT_FACTOR_THRESHOLD) {
            state.setStateLevel("ACTIVE");
        } else if (overallIntensity >= MINIMAL_FACTOR_THRESHOLD) {
            state.setStateLevel("MODERATE");
        } else {
            state.setStateLevel("QUIESCENT");
        }
        
        // Calculate category distribution
        Map<BuddhistPsychologyMapper.FactorCategory, Double> categoryDistribution = 
            factorClassifications.stream()
                .collect(Collectors.groupingBy(
                    MentalFactorClassifier.FactorClassification::getCategory,
                    Collectors.averagingDouble(MentalFactorClassifier.FactorClassification::getIntensity)
                ));
        state.setCategoryDistribution(categoryDistribution);
        
        state.setTimestamp(new Date());
        return state;
    }
    
    private String calculateTherapeuticPriority(MentalFactorClassifier.FactorClassification classification) {
        double intensity = classification.getIntensity();
        
        if (intensity >= DOMINANT_FACTOR_THRESHOLD) {
            return "HIGH_PRIORITY";
        } else if (intensity >= SIGNIFICANT_FACTOR_THRESHOLD) {
            return "MEDIUM_PRIORITY";
        } else if (intensity >= MINIMAL_FACTOR_THRESHOLD) {
            return "LOW_PRIORITY";
        } else {
            return "NO_PRIORITY";
        }
    }
    
    private String generateDominantFactorInsight(DominantFactor dominantFactor) {
        return String.format("Factor '%s' is highly dominant (intensity: %.2f) in the %s category. " +
                           "This suggests significant psychological activity in this domain.",
                           dominantFactor.getFactorName(),
                           dominantFactor.getIntensity(),
                           dominantFactor.getCategory().name());
    }
    
    private String generateTherapeuticImplication(DominantFactor dominantFactor) {
        return String.format("High therapeutic relevance for %s factors. " +
                           "Consider %s interventions to address this psychological pattern.",
                           dominantFactor.getFactorName(),
                           dominantFactor.getModernPsychologyMapping());
    }
    
    private double calculateInsightConfidence(double intensity) {
        // Higher intensity = higher confidence
        return Math.min(1.0, intensity);
    }
    
    private FactorInsight generateCategoryInsight(
            BuddhistPsychologyMapper.FactorCategory category,
            List<MentalFactorClassifier.FactorClassification> factors) {
        
        double averageIntensity = factors.stream()
            .mapToDouble(MentalFactorClassifier.FactorClassification::getIntensity)
            .average().orElse(0.0);
        
        if (averageIntensity < MINIMAL_FACTOR_THRESHOLD) {
            return null; // Skip low-intensity categories
        }
        
        FactorInsight insight = new FactorInsight();
        insight.setFactorName(category.name());
        insight.setInsightType("CATEGORY_ANALYSIS");
        insight.setDescription(String.format("%s category shows %s activity (avg intensity: %.2f)",
                                           category.name(),
                                           determineActivityLevel(averageIntensity),
                                           averageIntensity));
        insight.setTherapeuticImplication(generateCategoryTherapeuticImplication(category, averageIntensity));
        ModernPsychologyBridge.CategoryTranslation translation = modernPsychologyBridge.translateCategory(category, BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY);
        insight.setModernPsychologyContext(translation.getModernCategory());
        insight.setConfidence(calculateInsightConfidence(averageIntensity));
        insight.setTimestamp(new Date());
        
        return insight;
    }
    
    private String determineActivityLevel(double averageIntensity) {
        if (averageIntensity >= DOMINANT_FACTOR_THRESHOLD) {
            return "high";
        } else if (averageIntensity >= SIGNIFICANT_FACTOR_THRESHOLD) {
            return "moderate";
        } else {
            return "low";
        }
    }
    
    private String generateCategoryTherapeuticImplication(
            BuddhistPsychologyMapper.FactorCategory category, double averageIntensity) {
        
        ModernPsychologyBridge.CategoryTranslation translation = modernPsychologyBridge.translateCategory(category, BuddhistPsychologyMapper.ModernPsychologyFramework.CLINICAL_PSYCHOLOGY);
        String interventionType = translation.getModernCategory();
        
        if (averageIntensity >= DOMINANT_FACTOR_THRESHOLD) {
            return String.format("High therapeutic focus needed on %s factors. " +
                               "Consider intensive %s interventions.",
                               category.name(), interventionType);
        } else if (averageIntensity >= SIGNIFICANT_FACTOR_THRESHOLD) {
            return String.format("Moderate therapeutic attention to %s factors. " +
                               "Consider %s interventions.",
                               category.name(), interventionType);
        } else {
            return String.format("Low therapeutic priority for %s factors. " +
                               "Monitor for changes.",
                               category.name());
        }
    }
    
    private FactorInsight generatePatternInsight(
            List<MentalFactorClassifier.FactorClassification> factorClassifications) {
        
        // Analyze overall pattern
        long highIntensityCount = factorClassifications.stream()
            .filter(f -> f.getIntensity() >= DOMINANT_FACTOR_THRESHOLD)
            .count();
        
        long significantCount = factorClassifications.stream()
            .filter(f -> f.getIntensity() >= SIGNIFICANT_FACTOR_THRESHOLD)
            .count();
        
        FactorInsight insight = new FactorInsight();
        insight.setFactorName("OVERALL_PATTERN");
        insight.setInsightType("PATTERN_ANALYSIS");
        insight.setDescription(String.format("Overall psychological pattern: %d dominant factors, %d significant factors",
                                           highIntensityCount, significantCount));
        insight.setTherapeuticImplication(generatePatternTherapeuticImplication(highIntensityCount, significantCount));
        insight.setModernPsychologyContext("Comprehensive psychological assessment");
        insight.setConfidence(0.8); // High confidence for pattern analysis
        insight.setTimestamp(new Date());
        
        return insight;
    }
    
    private String generatePatternTherapeuticImplication(long highIntensityCount, long significantCount) {
        if (highIntensityCount >= 3) {
            return "Multiple dominant factors detected. Consider comprehensive therapeutic approach " +
                   "addressing multiple psychological domains simultaneously.";
        } else if (highIntensityCount >= 1) {
            return "Single dominant factor identified. Focus therapeutic interventions on this " +
                   "primary psychological pattern.";
        } else if (significantCount >= 3) {
            return "Multiple significant factors present. Consider balanced therapeutic approach " +
                   "addressing several psychological domains.";
        } else {
            return "Relatively balanced psychological profile. Focus on maintaining psychological " +
                   "well-being and preventing escalation of any single factor.";
        }
    }
    
    private PostFactorAnalysis createErrorPostAnalysis(String postContent, List<Double> psychologicalVector, String errorMessage) {
        PostFactorAnalysis analysis = new PostFactorAnalysis();
        analysis.setPostContent(postContent);
        analysis.setPsychologicalVector(psychologicalVector);
        analysis.setFactorClassifications(new ArrayList<>());
        analysis.setDominantFactors(new ArrayList<>());
        analysis.setFactorInsights(new ArrayList<>());
        analysis.setEmotionalAnalysis(new EmotionalPatternAnalysis());
        analysis.setTherapeuticRecommendations(new ArrayList<>());
        analysis.setPsychologicalState(new PsychologicalState());
        analysis.setTimestamp(new Date());
        return analysis;
    }
    
    // ==================== INNER CLASSES ====================
    
    public static class PostFactorAnalysis {
        private String postContent;
        private List<Double> psychologicalVector;
        private List<MentalFactorClassifier.FactorClassification> factorClassifications;
        private List<DominantFactor> dominantFactors;
        private List<FactorInsight> factorInsights;
        private EmotionalPatternAnalysis emotionalAnalysis;
        private List<TherapeuticRecommendation> therapeuticRecommendations;
        private PsychologicalState psychologicalState;
        private Date timestamp;
        
        // Getters and setters
        public String getPostContent() { return postContent; }
        public void setPostContent(String postContent) { this.postContent = postContent; }
        
        public List<Double> getPsychologicalVector() { return psychologicalVector; }
        public void setPsychologicalVector(List<Double> psychologicalVector) { this.psychologicalVector = psychologicalVector; }
        
        public List<MentalFactorClassifier.FactorClassification> getFactorClassifications() { return factorClassifications; }
        public void setFactorClassifications(List<MentalFactorClassifier.FactorClassification> factorClassifications) { this.factorClassifications = factorClassifications; }
        
        public List<DominantFactor> getDominantFactors() { return dominantFactors; }
        public void setDominantFactors(List<DominantFactor> dominantFactors) { this.dominantFactors = dominantFactors; }
        
        public List<FactorInsight> getFactorInsights() { return factorInsights; }
        public void setFactorInsights(List<FactorInsight> factorInsights) { this.factorInsights = factorInsights; }
        
        public EmotionalPatternAnalysis getEmotionalAnalysis() { return emotionalAnalysis; }
        public void setEmotionalAnalysis(EmotionalPatternAnalysis emotionalAnalysis) { this.emotionalAnalysis = emotionalAnalysis; }
        
        public List<TherapeuticRecommendation> getTherapeuticRecommendations() { return therapeuticRecommendations; }
        public void setTherapeuticRecommendations(List<TherapeuticRecommendation> therapeuticRecommendations) { this.therapeuticRecommendations = therapeuticRecommendations; }
        
        public PsychologicalState getPsychologicalState() { return psychologicalState; }
        public void setPsychologicalState(PsychologicalState psychologicalState) { this.psychologicalState = psychologicalState; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class DominantFactor {
        private int factorIndex;
        private String factorName;
        private double intensity;
        private BuddhistPsychologyMapper.FactorCategory category;
        private String significance;
        private String therapeuticRelevance;
        private String modernPsychologyMapping;
        private String therapeuticPriority;
        private Date timestamp;
        
        // Getters and setters
        public int getFactorIndex() { return factorIndex; }
        public void setFactorIndex(int factorIndex) { this.factorIndex = factorIndex; }
        
        public String getFactorName() { return factorName; }
        public void setFactorName(String factorName) { this.factorName = factorName; }
        
        public double getIntensity() { return intensity; }
        public void setIntensity(double intensity) { this.intensity = intensity; }
        
        public BuddhistPsychologyMapper.FactorCategory getCategory() { return category; }
        public void setCategory(BuddhistPsychologyMapper.FactorCategory category) { this.category = category; }
        
        public String getSignificance() { return significance; }
        public void setSignificance(String significance) { this.significance = significance; }
        
        public String getTherapeuticRelevance() { return therapeuticRelevance; }
        public void setTherapeuticRelevance(String therapeuticRelevance) { this.therapeuticRelevance = therapeuticRelevance; }
        
        public String getModernPsychologyMapping() { return modernPsychologyMapping; }
        public void setModernPsychologyMapping(String modernPsychologyMapping) { this.modernPsychologyMapping = modernPsychologyMapping; }
        
        public String getTherapeuticPriority() { return therapeuticPriority; }
        public void setTherapeuticPriority(String therapeuticPriority) { this.therapeuticPriority = therapeuticPriority; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class FactorInsight {
        private String factorName;
        private String insightType;
        private String description;
        private String therapeuticImplication;
        private String modernPsychologyContext;
        private double confidence;
        private Date timestamp;
        
        // Getters and setters
        public String getFactorName() { return factorName; }
        public void setFactorName(String factorName) { this.factorName = factorName; }
        
        public String getInsightType() { return insightType; }
        public void setInsightType(String insightType) { this.insightType = insightType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTherapeuticImplication() { return therapeuticImplication; }
        public void setTherapeuticImplication(String therapeuticImplication) { this.therapeuticImplication = therapeuticImplication; }
        
        public String getModernPsychologyContext() { return modernPsychologyContext; }
        public void setModernPsychologyContext(String modernPsychologyContext) { this.modernPsychologyContext = modernPsychologyContext; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class EmotionalPatternAnalysis {
        private int emotionalFactorCount;
        private double averageEmotionalIntensity;
        private double maxEmotionalIntensity;
        private String emotionalState;
        private Date timestamp;
        
        // Getters and setters
        public int getEmotionalFactorCount() { return emotionalFactorCount; }
        public void setEmotionalFactorCount(int emotionalFactorCount) { this.emotionalFactorCount = emotionalFactorCount; }
        
        public double getAverageEmotionalIntensity() { return averageEmotionalIntensity; }
        public void setAverageEmotionalIntensity(double averageEmotionalIntensity) { this.averageEmotionalIntensity = averageEmotionalIntensity; }
        
        public double getMaxEmotionalIntensity() { return maxEmotionalIntensity; }
        public void setMaxEmotionalIntensity(double maxEmotionalIntensity) { this.maxEmotionalIntensity = maxEmotionalIntensity; }
        
        public String getEmotionalState() { return emotionalState; }
        public void setEmotionalState(String emotionalState) { this.emotionalState = emotionalState; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class TherapeuticRecommendation {
        private String factorName;
        private BuddhistPsychologyMapper.FactorCategory category;
        private String priority;
        private String interventionType;
        private String description;
        private String modernPsychologyApproach;
        private Date timestamp;
        
        // Getters and setters
        public String getFactorName() { return factorName; }
        public void setFactorName(String factorName) { this.factorName = factorName; }
        
        public BuddhistPsychologyMapper.FactorCategory getCategory() { return category; }
        public void setCategory(BuddhistPsychologyMapper.FactorCategory category) { this.category = category; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getInterventionType() { return interventionType; }
        public void setInterventionType(String interventionType) { this.interventionType = interventionType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getModernPsychologyApproach() { return modernPsychologyApproach; }
        public void setModernPsychologyApproach(String modernPsychologyApproach) { this.modernPsychologyApproach = modernPsychologyApproach; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class PsychologicalState {
        private double overallIntensity;
        private String stateLevel;
        private Map<BuddhistPsychologyMapper.FactorCategory, Double> categoryDistribution;
        private Date timestamp;
        
        // Getters and setters
        public double getOverallIntensity() { return overallIntensity; }
        public void setOverallIntensity(double overallIntensity) { this.overallIntensity = overallIntensity; }
        
        public String getStateLevel() { return stateLevel; }
        public void setStateLevel(String stateLevel) { this.stateLevel = stateLevel; }
        
        public Map<BuddhistPsychologyMapper.FactorCategory, Double> getCategoryDistribution() { return categoryDistribution; }
        public void setCategoryDistribution(Map<BuddhistPsychologyMapper.FactorCategory, Double> categoryDistribution) { this.categoryDistribution = categoryDistribution; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
}
