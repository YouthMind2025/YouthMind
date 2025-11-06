package ai.youthmind.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.youthmind.config.DeepSeekConfig;

/**
 * Service for generating psychological vectors using DeepSeek AI
 * Provides AI-powered analysis of text content for 43-dimensional Buddhist psychological factors
 */
@Service
public class DeepSeekVectorScoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekVectorScoringService.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate 43-dimensional psychological vector using Deep Seek AI
     * 
     * @param content Text content to analyze
     * @param userId User ID for context
     * @return List of 43 scores representing Buddhist psychological factors
     */
    public List<Double> generatePsychologicalScores(String content, UUID userId) {
        try {
            logger.info("Generating psychological scores using DeepSeek AI for user {}", userId);
            
            // Create prompt for DeepSeek
            String systemPrompt = createSystemPrompt();
            String userPrompt = createUserPrompt(content);
            
            // Call DeepSeek API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekConfig.getModel());
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.1); // Low temperature for consistent scoring
            requestBody.put("max_tokens", 1000);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                deepSeekConfig.getBaseUrl() + "/chat/completions",
                HttpMethod.POST,
                entity,
                String.class
            );
            
            // Parse response
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String aiResponse = (String) message.get("content");
                
                // Log the raw response for debugging
                logger.info("Raw DeepSeek AI response: {}", aiResponse);
                
                // Parse scores from AI response
                List<Double> scores = parseScoresFromResponse(aiResponse);
                
                if (scores != null) {
                    logger.info("Successfully generated {} psychological scores using DeepSeek AI", scores.size());
                    // Log first 5 scores for verification
                    logger.info("First 5 scores: {}", scores.subList(0, Math.min(5, scores.size())));
                    return scores;
                } else {
                    logger.error("Failed to parse scores from DeepSeek response");
                    return null;
                }
            }
            
            logger.warn("DeepSeek API returned empty response, using fallback");
            return null;
            
        } catch (Exception e) {
            logger.error("Error calling DeepSeek API for psychological scoring: {}", e.getMessage());
            return null; // Return null to trigger fallback to keyword-based
        }
    }
    
    /**
     * Create system prompt for psychological analysis
     */
    private String createSystemPrompt() {
        return """
            You are a psychological analysis AI trained in Buddhist psychology and mental health.
            Your task is to analyze text content and score it on EXACTLY 43 Buddhist mental factors.
            
            CRITICAL: You MUST return EXACTLY 43 scores in this exact order:
            
            POSITIVE FACTORS (indices 0-15, total 16 factors):
            0. Attention, 1. Contact, 2. Feeling, 3. Thinking, 4. Deliberation,
            5. Faith, 6. Vigor, 7. Inner shame, 8. Shame, 9. Absence of craving,
            10. Absence of hatred, 11. Absence of ignorance, 12. Light ease,
            13. Non-laxness, 14. Renunciation, 15. Non-harming
            
            NEGATIVE FACTORS (indices 16-42, total 27 factors):
            16. Greed, 17. Anger, 18. Arrogance, 19. Foolishness, 20. Doubt,
            21. Improper views, 22. Wrath, 23. Hatred, 24. Rage, 25. Covering,
            26. Deceit, 27. Flattery, 28. Conceit, 29. Harming, 30. Jealousy,
            31. Stinginess, 32. Lack of remorse, 33. Lack of shame, 34. Lack of faith,
            35. Laziness, 36. Laxness, 37. Torpor, 38. Restlessness, 39. Distraction,
            40. Improper knowledge, 41. Scatteredness, 42. Regret
            
            TOTAL: 16 positive + 27 negative = 43 factors (indices 0-42)
            
            For each text, provide a score between 0.0 and 1.0 for each factor representing its intensity.
            Focus on implicit emotions, context, and underlying psychological states.
            
            CRITICAL: Respond with ONLY a JSON array of EXACTLY 43 numbers, nothing else.
            Example format: [0.2, 0.1, 0.5, 0.3, 0.4, 0.6, 0.1, 0.2, 0.3, 0.5, 0.1, 0.2, 0.4, 0.3, 0.5, 0.1, 0.2, 0.1, 0.3, 0.2, 0.1, 0.2, 0.1, 0.1, 0.2, 0.1, 0.2, 0.1, 0.1, 0.2, 0.1, 0.2, 0.1, 0.2, 0.1, 0.2, 0.1, 0.3, 0.2, 0.1, 0.2, 0.1, 0.2]
            """;
    }
    
    /**
     * Create user prompt with content to analyze
     */
    private String createUserPrompt(String content) {
        return String.format("""
            Analyze this text and provide psychological factor scores:
            
            "%s"
            
            CRITICAL: Return ONLY a JSON array of EXACTLY 43 scores [0.0-1.0], one for each of the 43 factors listed above.
            Your response must be a valid JSON array with exactly 43 numbers.
            """, content);
    }
    
    /**
     * Parse psychological scores from AI response
     */
    private List<Double> parseScoresFromResponse(String response) {
        try {
            // Try to find JSON array in response
            String jsonArray = response.trim();
            
            // Remove markdown code blocks if present
            jsonArray = jsonArray.replaceAll("```json\\s*", "");
            jsonArray = jsonArray.replaceAll("```\\s*", "");
            jsonArray = jsonArray.trim();
            
            // Parse JSON array
            if (jsonArray.startsWith("[") && jsonArray.endsWith("]")) {
                // Parse as List of Objects first to handle both integers and doubles
                List<?> rawScores = objectMapper.readValue(jsonArray, List.class);
                
                // Convert all to Double
                List<Double> scores = new ArrayList<>();
                for (Object score : rawScores) {
                    if (score instanceof Number) {
                        scores.add(((Number) score).doubleValue());
                    } else {
                        logger.warn("Non-numeric score found: {}", score);
                        scores.add(0.0);
                    }
                }
                
                // Validate we have 43 scores (or handle 42 with smart fallback)
                if (scores.size() == 43) {
                    // Ensure all scores are in [0.0, 1.0] range
                    for (int i = 0; i < scores.size(); i++) {
                        double score = scores.get(i);
                        if (score < 0.0) scores.set(i, 0.0);
                        if (score > 1.0) scores.set(i, 1.0);
                    }
                    
                    // Log statistics for debugging
                    long nonZeroCount = scores.stream().filter(s -> s > 0.0).count();
                    double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    logger.info("Parsed scores - Non-zero: {}/43, Mean: {:.3f}", nonZeroCount, mean);
                    
                    return scores;
                } else if (scores.size() == 42) {
                    // Common issue: DeepSeek returns 42 instead of 43
                    // Add a default score for the missing factor (last one: Regret)
                    logger.warn("DeepSeek returned 42 scores, adding default 0.1 for missing factor 42 (Regret)");
                    scores.add(0.1);
                    
                    // Ensure all scores are in [0.0, 1.0] range
                    for (int i = 0; i < scores.size(); i++) {
                        double score = scores.get(i);
                        if (score < 0.0) scores.set(i, 0.0);
                        if (score > 1.0) scores.set(i, 1.0);
                    }
                    
                    // Log statistics
                    long nonZeroCount = scores.stream().filter(s -> s > 0.0).count();
                    double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    logger.info("Parsed scores (with fallback) - Non-zero: {}/43, Mean: {:.3f}", nonZeroCount, mean);
                    
                    return scores;
                } else {
                    logger.warn("DeepSeek returned {} scores instead of 43 - cannot recover", scores.size());
                }
            }
            
            logger.warn("Failed to parse scores from DeepSeek response: {}", response);
            return null;
            
        } catch (Exception e) {
            logger.error("Error parsing DeepSeek response: {}", e.getMessage());
            return null;
        }
    }
}


