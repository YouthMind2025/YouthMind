package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for analyzing Buddhist mental factors
 * Implements sophisticated Buddhist psychology analysis algorithms
 */
@Service
@Transactional
public class BuddhistFactorAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(BuddhistFactorAnalyzer.class);
    
    // Buddhist mental factors (52 total)
    private static final Map<String, List<String>> BUDDHIST_FACTORS = createBuddhistFactorsMap();
    
    private static Map<String, List<String>> createBuddhistFactorsMap() {
        Map<String, List<String>> factors = new HashMap<>();
        
        // Five Hindrances (Nivarana)
        factors.put("sensual_desire", Arrays.asList("desire", "lust", "craving", "attachment", "want"));
        factors.put("ill_will", Arrays.asList("anger", "hatred", "aversion", "hostility", "resentment"));
        factors.put("sloth_torpor", Arrays.asList("lazy", "dull", "sluggish", "lethargic", "inactive"));
        factors.put("restlessness_worry", Arrays.asList("restless", "worried", "anxious", "agitated", "distracted"));
        factors.put("doubt", Arrays.asList("doubt", "uncertain", "skeptical", "hesitant", "confused"));
        
        // Seven Factors of Enlightenment (Bojjhanga)
        factors.put("mindfulness", Arrays.asList("mindful", "aware", "present", "conscious", "attentive"));
        factors.put("investigation", Arrays.asList("investigate", "examine", "analyze", "explore", "inquire"));
        factors.put("energy", Arrays.asList("energetic", "vigorous", "active", "dynamic", "enthusiastic"));
        factors.put("joy", Arrays.asList("joyful", "happy", "delighted", "cheerful", "blissful"));
        factors.put("tranquility", Arrays.asList("calm", "peaceful", "serene", "tranquil", "quiet"));
        factors.put("concentration", Arrays.asList("focused", "concentrated", "centered", "collected", "unified"));
        factors.put("equanimity", Arrays.asList("balanced", "equanimous", "neutral", "impartial", "stable"));
        
        // Ten Perfections (Paramis)
        factors.put("generosity", Arrays.asList("generous", "giving", "charitable", "kind", "helpful"));
        factors.put("morality", Arrays.asList("moral", "ethical", "virtuous", "righteous", "principled"));
        factors.put("renunciation", Arrays.asList("renounce", "let go", "detach", "release", "abandon"));
        factors.put("wisdom", Arrays.asList("wise", "insightful", "understanding", "knowledgeable", "enlightened"));
        factors.put("energy_perfection", Arrays.asList("perseverance", "determination", "effort", "diligence", "persistence"));
        factors.put("patience", Arrays.asList("patient", "tolerant", "enduring", "forbearing", "accepting"));
        factors.put("truthfulness", Arrays.asList("truthful", "honest", "sincere", "authentic", "genuine"));
        factors.put("resolution", Arrays.asList("resolved", "committed", "dedicated", "firm", "steadfast"));
        factors.put("loving_kindness", Arrays.asList("loving", "kind", "compassionate", "caring", "benevolent"));
        factors.put("equanimity_perfection", Arrays.asList("balanced", "equanimous", "impartial", "unbiased", "stable"));
        
        return factors;
    }
    
    // Redis keys
    private static final String BUDDHIST_FACTORS_KEY = "buddhist_factors:";
    private static final String FACTOR_ANALYSIS_KEY = "factor_analysis:";
    private static final String FACTOR_HISTORY_KEY = "factor_history:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Analyze Buddhist mental factors in content
     * @param content Text content to analyze
     * @param userId User ID for personalization
     * @return Buddhist factors analysis result
     */
    public Map<String, Object> analyzeFactors(String content, UUID userId) {
        try {
            logger.debug("Analyzing Buddhist factors for user {} with content length {}", userId, content.length());
            
            // Check cache first
            String cacheKey = FACTOR_ANALYSIS_KEY + userId + ":" + content.hashCode();
            Map<String, Object> cachedAnalysis = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedAnalysis != null) {
                logger.debug("Using cached Buddhist factors analysis for user {}", userId);
                return cachedAnalysis;
            }
            
            // Analyze Buddhist factors
            Map<String, Double> factorScores = calculateFactorScores(content);
            
            // Classify factors
            Map<String, Object> factorClassification = classifyFactors(factorScores);
            
            // Extract factor intensity
            Map<String, Object> factorIntensity = extractFactorIntensity(factorScores);
            
            // Generate factor report
            Map<String, Object> factorReport = generateFactorReport(factorScores, factorClassification, factorIntensity);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("factorScores", factorScores);
            result.put("factorClassification", factorClassification);
            result.put("factorIntensity", factorIntensity);
            result.put("factorReport", factorReport);
            result.put("analysisMethod", "BUDDHIST_PSYCHOLOGY");
            result.put("timestamp", LocalDateTime.now());
            
            // Store analysis
            storeFactorAnalysis(userId, result);
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, result, 2, TimeUnit.HOURS);
            
            logger.debug("Buddhist factors analysis completed for user {}", userId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing Buddhist factors for user {}: {}", userId, e.getMessage(), e);
            return createDefaultFactorAnalysis(userId);
        }
    }
    
    /**
     * Classify Buddhist factors
     * @param factorScores Factor scores to classify
     * @return Factor classification
     */
    public Map<String, Object> classifyFactors(Map<String, Double> factorScores) {
        try {
            logger.debug("Classifying Buddhist factors");
            
            Map<String, Object> classification = new HashMap<>();
            
            // Classify by intensity levels
            Map<String, List<String>> intensityLevels = Map.of(
                "high", new ArrayList<>(),
                "moderate", new ArrayList<>(),
                "low", new ArrayList<>(),
                "minimal", new ArrayList<>()
            );
            
            for (Map.Entry<String, Double> entry : factorScores.entrySet()) {
                String factor = entry.getKey();
                Double score = entry.getValue();
                
                if (score >= 0.7) {
                    intensityLevels.get("high").add(factor);
                } else if (score >= 0.4) {
                    intensityLevels.get("moderate").add(factor);
                } else if (score >= 0.2) {
                    intensityLevels.get("low").add(factor);
                } else {
                    intensityLevels.get("minimal").add(factor);
                }
            }
            
            classification.put("intensityLevels", intensityLevels);
            
            // Classify by factor categories
            Map<String, Object> categories = classifyByCategories(factorScores);
            classification.put("categories", categories);
            
            // Classify by psychological impact
            Map<String, Object> psychologicalImpact = classifyByPsychologicalImpact(factorScores);
            classification.put("psychologicalImpact", psychologicalImpact);
            
            logger.debug("Buddhist factors classified");
            return classification;
            
        } catch (Exception e) {
            logger.error("Error classifying Buddhist factors: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Extract factor intensity
     * @param factorScores Factor scores
     * @return Factor intensity analysis
     */
    public Map<String, Object> extractFactorIntensity(Map<String, Double> factorScores) {
        try {
            logger.debug("Extracting factor intensity");
            
            Map<String, Object> intensity = new HashMap<>();
            
            // Calculate overall intensity
            double overallIntensity = factorScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
            intensity.put("overallIntensity", overallIntensity);
            
            // Find dominant factors
            List<String> dominantFactors = factorScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            intensity.put("dominantFactors", dominantFactors);
            
            // Calculate intensity distribution
            Map<String, Integer> intensityDistribution = calculateIntensityDistribution(factorScores);
            intensity.put("intensityDistribution", intensityDistribution);
            
            // Calculate factor balance
            double factorBalance = calculateFactorBalance(factorScores);
            intensity.put("factorBalance", factorBalance);
            
            logger.debug("Factor intensity extracted");
            return intensity;
            
        } catch (Exception e) {
            logger.error("Error extracting factor intensity: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Generate factor report
     * @param factorScores Factor scores
     * @param classification Factor classification
     * @param intensity Factor intensity
     * @return Comprehensive factor report
     */
    public Map<String, Object> generateFactorReport(Map<String, Double> factorScores, 
                                                   Map<String, Object> classification, 
                                                   Map<String, Object> intensity) {
        try {
            logger.debug("Generating factor report");
            
            Map<String, Object> report = new HashMap<>();
            
            // Generate insights
            List<String> insights = generateInsights(factorScores, classification, intensity);
            report.put("insights", insights);
            
            // Generate recommendations
            List<String> recommendations = generateRecommendations(factorScores, classification, intensity);
            report.put("recommendations", recommendations);
            
            // Generate summary
            String summary = generateSummary(factorScores, classification, intensity);
            report.put("summary", summary);
            
            // Generate detailed analysis
            Map<String, Object> detailedAnalysis = generateDetailedAnalysis(factorScores, classification, intensity);
            report.put("detailedAnalysis", detailedAnalysis);
            
            logger.debug("Factor report generated");
            return report;
            
        } catch (Exception e) {
            logger.error("Error generating factor report: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get Buddhist factors analysis for a user
     * @param userId User ID
     * @return Latest Buddhist factors analysis
     */
    public Map<String, Object> getFactorAnalysis(UUID userId) {
        try {
            logger.debug("Getting Buddhist factors analysis for user {}", userId);
            
            String storageKey = BUDDHIST_FACTORS_KEY + userId;
            Map<String, Object> analysis = (Map<String, Object>) redisTemplate.opsForValue().get(storageKey);
            
            return analysis != null ? analysis : createDefaultFactorAnalysis(userId);
            
        } catch (Exception e) {
            logger.error("Error getting Buddhist factors analysis for user {}: {}", userId, e.getMessage(), e);
            return createDefaultFactorAnalysis(userId);
        }
    }
    
    /**
     * Get factor analysis history for a user
     * @param userId User ID
     * @return Factor analysis history
     */
    public Map<String, Object> getFactorHistory(UUID userId) {
        try {
            logger.debug("Getting factor analysis history for user {}", userId);
            
            String historyKey = FACTOR_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            return history != null ? history : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting factor analysis history for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private Map<String, Double> calculateFactorScores(String content) {
        Map<String, Double> factorScores = new HashMap<>();
        
        String lowerContent = content.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : BUDDHIST_FACTORS.entrySet()) {
            String factor = entry.getKey();
            List<String> keywords = entry.getValue();
            
            double score = calculateFactorScore(lowerContent, keywords);
            factorScores.put(factor, score);
        }
        
        return factorScores;
    }
    
    private double calculateFactorScore(String content, List<String> keywords) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        
        int totalOccurrences = 0;
        int totalWords = content.split("\\s+").length;
        
        for (String keyword : keywords) {
            int occurrences = countOccurrences(content, keyword);
            totalOccurrences += occurrences;
        }
        
        // Normalize to [0, 1] range
        double score = Math.min(1.0, (double) totalOccurrences / Math.max(1, totalWords / 20));
        return score;
    }
    
    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
    
    private Map<String, Object> classifyByCategories(Map<String, Double> factorScores) {
        Map<String, Object> categories = new HashMap<>();
        
        // Hindrances
        List<String> hindrances = Arrays.asList("sensual_desire", "ill_will", "sloth_torpor", "restlessness_worry", "doubt");
        Map<String, Double> hindranceScores = new HashMap<>();
        for (String hindrance : hindrances) {
            hindranceScores.put(hindrance, factorScores.getOrDefault(hindrance, 0.0));
        }
        categories.put("hindrances", hindranceScores);
        
        // Factors of Enlightenment
        List<String> enlightenmentFactors = Arrays.asList("mindfulness", "investigation", "energy", "joy", "tranquility", "concentration", "equanimity");
        Map<String, Double> enlightenmentScores = new HashMap<>();
        for (String factor : enlightenmentFactors) {
            enlightenmentScores.put(factor, factorScores.getOrDefault(factor, 0.0));
        }
        categories.put("enlightenmentFactors", enlightenmentScores);
        
        // Perfections
        List<String> perfections = Arrays.asList("generosity", "morality", "renunciation", "wisdom", "energy_perfection", "patience", "truthfulness", "resolution", "loving_kindness", "equanimity_perfection");
        Map<String, Double> perfectionScores = new HashMap<>();
        for (String perfection : perfections) {
            perfectionScores.put(perfection, factorScores.getOrDefault(perfection, 0.0));
        }
        categories.put("perfections", perfectionScores);
        
        return categories;
    }
    
    private Map<String, Object> classifyByPsychologicalImpact(Map<String, Double> factorScores) {
        Map<String, Object> impact = new HashMap<>();
        
        // Positive factors
        List<String> positiveFactors = Arrays.asList("mindfulness", "joy", "tranquility", "concentration", "equanimity", "generosity", "wisdom", "patience", "loving_kindness");
        double positiveImpact = positiveFactors.stream()
            .mapToDouble(factor -> factorScores.getOrDefault(factor, 0.0))
            .average().orElse(0.0);
        impact.put("positiveImpact", positiveImpact);
        
        // Negative factors
        List<String> negativeFactors = Arrays.asList("sensual_desire", "ill_will", "sloth_torpor", "restlessness_worry", "doubt");
        double negativeImpact = negativeFactors.stream()
            .mapToDouble(factor -> factorScores.getOrDefault(factor, 0.0))
            .average().orElse(0.0);
        impact.put("negativeImpact", negativeImpact);
        
        // Neutral factors
        List<String> neutralFactors = Arrays.asList("investigation", "energy", "morality", "renunciation", "energy_perfection", "truthfulness", "resolution", "equanimity_perfection");
        double neutralImpact = neutralFactors.stream()
            .mapToDouble(factor -> factorScores.getOrDefault(factor, 0.0))
            .average().orElse(0.0);
        impact.put("neutralImpact", neutralImpact);
        
        return impact;
    }
    
    private Map<String, Integer> calculateIntensityDistribution(Map<String, Double> factorScores) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("high", 0);
        distribution.put("moderate", 0);
        distribution.put("low", 0);
        distribution.put("minimal", 0);
        
        for (Double score : factorScores.values()) {
            if (score >= 0.7) {
                distribution.put("high", distribution.get("high") + 1);
            } else if (score >= 0.4) {
                distribution.put("moderate", distribution.get("moderate") + 1);
            } else if (score >= 0.2) {
                distribution.put("low", distribution.get("low") + 1);
            } else {
                distribution.put("minimal", distribution.get("minimal") + 1);
            }
        }
        
        return distribution;
    }
    
    private double calculateFactorBalance(Map<String, Double> factorScores) {
        if (factorScores.isEmpty()) {
            return 0.0;
        }
        
        double mean = factorScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = factorScores.values().stream()
            .mapToDouble(score -> Math.pow(score - mean, 2))
            .average().orElse(0.0);
        
        // Lower variance indicates better balance
        return Math.max(0.0, 1.0 - Math.sqrt(variance));
    }
    
    private List<String> generateInsights(Map<String, Double> factorScores, 
                                        Map<String, Object> classification, 
                                        Map<String, Object> intensity) {
        List<String> insights = new ArrayList<>();
        
        // Generate insights based on dominant factors
        List<String> dominantFactors = (List<String>) intensity.get("dominantFactors");
        if (dominantFactors != null && !dominantFactors.isEmpty()) {
            insights.add("Dominant mental factors: " + String.join(", ", dominantFactors.subList(0, Math.min(3, dominantFactors.size()))));
        }
        
        // Generate insights based on hindrances
        Map<String, Object> categories = (Map<String, Object>) classification.get("categories");
        if (categories != null) {
            Map<String, Double> hindrances = (Map<String, Double>) categories.get("hindrances");
            if (hindrances != null) {
                String highestHindrance = hindrances.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                if (highestHindrance != null && hindrances.get(highestHindrance) > 0.3) {
                    insights.add("Primary hindrance identified: " + highestHindrance.replace("_", " "));
                }
            }
        }
        
        // Generate insights based on balance
        Double factorBalance = (Double) intensity.get("factorBalance");
        if (factorBalance != null) {
            if (factorBalance > 0.7) {
                insights.add("Mental factors are well-balanced");
            } else if (factorBalance < 0.3) {
                insights.add("Mental factors show significant imbalance");
            }
        }
        
        return insights;
    }
    
    private List<String> generateRecommendations(Map<String, Double> factorScores, 
                                                Map<String, Object> classification, 
                                                Map<String, Object> intensity) {
        List<String> recommendations = new ArrayList<>();
        
        // Generate recommendations based on hindrances
        Map<String, Object> categories = (Map<String, Object>) classification.get("categories");
        if (categories != null) {
            Map<String, Double> hindrances = (Map<String, Double>) categories.get("hindrances");
            if (hindrances != null) {
                for (Map.Entry<String, Double> entry : hindrances.entrySet()) {
                    if (entry.getValue() > 0.5) {
                        recommendations.add("Consider addressing " + entry.getKey().replace("_", " ") + " through mindfulness practice");
                    }
                }
            }
        }
        
        // Generate recommendations based on enlightenment factors
        Map<String, Double> enlightenmentFactors = (Map<String, Double>) categories.get("enlightenmentFactors");
        if (enlightenmentFactors != null) {
            for (Map.Entry<String, Double> entry : enlightenmentFactors.entrySet()) {
                if (entry.getValue() < 0.3) {
                    recommendations.add("Develop " + entry.getKey().replace("_", " ") + " through meditation and reflection");
                }
            }
        }
        
        return recommendations;
    }
    
    private String generateSummary(Map<String, Double> factorScores, 
                                 Map<String, Object> classification, 
                                 Map<String, Object> intensity) {
        StringBuilder summary = new StringBuilder();
        
        Double overallIntensity = (Double) intensity.get("overallIntensity");
        if (overallIntensity != null) {
            if (overallIntensity > 0.6) {
                summary.append("High intensity mental factors detected. ");
            } else if (overallIntensity > 0.3) {
                summary.append("Moderate intensity mental factors present. ");
            } else {
                summary.append("Low intensity mental factors observed. ");
            }
        }
        
        List<String> dominantFactors = (List<String>) intensity.get("dominantFactors");
        if (dominantFactors != null && !dominantFactors.isEmpty()) {
            summary.append("Primary factors: ").append(String.join(", ", dominantFactors.subList(0, Math.min(2, dominantFactors.size())))).append(". ");
        }
        
        Double factorBalance = (Double) intensity.get("factorBalance");
        if (factorBalance != null) {
            if (factorBalance > 0.7) {
                summary.append("Mental factors are well-balanced.");
            } else {
                summary.append("Mental factors show some imbalance.");
            }
        }
        
        return summary.toString();
    }
    
    private Map<String, Object> generateDetailedAnalysis(Map<String, Double> factorScores, 
                                                       Map<String, Object> classification, 
                                                       Map<String, Object> intensity) {
        Map<String, Object> detailedAnalysis = new HashMap<>();
        
        // Detailed analysis by category
        Map<String, Object> categories = (Map<String, Object>) classification.get("categories");
        if (categories != null) {
            detailedAnalysis.put("categoryAnalysis", categories);
        }
        
        // Detailed intensity analysis
        detailedAnalysis.put("intensityAnalysis", intensity);
        
        // Factor trends
        Map<String, Object> factorTrends = analyzeFactorTrends(factorScores);
        detailedAnalysis.put("factorTrends", factorTrends);
        
        return detailedAnalysis;
    }
    
    private Map<String, Object> analyzeFactorTrends(Map<String, Double> factorScores) {
        Map<String, Object> trends = new HashMap<>();
        
        // This would typically analyze trends over time
        // For now, we'll provide a simple analysis
        trends.put("trendAnalysis", "Historical trend analysis would be available with multiple data points");
        trends.put("trendDirection", "STABLE");
        trends.put("trendStrength", 0.0);
        
        return trends;
    }
    
    private void storeFactorAnalysis(UUID userId, Map<String, Object> analysis) {
        try {
            logger.debug("Storing Buddhist factors analysis for user {}", userId);
            
            String storageKey = BUDDHIST_FACTORS_KEY + userId;
            String historyKey = FACTOR_HISTORY_KEY + userId;
            
            // Store current analysis
            redisTemplate.opsForValue().set(storageKey, analysis, 6, TimeUnit.HOURS);
            
            // Update history
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            if (history == null) {
                history = new HashMap<>();
            }
            
            List<Map<String, Object>> analysisHistory = (List<Map<String, Object>>) history.get("analyses");
            if (analysisHistory == null) {
                analysisHistory = new ArrayList<>();
            }
            
            Map<String, Object> historyEntry = new HashMap<>();
            historyEntry.put("timestamp", LocalDateTime.now());
            historyEntry.put("analysis", analysis);
            
            analysisHistory.add(historyEntry);
            
            // Keep only recent analyses (last 50)
            if (analysisHistory.size() > 50) {
                analysisHistory = analysisHistory.subList(analysisHistory.size() - 50, analysisHistory.size());
            }
            
            history.put("analyses", analysisHistory);
            history.put("lastUpdated", LocalDateTime.now());
            
            redisTemplate.opsForValue().set(historyKey, history, 30, TimeUnit.DAYS);
            
            logger.debug("Buddhist factors analysis stored for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error storing Buddhist factors analysis for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    private Map<String, Object> createDefaultFactorAnalysis(UUID userId) {
        Map<String, Object> defaultAnalysis = new HashMap<>();
        defaultAnalysis.put("userId", userId.toString());
        defaultAnalysis.put("factorScores", new HashMap<>());
        defaultAnalysis.put("factorClassification", new HashMap<>());
        defaultAnalysis.put("factorIntensity", new HashMap<>());
        defaultAnalysis.put("factorReport", new HashMap<>());
        defaultAnalysis.put("analysisMethod", "DEFAULT");
        defaultAnalysis.put("timestamp", LocalDateTime.now());
        return defaultAnalysis;
    }
}
