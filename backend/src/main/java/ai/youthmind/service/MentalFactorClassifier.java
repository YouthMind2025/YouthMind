package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classifies psychological factors into categories and analyzes patterns
 * 
 * This service provides sophisticated classification of the 43-dimensional
 * Buddhist mental factors system into meaningful psychological categories
 * and analyzes patterns for therapeutic and research purposes.
 */
@Service
public class MentalFactorClassifier {
    
    private static final Logger logger = LoggerFactory.getLogger(MentalFactorClassifier.class);
    
    @Autowired
    private BuddhistPsychologyMapper buddhistPsychologyMapper;
    
    // Classification thresholds
    private static final double HIGH_INTENSITY_THRESHOLD = 0.7;
    private static final double MEDIUM_INTENSITY_THRESHOLD = 0.4;
    private static final double LOW_INTENSITY_THRESHOLD = 0.1;
    
    /**
     * Classify a specific factor based on its intensity and characteristics
     * 
     * @param factorIndex Index of the factor (0-42)
     * @param intensity Factor intensity value
     * @return Factor classification result
     */
    public FactorClassification classifyFactor(int factorIndex, double intensity) {
        try {
            logger.debug("Classifying factor {} with intensity {}", factorIndex, intensity);
            
            if (factorIndex < 0 || factorIndex >= 43) {
                throw new IllegalArgumentException("Factor index must be between 0 and 42");
            }
            
            BuddhistPsychologyMapper.FactorDescription factorDesc = 
                buddhistPsychologyMapper.getFactorDescription(factorIndex);
            
            FactorClassification classification = new FactorClassification();
            classification.setFactorIndex(factorIndex);
            classification.setFactorName(factorDesc.getName());
            classification.setIntensity(intensity);
            classification.setCategory(factorDesc.getCategory());
            classification.setIntensityLevel(determineIntensityLevel(intensity));
            classification.setSignificance(determineSignificance(intensity));
            classification.setTherapeuticRelevance(determineTherapeuticRelevance(factorDesc, intensity));
            classification.setPatternType(identifyPatternType(factorDesc, intensity));
            classification.setTimestamp(new Date());
            
            logger.debug("Classified factor {} as {} (intensity: {})", 
                        factorIndex, classification.getIntensityLevel(), intensity);
            
            return classification;
            
        } catch (Exception e) {
            logger.error("Error classifying factor {}: {}", factorIndex, e.getMessage(), e);
            return createErrorClassification(factorIndex, intensity, e.getMessage());
        }
    }
    
