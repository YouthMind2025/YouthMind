package ai.youthmind.service;

import ai.youthmind.entity.AiApiUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for calculating AI API costs based on different models and usage patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiCostCalculationService {

    // DeepSeek pricing (as of 2024) - in USD per 1K tokens
    private static final Map<String, BigDecimal> MODEL_PRICING = new HashMap<>();
    
    static {
        // DeepSeek pricing
        MODEL_PRICING.put("deepseek-chat", new BigDecimal("0.00014")); // $0.14 per 1M tokens
        MODEL_PRICING.put("deepseek-coder", new BigDecimal("0.00014")); // $0.14 per 1M tokens
        
        // OpenAI pricing for comparison
        MODEL_PRICING.put("gpt-4", new BigDecimal("0.03")); // $30 per 1M tokens
        MODEL_PRICING.put("gpt-4-turbo", new BigDecimal("0.01")); // $10 per 1M tokens
        MODEL_PRICING.put("gpt-3.5-turbo", new BigDecimal("0.001")); // $1 per 1M tokens
        
        // Anthropic pricing for comparison
        MODEL_PRICING.put("claude-3-opus", new BigDecimal("0.015")); // $15 per 1M tokens
        MODEL_PRICING.put("claude-3-sonnet", new BigDecimal("0.003")); // $3 per 1M tokens
        MODEL_PRICING.put("claude-3-haiku", new BigDecimal("0.00025")); // $0.25 per 1M tokens
    }

    /**
     * Calculate cost for a specific model and token usage.
     */
    public BigDecimal calculateCost(String modelName, int promptTokens, int completionTokens) {
        log.debug("Calculating cost for model: {}, prompt tokens: {}, completion tokens: {}", 
                modelName, promptTokens, completionTokens);

        BigDecimal costPerThousandTokens = MODEL_PRICING.getOrDefault(modelName, getDefaultPricing());
        int totalTokens = promptTokens + completionTokens;
        
        // Convert to cost per token (divide by 1000)
        BigDecimal costPerToken = costPerThousandTokens.divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP);
        
        // Calculate total cost
        BigDecimal totalCost = costPerToken.multiply(new BigDecimal(totalTokens));
        
        log.debug("Calculated cost: {} USD for {} tokens using model {}", totalCost, totalTokens, modelName);
        
        return totalCost;
    }

    /**
     * Calculate cost per token for a specific model.
     */
    public BigDecimal getCostPerToken(String modelName) {
        BigDecimal costPerThousandTokens = MODEL_PRICING.getOrDefault(modelName, getDefaultPricing());
        return costPerThousandTokens.divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP);
    }

    /**
     * Get pricing information for all supported models.
     */
    public Map<String, BigDecimal> getAllModelPricing() {
        return new HashMap<>(MODEL_PRICING);
    }

    /**
     * Get pricing information for a specific model.
     */
    public BigDecimal getModelPricing(String modelName) {
        return MODEL_PRICING.getOrDefault(modelName, getDefaultPricing());
    }

    /**
     * Calculate monthly cost estimate based on usage patterns.
     */
    public BigDecimal calculateMonthlyEstimate(String modelName, int averageRequestsPerDay, 
                                             int averageTokensPerRequest) {
        log.debug("Calculating monthly estimate for model: {}, requests/day: {}, tokens/request: {}", 
                modelName, averageRequestsPerDay, averageTokensPerRequest);

        BigDecimal costPerToken = getCostPerToken(modelName);
        BigDecimal dailyCost = costPerToken.multiply(new BigDecimal(averageTokensPerRequest))
                                          .multiply(new BigDecimal(averageRequestsPerDay));
        BigDecimal monthlyCost = dailyCost.multiply(new BigDecimal("30")); // 30 days

        log.debug("Monthly cost estimate: {} USD", monthlyCost);
        
        return monthlyCost;
    }

    /**
     * Calculate cost savings by switching models.
     */
    public Map<String, BigDecimal> calculateCostSavings(String currentModel, String targetModel, 
                                                      int monthlyTokens) {
        log.debug("Calculating cost savings from {} to {} for {} monthly tokens", 
                currentModel, targetModel, monthlyTokens);

        BigDecimal currentCost = getCostPerToken(currentModel).multiply(new BigDecimal(monthlyTokens));
        BigDecimal targetCost = getCostPerToken(targetModel).multiply(new BigDecimal(monthlyTokens));
        BigDecimal savings = currentCost.subtract(targetCost);
        BigDecimal savingsPercentage = currentCost.compareTo(BigDecimal.ZERO) > 0 
                ? savings.divide(currentCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("currentCost", currentCost);
        result.put("targetCost", targetCost);
        result.put("savings", savings);
        result.put("savingsPercentage", savingsPercentage);

        log.debug("Cost savings: {} USD ({}%)", savings, savingsPercentage);

        return result;
    }

    /**
     * Get cost breakdown for different request types.
     */
    public Map<String, BigDecimal> getCostBreakdownByRequestType() {
        Map<String, BigDecimal> breakdown = new HashMap<>();
        
        // Typical token usage by request type
        Map<AiApiUsage.RequestType, Integer> typicalTokens = new HashMap<>();
        typicalTokens.put(AiApiUsage.RequestType.MODERATION, 100);
        typicalTokens.put(AiApiUsage.RequestType.CONTENT_GENERATION, 500);
        typicalTokens.put(AiApiUsage.RequestType.SENTIMENT_ANALYSIS, 50);
        typicalTokens.put(AiApiUsage.RequestType.LANGUAGE_DETECTION, 30);
        typicalTokens.put(AiApiUsage.RequestType.TEXT_SUMMARIZATION, 200);
        typicalTokens.put(AiApiUsage.RequestType.TRANSLATION, 300);
        typicalTokens.put(AiApiUsage.RequestType.OTHER, 100);

        // Calculate costs using DeepSeek pricing
        BigDecimal deepSeekCostPerToken = getCostPerToken("deepseek-chat");
        
        for (Map.Entry<AiApiUsage.RequestType, Integer> entry : typicalTokens.entrySet()) {
            AiApiUsage.RequestType requestType = entry.getKey();
            Integer tokens = entry.getValue();
            BigDecimal cost = deepSeekCostPerToken.multiply(new BigDecimal(tokens));
            breakdown.put(requestType.toString(), cost);
        }

        return breakdown;
    }

    /**
     * Validate if a cost is within acceptable limits.
     */
    public boolean isCostWithinLimits(BigDecimal cost, BigDecimal maxCostPerRequest) {
        return cost.compareTo(maxCostPerRequest) <= 0;
    }

    /**
     * Get cost efficiency metrics.
     */
    public Map<String, Object> getCostEfficiencyMetrics(String modelName, int totalRequests, 
                                                        BigDecimal totalCost, long totalTokens) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (totalRequests > 0) {
            BigDecimal avgCostPerRequest = totalCost.divide(new BigDecimal(totalRequests), 6, RoundingMode.HALF_UP);
            metrics.put("avgCostPerRequest", avgCostPerRequest);
        }
        
        if (totalTokens > 0) {
            BigDecimal avgCostPerToken = totalCost.divide(new BigDecimal(totalTokens), 8, RoundingMode.HALF_UP);
            metrics.put("avgCostPerToken", avgCostPerToken);
        }
        
        BigDecimal expectedCost = getCostPerToken(modelName).multiply(new BigDecimal(totalTokens));
        BigDecimal costEfficiency = totalCost.compareTo(BigDecimal.ZERO) > 0 
                ? expectedCost.divide(totalCost, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        metrics.put("costEfficiency", costEfficiency);
        
        return metrics;
    }

    /**
     * Get default pricing for unknown models.
     */
    private BigDecimal getDefaultPricing() {
        // Default to DeepSeek pricing for unknown models
        return new BigDecimal("0.00014");
    }

    /**
     * Update model pricing (for dynamic pricing updates).
     */
    public void updateModelPricing(String modelName, BigDecimal newPricing) {
        log.info("Updating pricing for model {} to {} per 1K tokens", modelName, newPricing);
        MODEL_PRICING.put(modelName, newPricing);
    }

    /**
     * Add new model pricing.
     */
    public void addModelPricing(String modelName, BigDecimal pricing) {
        log.info("Adding pricing for new model {}: {} per 1K tokens", modelName, pricing);
        MODEL_PRICING.put(modelName, pricing);
    }
}
