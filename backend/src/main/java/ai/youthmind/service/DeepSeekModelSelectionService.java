package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for intelligent model selection based on request characteristics,
 * performance metrics, and cost optimization.
 */
@Service
public class DeepSeekModelSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekModelSelectionService.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    @Autowired
    private DeepSeekOffPeakRoutingService offPeakRoutingService;
    
    // Model performance tracking
    private final Map<String, ModelPerformanceMetrics> modelMetrics = new ConcurrentHashMap<>();
    
    // Request type to model mapping
    private final Map<String, List<String>> requestTypeModelPreferences = Map.of(
        "moderation", Arrays.asList("deepseek-chat", "deepseek-reasoner"),
        "chat", Arrays.asList("deepseek-chat", "deepseek-coder"),
        "analysis", Arrays.asList("deepseek-reasoner", "deepseek-chat"),
        "reasoning", Arrays.asList("deepseek-reasoner", "deepseek-chat"),
        "coding", Arrays.asList("deepseek-coder", "deepseek-chat"),
        "summarization", Arrays.asList("deepseek-chat", "deepseek-reasoner"),
        "translation", Arrays.asList("deepseek-chat", "deepseek-coder")
    );
    
    /**
     * Select optimal model for a request
     * 
     * @param requestType Type of request
     * @param complexity Complexity level (low, medium, high)
     * @param urgency Urgency level (low, medium, high)
     * @param costSensitivity Cost sensitivity (low, medium, high)
     * @param userId User ID for personalized selection
     * @return ModelSelectionResult with selected model and reasoning
     */
    public ModelSelectionResult selectOptimalModel(String requestType, String complexity, 
                                                 String urgency, String costSensitivity, UUID userId) {
        try {
            logger.debug("Selecting model for request type: {}, complexity: {}, urgency: {}, cost sensitivity: {}", 
                requestType, complexity, urgency, costSensitivity);
            
            // Get available models for this request type
            List<String> preferredModels = requestTypeModelPreferences.getOrDefault(requestType, 
                Arrays.asList(deepSeekConfig.getDefaultModel()));
            
            // Filter available models
            List<String> availableModels = preferredModels.stream()
                .filter(deepSeekConfig::isModelAvailable)
                .toList();
            
            if (availableModels.isEmpty()) {
                return createFallbackSelection(requestType, "No preferred models available");
            }
            
            // Score each available model
            List<ModelScore> modelScores = new ArrayList<>();
            for (String modelName : availableModels) {
                ModelScore score = calculateModelScore(modelName, requestType, complexity, 
                    urgency, costSensitivity, userId);
                modelScores.add(score);
            }
            
            // Sort by score (highest first)
            modelScores.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
            
            ModelScore bestModel = modelScores.get(0);
            
            // Create selection result
            ModelSelectionResult result = new ModelSelectionResult();
            result.setSelectedModel(bestModel.getModelName());
            result.setScore(bestModel.getTotalScore());
            result.setReasoning(bestModel.getReasoning());
            result.setAlternatives(modelScores.subList(1, Math.min(3, modelScores.size()))
                .stream().map(ModelScore::getModelName).toList());
            result.setTimestamp(LocalDateTime.now());
            result.setRequestType(requestType);
            result.setComplexity(complexity);
            result.setUrgency(urgency);
            result.setCostSensitivity(costSensitivity);
            
            // Update model metrics
            updateModelMetrics(bestModel.getModelName(), requestType);
            
            logger.info("Selected model: {} (score: {}) for {} request", 
                bestModel.getModelName(), bestModel.getTotalScore(), requestType);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error selecting optimal model: {}", e.getMessage(), e);
            return createFallbackSelection(requestType, "Error in model selection: " + e.getMessage());
        }
    }
    
    /**
     * Calculate score for a specific model
     */
    private ModelScore calculateModelScore(String modelName, String requestType, String complexity, 
                                         String urgency, String costSensitivity, UUID userId) {
        DeepSeekConfig.ModelConfig modelConfig = deepSeekConfig.getModelConfig(modelName);
        ModelPerformanceMetrics metrics = modelMetrics.computeIfAbsent(modelName, 
            k -> new ModelPerformanceMetrics());
        
        double performanceScore = 0.0;
        double costScore = 0.0;
        double availabilityScore = 0.0;
        double contextualScore = 0.0;
        
        // Performance score (0-40 points)
        performanceScore = calculatePerformanceScore(modelConfig, metrics, complexity, urgency);
        
        // Cost score (0-30 points)
        costScore = calculateCostScore(modelConfig, costSensitivity);
        
        // Availability score (0-20 points)
        availabilityScore = calculateAvailabilityScore(modelName, metrics);
        
        // Contextual score (0-10 points)
        contextualScore = calculateContextualScore(modelName, requestType, userId);
        
        double totalScore = performanceScore + costScore + availabilityScore + contextualScore;
        
        String reasoning = String.format(
            "Performance: %.1f, Cost: %.1f, Availability: %.1f, Context: %.1f (Total: %.1f)",
            performanceScore, costScore, availabilityScore, contextualScore, totalScore
        );
        
        return new ModelScore(modelName, totalScore, reasoning);
    }
    
    /**
     * Calculate performance score based on model capabilities and metrics
     */
    private double calculatePerformanceScore(DeepSeekConfig.ModelConfig modelConfig, 
                                           ModelPerformanceMetrics metrics, String complexity, String urgency) {
        double baseScore = modelConfig.getPerformanceScore() * 20; // Base performance (0-20)
        
        // Adjust for complexity
        double complexityMultiplier = switch (complexity.toLowerCase()) {
            case "high" -> 1.2; // Favor high-performance models
            case "medium" -> 1.0;
            case "low" -> 0.8; // Can use simpler models
            default -> 1.0;
        };
        
        // Adjust for urgency
        double urgencyMultiplier = switch (urgency.toLowerCase()) {
            case "high" -> 1.1; // Slightly favor faster models
            case "medium" -> 1.0;
            case "low" -> 0.9; // Can use slower but better models
            default -> 1.0;
        };
        
        // Adjust for historical performance
        double historicalMultiplier = calculateHistoricalPerformanceMultiplier(metrics);
        
        return baseScore * complexityMultiplier * urgencyMultiplier * historicalMultiplier;
    }
    
    /**
     * Calculate cost score based on model pricing and cost sensitivity
     */
    private double calculateCostScore(DeepSeekConfig.ModelConfig modelConfig, String costSensitivity) {
        // Lower cost = higher score
        double avgCost = (modelConfig.getInputCostPer1M() + modelConfig.getOutputCostPer1M()) / 2;
        double baseScore = Math.max(0, 30 - (avgCost * 10)); // Scale cost to 0-30 range
        
        // Adjust for cost sensitivity
        double sensitivityMultiplier = switch (costSensitivity.toLowerCase()) {
            case "high" -> 1.3; // Strongly favor cheaper models
            case "medium" -> 1.0;
            case "low" -> 0.7; // Less emphasis on cost
            default -> 1.0;
        };
        
        return baseScore * sensitivityMultiplier;
    }
    
    /**
     * Calculate availability score based on model health and usage
     */
    private double calculateAvailabilityScore(String modelName, ModelPerformanceMetrics metrics) {
        double healthScore = metrics.getHealthScore() * 10; // Health (0-10)
        double loadScore = Math.max(0, 10 - metrics.getCurrentLoad() * 10); // Load (0-10)
        
        return healthScore + loadScore;
    }
    
    /**
     * Calculate contextual score based on request type and user preferences
     */
    private double calculateContextualScore(String modelName, String requestType, UUID userId) {
        double score = 5.0; // Base contextual score
        
        // Adjust for request type preferences
        List<String> preferredModels = requestTypeModelPreferences.get(requestType);
        if (preferredModels != null && preferredModels.contains(modelName)) {
            score += 3.0; // Bonus for preferred model
        }
        
        // Adjust for off-peak routing
        if (offPeakRoutingService.isOffPeakActive()) {
            if ("deepseek-reasoner".equals(modelName)) {
                score += 2.0; // Bonus for reasoner during off-peak
            }
        }
        
        return Math.min(score, 10.0); // Cap at 10
    }
    
    /**
     * Calculate historical performance multiplier
     */
    private double calculateHistoricalPerformanceMultiplier(ModelPerformanceMetrics metrics) {
        if (metrics.getRequestCount() == 0) {
            return 1.0; // No history, neutral multiplier
        }
        
        double successRate = metrics.getSuccessRate();
        double avgResponseTime = metrics.getAverageResponseTime();
        
        // Success rate multiplier (0.8 to 1.2)
        double successMultiplier = 0.8 + (successRate * 0.4);
        
        // Response time multiplier (0.9 to 1.1)
        double timeMultiplier = avgResponseTime < 2000 ? 1.1 : // Fast responses get bonus
                              avgResponseTime > 5000 ? 0.9 : 1.0; // Slow responses get penalty
        
        return successMultiplier * timeMultiplier;
    }
    
    /**
     * Update model metrics after a request
     */
    private void updateModelMetrics(String modelName, String requestType) {
        ModelPerformanceMetrics metrics = modelMetrics.computeIfAbsent(modelName, 
            k -> new ModelPerformanceMetrics());
        
        metrics.incrementRequestCount();
        metrics.setLastUsed(LocalDateTime.now());
        metrics.setLastRequestType(requestType);
    }
    
    /**
     * Record model performance after request completion
     */
    public void recordModelPerformance(String modelName, boolean success, long responseTimeMs, 
                                      double cost, String requestType) {
        try {
            ModelPerformanceMetrics metrics = modelMetrics.computeIfAbsent(modelName, 
                k -> new ModelPerformanceMetrics());
            
            metrics.recordRequest(success, responseTimeMs, cost);
            
            logger.debug("Recorded performance for {}: success={}, time={}ms, cost=${}", 
                modelName, success, responseTimeMs, cost);
            
        } catch (Exception e) {
            logger.error("Error recording model performance: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get model recommendations for a request type
     */
    public List<ModelRecommendation> getModelRecommendations(String requestType) {
        try {
            List<String> preferredModels = requestTypeModelPreferences.getOrDefault(requestType, 
                Arrays.asList(deepSeekConfig.getDefaultModel()));
            
            List<ModelRecommendation> recommendations = new ArrayList<>();
            
            for (String modelName : preferredModels) {
                if (deepSeekConfig.isModelAvailable(modelName)) {
                    DeepSeekConfig.ModelConfig config = deepSeekConfig.getModelConfig(modelName);
                    ModelPerformanceMetrics metrics = modelMetrics.getOrDefault(modelName, 
                        new ModelPerformanceMetrics());
                    
                    ModelRecommendation recommendation = new ModelRecommendation();
                    recommendation.setModelName(modelName);
                    recommendation.setPerformanceScore(config.getPerformanceScore());
                    recommendation.setCostPer1M((config.getInputCostPer1M() + config.getOutputCostPer1M()) / 2);
                    recommendation.setSuccessRate(metrics.getSuccessRate());
                    recommendation.setAverageResponseTime(metrics.getAverageResponseTime());
                    recommendation.setRequestCount(metrics.getRequestCount());
                    
                    recommendations.add(recommendation);
                }
            }
            
            return recommendations;
            
        } catch (Exception e) {
            logger.error("Error getting model recommendations: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Create fallback selection
     */
    private ModelSelectionResult createFallbackSelection(String requestType, String reason) {
        ModelSelectionResult result = new ModelSelectionResult();
        result.setSelectedModel(deepSeekConfig.getFallbackModel());
        result.setScore(0.0);
        result.setReasoning("Fallback selection: " + reason);
        result.setAlternatives(List.of());
        result.setTimestamp(LocalDateTime.now());
        result.setRequestType(requestType);
        result.setComplexity("medium");
        result.setUrgency("medium");
        result.setCostSensitivity("medium");
        
        return result;
    }
    
    /**
     * Get model performance statistics
     */
    public Map<String, ModelPerformanceMetrics> getModelPerformanceStatistics() {
        return Map.copyOf(modelMetrics);
    }
    
    /**
     * Model score for selection
     */
    public static class ModelScore {
        private final String modelName;
        private final double totalScore;
        private final String reasoning;
        
        public ModelScore(String modelName, double totalScore, String reasoning) {
            this.modelName = modelName;
            this.totalScore = totalScore;
            this.reasoning = reasoning;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public double getTotalScore() { return totalScore; }
        public String getReasoning() { return reasoning; }
    }
    
    /**
     * Model selection result
     */
    public static class ModelSelectionResult {
        private String selectedModel;
        private double score;
        private String reasoning;
        private List<String> alternatives;
        private LocalDateTime timestamp;
        private String requestType;
        private String complexity;
        private String urgency;
        private String costSensitivity;
        
        // Getters and setters
        public String getSelectedModel() { return selectedModel; }
        public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }
        
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        
        public List<String> getAlternatives() { return alternatives; }
        public void setAlternatives(List<String> alternatives) { this.alternatives = alternatives; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
        
        public String getComplexity() { return complexity; }
        public void setComplexity(String complexity) { this.complexity = complexity; }
        
        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }
        
        public String getCostSensitivity() { return costSensitivity; }
        public void setCostSensitivity(String costSensitivity) { this.costSensitivity = costSensitivity; }
    }
    
    /**
     * Model performance metrics
     */
    public static class ModelPerformanceMetrics {
        private int requestCount = 0;
        private int successCount = 0;
        private long totalResponseTime = 0;
        private double totalCost = 0.0;
        private LocalDateTime lastUsed;
        private String lastRequestType;
        private double healthScore = 1.0; // 0.0 to 1.0
        private double currentLoad = 0.0; // 0.0 to 1.0
        
        public void incrementRequestCount() { requestCount++; }
        
        public void recordRequest(boolean success, long responseTimeMs, double cost) {
            requestCount++;
            if (success) successCount++;
            totalResponseTime += responseTimeMs;
            totalCost += cost;
        }
        
        public double getSuccessRate() {
            return requestCount > 0 ? (double) successCount / requestCount : 0.0;
        }
        
        public double getAverageResponseTime() {
            return requestCount > 0 ? (double) totalResponseTime / requestCount : 0.0;
        }
        
        public double getAverageCost() {
            return requestCount > 0 ? totalCost / requestCount : 0.0;
        }
        
        // Getters and setters
        public int getRequestCount() { return requestCount; }
        public int getSuccessCount() { return successCount; }
        public long getTotalResponseTime() { return totalResponseTime; }
        public double getTotalCost() { return totalCost; }
        public LocalDateTime getLastUsed() { return lastUsed; }
        public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
        public String getLastRequestType() { return lastRequestType; }
        public void setLastRequestType(String lastRequestType) { this.lastRequestType = lastRequestType; }
        public double getHealthScore() { return healthScore; }
        public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
        public double getCurrentLoad() { return currentLoad; }
        public void setCurrentLoad(double currentLoad) { this.currentLoad = currentLoad; }
    }
    
    /**
     * Model recommendation
     */
    public static class ModelRecommendation {
        private String modelName;
        private double performanceScore;
        private double costPer1M;
        private double successRate;
        private double averageResponseTime;
        private int requestCount;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public double getPerformanceScore() { return performanceScore; }
        public void setPerformanceScore(double performanceScore) { this.performanceScore = performanceScore; }
        
        public double getCostPer1M() { return costPer1M; }
        public void setCostPer1M(double costPer1M) { this.costPer1M = costPer1M; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public int getRequestCount() { return requestCount; }
        public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
    }
}