    /**
     * Get all factors in a specific category
     * 
     * @param category Factor category
     * @return List of factors in the category
     */
    public List<FactorClassification> getCategoryFactors(BuddhistPsychologyMapper.FactorCategory category) {
        try {
            logger.debug("Getting factors for category {}", category);
            
            List<BuddhistPsychologyMapper.FactorDescription> categoryFactors = 
                buddhistPsychologyMapper.getFactorsByCategory(category);
            
            return categoryFactors.stream()
                .map(factor -> {
                    FactorClassification classification = new FactorClassification();
                    classification.setFactorIndex(factor.getIndex());
                    classification.setFactorName(factor.getName());
                    classification.setCategory(factor.getCategory());
                    classification.setIntensity(0.0); // Default intensity
                    classification.setIntensityLevel("UNKNOWN");
                    classification.setSignificance("UNKNOWN");
                    classification.setTherapeuticRelevance("UNKNOWN");
                    classification.setPatternType("UNKNOWN");
                    classification.setTimestamp(new Date());
                    return classification;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error getting category factors for {}: {}", category, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Analyze factor patterns in a 43-dimensional vector
     * 
     * @param vector 43-dimensional Buddhist mental factors vector
     * @return Factor pattern analysis result
     */
    public FactorPatternAnalysis analyzeFactorPatterns(List<Double> vector) {
        try {
            logger.debug("Analyzing factor patterns in 43D vector");
            
            if (vector == null || vector.size() != 43) {
                throw new IllegalArgumentException("Vector must be exactly 43 dimensions");
            }
            
            FactorPatternAnalysis analysis = new FactorPatternAnalysis();
            analysis.setVector(vector);
            
            // Classify all factors
            List<FactorClassification> classifications = new ArrayList<>();
            for (int i = 0; i < vector.size(); i++) {
                FactorClassification classification = classifyFactor(i, vector.get(i));
                classifications.add(classification);
            }
            analysis.setFactorClassifications(classifications);
            
            // Analyze patterns by category
            Map<BuddhistPsychologyMapper.FactorCategory, CategoryPattern> categoryPatterns = 
                analyzeCategoryPatterns(classifications);
            analysis.setCategoryPatterns(categoryPatterns);
            
            // Identify dominant patterns
            List<DominantPattern> dominantPatterns = identifyDominantPatterns(classifications);
            analysis.setDominantPatterns(dominantPatterns);
            
            // Calculate pattern coherence
            double coherence = calculatePatternCoherence(classifications);
            analysis.setCoherence(coherence);
            
            // Generate insights
            List<String> insights = generatePatternInsights(analysis);
            analysis.setInsights(insights);
            
            analysis.setTimestamp(new Date());
            
            logger.debug("Factor pattern analysis completed (coherence: {})", coherence);
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error analyzing factor patterns: {}", e.getMessage(), e);
            return createErrorPatternAnalysis(vector, e.getMessage());
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private String determineIntensityLevel(double intensity) {
        if (intensity >= HIGH_INTENSITY_THRESHOLD) {
            return "HIGH";
        } else if (intensity >= MEDIUM_INTENSITY_THRESHOLD) {
            return "MEDIUM";
        } else if (intensity >= LOW_INTENSITY_THRESHOLD) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }
    
    private String determineSignificance(double intensity) {
        if (intensity >= HIGH_INTENSITY_THRESHOLD) {
            return "HIGHLY_SIGNIFICANT";
        } else if (intensity >= MEDIUM_INTENSITY_THRESHOLD) {
            return "SIGNIFICANT";
        } else if (intensity >= LOW_INTENSITY_THRESHOLD) {
            return "MODERATELY_SIGNIFICANT";
        } else {
            return "NOT_SIGNIFICANT";
        }
    }
    
    private String determineTherapeuticRelevance(BuddhistPsychologyMapper.FactorDescription factorDesc, double intensity) {
        if (intensity >= HIGH_INTENSITY_THRESHOLD) {
            return "HIGH_THERAPEUTIC_PRIORITY";
        } else if (intensity >= MEDIUM_INTENSITY_THRESHOLD) {
            return "MODERATE_THERAPEUTIC_PRIORITY";
        } else if (intensity >= LOW_INTENSITY_THRESHOLD) {
            return "LOW_THERAPEUTIC_PRIORITY";
        } else {
            return "NO_THERAPEUTIC_PRIORITY";
        }
    }
    
    private String identifyPatternType(BuddhistPsychologyMapper.FactorDescription factorDesc, double intensity) {
        BuddhistPsychologyMapper.FactorCategory category = factorDesc.getCategory();
        
        if (intensity >= HIGH_INTENSITY_THRESHOLD) {
            return "DOMINANT_" + category.name();
        } else if (intensity >= MEDIUM_INTENSITY_THRESHOLD) {
            return "PRESENT_" + category.name();
        } else if (intensity >= LOW_INTENSITY_THRESHOLD) {
            return "SUBTLE_" + category.name();
        } else {
            return "ABSENT_" + category.name();
        }
    }
    
    private Map<BuddhistPsychologyMapper.FactorCategory, CategoryPattern> analyzeCategoryPatterns(
            List<FactorClassification> classifications) {
        
        Map<BuddhistPsychologyMapper.FactorCategory, CategoryPattern> patterns = new HashMap<>();
        
        // Group by category
        Map<BuddhistPsychologyMapper.FactorCategory, List<FactorClassification>> categoryGroups = 
            classifications.stream()
                .collect(Collectors.groupingBy(FactorClassification::getCategory));
        
        // Analyze each category
        for (Map.Entry<BuddhistPsychologyMapper.FactorCategory, List<FactorClassification>> entry : 
             categoryGroups.entrySet()) {
            
            BuddhistPsychologyMapper.FactorCategory category = entry.getKey();
            List<FactorClassification> factors = entry.getValue();
            
            CategoryPattern pattern = new CategoryPattern();
            pattern.setCategory(category);
            pattern.setFactorCount(factors.size());
            
            // Calculate category statistics
            double averageIntensity = factors.stream()
                .mapToDouble(FactorClassification::getIntensity)
                .average().orElse(0.0);
            pattern.setAverageIntensity(averageIntensity);
            
            double maxIntensity = factors.stream()
                .mapToDouble(FactorClassification::getIntensity)
                .max().orElse(0.0);
            pattern.setMaxIntensity(maxIntensity);
            
            long highIntensityCount = factors.stream()
                .filter(f -> f.getIntensity() >= HIGH_INTENSITY_THRESHOLD)
                .count();
            pattern.setHighIntensityCount((int) highIntensityCount);
            
            pattern.setDominanceLevel(determineCategoryDominance(averageIntensity, highIntensityCount));
            pattern.setTimestamp(new Date());
            
            patterns.put(category, pattern);
        }
        
        return patterns;
    }
    
    private String determineCategoryDominance(double averageIntensity, long highIntensityCount) {
        if (averageIntensity >= HIGH_INTENSITY_THRESHOLD && highIntensityCount >= 2) {
            return "HIGHLY_DOMINANT";
        } else if (averageIntensity >= MEDIUM_INTENSITY_THRESHOLD && highIntensityCount >= 1) {
            return "DOMINANT";
        } else if (averageIntensity >= LOW_INTENSITY_THRESHOLD) {
            return "MODERATELY_PRESENT";
        } else {
            return "MINIMAL_PRESENCE";
        }
    }
    
    private List<DominantPattern> identifyDominantPatterns(List<FactorClassification> classifications) {
        List<DominantPattern> patterns = new ArrayList<>();
        
        // Find high-intensity factors
        List<FactorClassification> highIntensityFactors = classifications.stream()
            .filter(f -> f.getIntensity() >= HIGH_INTENSITY_THRESHOLD)
            .sorted((a, b) -> Double.compare(b.getIntensity(), a.getIntensity()))
            .collect(Collectors.toList());
        
        for (FactorClassification factor : highIntensityFactors) {
            DominantPattern pattern = new DominantPattern();
            pattern.setFactorName(factor.getFactorName());
            pattern.setFactorIndex(factor.getFactorIndex());
            pattern.setIntensity(factor.getIntensity());
            pattern.setCategory(factor.getCategory());
            pattern.setSignificance(factor.getSignificance());
            pattern.setTherapeuticRelevance(factor.getTherapeuticRelevance());
            pattern.setTimestamp(new Date());
            patterns.add(pattern);
        }
        
        return patterns;
    }
    
    private double calculatePatternCoherence(List<FactorClassification> classifications) {
        // Calculate coherence based on consistency within categories
        Map<BuddhistPsychologyMapper.FactorCategory, List<FactorClassification>> categoryGroups = 
            classifications.stream()
                .collect(Collectors.groupingBy(FactorClassification::getCategory));
        
        double totalCoherence = 0.0;
        int categoryCount = 0;
        
        for (List<FactorClassification> factors : categoryGroups.values()) {
            if (factors.size() > 1) {
                double categoryCoherence = calculateCategoryCoherence(factors);
                totalCoherence += categoryCoherence;
                categoryCount++;
            }
        }
        
        return categoryCount > 0 ? totalCoherence / categoryCount : 0.0;
    }
    
    private double calculateCategoryCoherence(List<FactorClassification> factors) {
        // Calculate variance within category
        double mean = factors.stream()
            .mapToDouble(FactorClassification::getIntensity)
            .average().orElse(0.0);
        
        double variance = factors.stream()
            .mapToDouble(f -> Math.pow(f.getIntensity() - mean, 2))
            .average().orElse(0.0);
        
        // Lower variance = higher coherence
        return Math.max(0.0, 1.0 - Math.sqrt(variance));
    }
    
    private List<String> generatePatternInsights(FactorPatternAnalysis analysis) {
        List<String> insights = new ArrayList<>();
        
        // Generate insights based on dominant patterns
        List<DominantPattern> dominantPatterns = analysis.getDominantPatterns();
        if (!dominantPatterns.isEmpty()) {
            insights.add(String.format("Found %d dominant psychological patterns", dominantPatterns.size()));
            
            BuddhistPsychologyMapper.FactorCategory mostDominantCategory = 
                dominantPatterns.stream()
                    .collect(Collectors.groupingBy(DominantPattern::getCategory, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            
            if (mostDominantCategory != null) {
                insights.add(String.format("Most dominant category: %s", mostDominantCategory.name()));
            }
        }
        
        // Generate insights based on coherence
        double coherence = analysis.getCoherence();
        if (coherence >= 0.8) {
            insights.add("High pattern coherence - consistent psychological profile");
        } else if (coherence >= 0.6) {
            insights.add("Moderate pattern coherence - some psychological consistency");
        } else {
            insights.add("Low pattern coherence - mixed psychological patterns");
        }
        
        // Generate insights based on category patterns
        Map<BuddhistPsychologyMapper.FactorCategory, CategoryPattern> categoryPatterns = 
            analysis.getCategoryPatterns();
        
        long dominantCategories = categoryPatterns.values().stream()
            .filter(p -> p.getDominanceLevel().contains("DOMINANT"))
            .count();
        
        insights.add(String.format("Number of dominant psychological categories: %d", dominantCategories));
        
        return insights;
    }
    
    private FactorClassification createErrorClassification(int factorIndex, double intensity, String errorMessage) {
        FactorClassification classification = new FactorClassification();
        classification.setFactorIndex(factorIndex);
        classification.setFactorName("ERROR");
        classification.setIntensity(intensity);
        classification.setCategory(BuddhistPsychologyMapper.FactorCategory.COGNITIVE);
        classification.setIntensityLevel("ERROR");
        classification.setSignificance("ERROR");
        classification.setTherapeuticRelevance("ERROR");
        classification.setPatternType("ERROR");
        classification.setTimestamp(new Date());
        return classification;
    }
    
    private FactorPatternAnalysis createErrorPatternAnalysis(List<Double> vector, String errorMessage) {
        FactorPatternAnalysis analysis = new FactorPatternAnalysis();
        analysis.setVector(vector);
        analysis.setFactorClassifications(new ArrayList<>());
        analysis.setCategoryPatterns(new HashMap<>());
        analysis.setDominantPatterns(new ArrayList<>());
        analysis.setCoherence(0.0);
        analysis.setInsights(Arrays.asList("Error: " + errorMessage));
        analysis.setTimestamp(new Date());
        return analysis;
    }
    
    // ==================== INNER CLASSES ====================
    
    public static class FactorClassification {
        private int factorIndex;
        private String factorName;
        private double intensity;
        private BuddhistPsychologyMapper.FactorCategory category;
        private String intensityLevel;
        private String significance;
        private String therapeuticRelevance;
        private String patternType;
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
        
        public String getIntensityLevel() { return intensityLevel; }
        public void setIntensityLevel(String intensityLevel) { this.intensityLevel = intensityLevel; }
        
        public String getSignificance() { return significance; }
        public void setSignificance(String significance) { this.significance = significance; }
        
        public String getTherapeuticRelevance() { return therapeuticRelevance; }
        public void setTherapeuticRelevance(String therapeuticRelevance) { this.therapeuticRelevance = therapeuticRelevance; }
        
        public String getPatternType() { return patternType; }
        public void setPatternType(String patternType) { this.patternType = patternType; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class CategoryPattern {
        private BuddhistPsychologyMapper.FactorCategory category;
        private int factorCount;
        private double averageIntensity;
        private double maxIntensity;
        private int highIntensityCount;
        private String dominanceLevel;
        private Date timestamp;
        
        // Getters and setters
        public BuddhistPsychologyMapper.FactorCategory getCategory() { return category; }
        public void setCategory(BuddhistPsychologyMapper.FactorCategory category) { this.category = category; }
        
        public int getFactorCount() { return factorCount; }
        public void setFactorCount(int factorCount) { this.factorCount = factorCount; }
        
        public double getAverageIntensity() { return averageIntensity; }
        public void setAverageIntensity(double averageIntensity) { this.averageIntensity = averageIntensity; }
        
        public double getMaxIntensity() { return maxIntensity; }
        public void setMaxIntensity(double maxIntensity) { this.maxIntensity = maxIntensity; }
        
        public int getHighIntensityCount() { return highIntensityCount; }
        public void setHighIntensityCount(int highIntensityCount) { this.highIntensityCount = highIntensityCount; }
        
        public String getDominanceLevel() { return dominanceLevel; }
        public void setDominanceLevel(String dominanceLevel) { this.dominanceLevel = dominanceLevel; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class DominantPattern {
        private String factorName;
        private int factorIndex;
        private double intensity;
        private BuddhistPsychologyMapper.FactorCategory category;
        private String significance;
        private String therapeuticRelevance;
        private Date timestamp;
        
        // Getters and setters
        public String getFactorName() { return factorName; }
        public void setFactorName(String factorName) { this.factorName = factorName; }
        
        public int getFactorIndex() { return factorIndex; }
        public void setFactorIndex(int factorIndex) { this.factorIndex = factorIndex; }
        
        public double getIntensity() { return intensity; }
        public void setIntensity(double intensity) { this.intensity = intensity; }
        
        public BuddhistPsychologyMapper.FactorCategory getCategory() { return category; }
        public void setCategory(BuddhistPsychologyMapper.FactorCategory category) { this.category = category; }
        
        public String getSignificance() { return significance; }
        public void setSignificance(String significance) { this.significance = significance; }
        
        public String getTherapeuticRelevance() { return therapeuticRelevance; }
        public void setTherapeuticRelevance(String therapeuticRelevance) { this.therapeuticRelevance = therapeuticRelevance; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
    
    public static class FactorPatternAnalysis {
        private List<Double> vector;
        private List<FactorClassification> factorClassifications;
        private Map<BuddhistPsychologyMapper.FactorCategory, CategoryPattern> categoryPatterns;
        private List<DominantPattern> dominantPatterns;
        private double coherence;
        private List<String> insights;
        private Date timestamp;
        
        // Getters and setters
        public List<Double> getVector() { return vector; }
        public void setVector(List<Double> vector) { this.vector = vector; }
        
        public List<FactorClassification> getFactorClassifications() { return factorClassifications; }
        public void setFactorClassifications(List<FactorClassification> factorClassifications) { this.factorClassifications = factorClassifications; }
        
        public Map<BuddhistPsychologyMapper.FactorCategory, CategoryPattern> getCategoryPatterns() { return categoryPatterns; }
        public void setCategoryPatterns(Map<BuddhistPsychologyMapper.FactorCategory, CategoryPattern> categoryPatterns) { this.categoryPatterns = categoryPatterns; }
        
        public List<DominantPattern> getDominantPatterns() { return dominantPatterns; }
        public void setDominantPatterns(List<DominantPattern> dominantPatterns) { this.dominantPatterns = dominantPatterns; }
        
        public double getCoherence() { return coherence; }
        public void setCoherence(double coherence) { this.coherence = coherence; }
        
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
        
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }
}

