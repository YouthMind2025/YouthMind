package ai.youthmind.util;

import ai.youthmind.dto.ModerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing AI moderation responses
 * Handles various response formats and extracts structured data
 */
public class ModerationResponseParser {
    
    private static final Logger logger = LoggerFactory.getLogger(ModerationResponseParser.class);
    
    // Primary patterns for parsing responses
    private static final Pattern APPROVE_PATTERN = Pattern.compile("APPROVE:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REJECT_PATTERN = Pattern.compile("REJECT:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    
    // Alternative patterns for different response formats
    private static final Pattern SAFE_PATTERN = Pattern.compile("(SAFE|CLEAN|OK|GOOD):\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_PATTERN = Pattern.compile("(UNSAFE|UNSAFE|BAD|PROBLEMATIC):\\s*(.+)", Pattern.CASE_INSENSITIVE);
    
    // Confidence score patterns
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("confidence[\\s:]+([0-9.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCORE_PATTERN = Pattern.compile("score[\\s:]+([0-9.]+)", Pattern.CASE_INSENSITIVE);
    
    // Severity patterns
    private static final Pattern SEVERITY_PATTERN = Pattern.compile("severity[\\s:]+(LOW|MEDIUM|HIGH|CRITICAL)", Pattern.CASE_INSENSITIVE);
    
    // Category patterns
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("(harassment|hate_speech|violence|spam|self_harm|inappropriate|offensive)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Parse AI response into structured ModerationResult
     */
    public static ModerationResult parseResponse(String response, String originalContent) {
        if (response == null || response.trim().isEmpty()) {
            logger.warn("Empty response from AI service");
            return ModerationResult.safe("Empty AI response");
        }
        
        logger.debug("Parsing AI response: {}", response);
        
        // Clean the response
        String cleanResponse = response.trim();
        
        // Try primary patterns first
        if (APPROVE_PATTERN.matcher(cleanResponse).find()) {
            return parseApprovalResponse(cleanResponse, originalContent);
        } else if (REJECT_PATTERN.matcher(cleanResponse).find()) {
            return parseRejectionResponse(cleanResponse, originalContent);
        }
        
        // Try alternative patterns
        if (SAFE_PATTERN.matcher(cleanResponse).find()) {
            return parseSafeResponse(cleanResponse, originalContent);
        } else if (UNSAFE_PATTERN.matcher(cleanResponse).find()) {
            return parseUnsafeResponse(cleanResponse, originalContent);
        }
        
        // Try to extract decision from unstructured response
        return parseUnstructuredResponse(cleanResponse, originalContent);
    }
    
    /**
     * Parse approval response
     */
    private static ModerationResult parseApprovalResponse(String response, String originalContent) {
        String reason = extractReason(response, APPROVE_PATTERN);
        double confidence = extractConfidence(response);
        
        ModerationResult result = ModerationResult.safe(reason);
        result.setConfidence(confidence);
        result.setRawResponse(response);
        result.setSeverity(ModerationResult.SeverityLevel.LOW);
        result.setDetectedCategories(new String[0]);
        
        logger.debug("Parsed approval: reason={}, confidence={}", reason, confidence);
        return result;
    }
    
    /**
     * Parse rejection response
     */
    private static ModerationResult parseRejectionResponse(String response, String originalContent) {
        String reason = extractReason(response, REJECT_PATTERN);
        double confidence = extractConfidence(response);
        ModerationResult.SeverityLevel severity = extractSeverity(response, originalContent);
        String[] categories = extractCategories(response, originalContent);
        
        ModerationResult result = ModerationResult.unsafe(reason, severity, categories);
        result.setConfidence(confidence);
        result.setRawResponse(response);
        
        logger.debug("Parsed rejection: reason={}, confidence={}, severity={}, categories={}", 
            reason, confidence, severity, Arrays.toString(categories));
        return result;
    }
    
    /**
     * Parse safe response (alternative format)
     */
    private static ModerationResult parseSafeResponse(String response, String originalContent) {
        String reason = extractReason(response, SAFE_PATTERN);
        double confidence = extractConfidence(response);
        
        ModerationResult result = ModerationResult.safe(reason);
        result.setConfidence(confidence);
        result.setRawResponse(response);
        result.setSeverity(ModerationResult.SeverityLevel.LOW);
        result.setDetectedCategories(new String[0]);
        
        logger.debug("Parsed safe response: reason={}, confidence={}", reason, confidence);
        return result;
    }
    
    /**
     * Parse unsafe response (alternative format)
     */
    private static ModerationResult parseUnsafeResponse(String response, String originalContent) {
        String reason = extractReason(response, UNSAFE_PATTERN);
        double confidence = extractConfidence(response);
        ModerationResult.SeverityLevel severity = extractSeverity(response, originalContent);
        String[] categories = extractCategories(response, originalContent);
        
        ModerationResult result = ModerationResult.unsafe(reason, severity, categories);
        result.setConfidence(confidence);
        result.setRawResponse(response);
        
        logger.debug("Parsed unsafe response: reason={}, confidence={}, severity={}, categories={}", 
            reason, confidence, severity, Arrays.toString(categories));
        return result;
    }
    
    /**
     * Parse unstructured response by analyzing content
     */
    private static ModerationResult parseUnstructuredResponse(String response, String originalContent) {
        logger.warn("Parsing unstructured response: {}", response);
        
        // Analyze response for keywords
        String lowerResponse = response.toLowerCase();
        
        // Check for positive indicators
        List<String> positiveKeywords = Arrays.asList("safe", "clean", "ok", "good", "fine", "acceptable", "appropriate");
        boolean isPositive = positiveKeywords.stream().anyMatch(lowerResponse::contains);
        
        // Check for negative indicators
        List<String> negativeKeywords = Arrays.asList("unsafe", "bad", "problematic", "inappropriate", "offensive", "harmful", "dangerous");
        boolean isNegative = negativeKeywords.stream().anyMatch(lowerResponse::contains);
        
        if (isPositive && !isNegative) {
            ModerationResult result = ModerationResult.safe("Content appears safe based on AI analysis");
            result.setConfidence(0.7); // Lower confidence due to unstructured response
            result.setRawResponse(response);
            return result;
        } else if (isNegative && !isPositive) {
            ModerationResult.SeverityLevel severity = extractSeverity(response, originalContent);
            String[] categories = extractCategories(response, originalContent);
            ModerationResult result = ModerationResult.unsafe("Content flagged as potentially harmful", severity, categories);
            result.setConfidence(0.7); // Lower confidence due to unstructured response
            result.setRawResponse(response);
            return result;
        } else {
            // Ambiguous response - default to safe with low confidence
            ModerationResult result = ModerationResult.safe("Ambiguous AI response - manual review recommended");
            result.setConfidence(0.3); // Very low confidence
            result.setRawResponse(response);
            return result;
        }
    }
    
    /**
     * Extract reason from response using pattern
     */
    private static String extractReason(String response, Pattern pattern) {
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String reason = matcher.group(1).trim();
            // Clean up the reason
            reason = reason.replaceAll("\\s+", " ");
            return reason.isEmpty() ? "No specific reason provided" : reason;
        }
        return "No specific reason provided";
    }
    
    /**
     * Extract confidence score from response
     */
    private static double extractConfidence(String response) {
        // Try confidence pattern first
        Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(response);
        if (confidenceMatcher.find()) {
            try {
                return Double.parseDouble(confidenceMatcher.group(1));
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse confidence score: {}", confidenceMatcher.group(1));
            }
        }
        
        // Try score pattern
        Matcher scoreMatcher = SCORE_PATTERN.matcher(response);
        if (scoreMatcher.find()) {
            try {
                return Double.parseDouble(scoreMatcher.group(1));
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse score: {}", scoreMatcher.group(1));
            }
        }
        
        // Default confidence based on response structure
        if (response.contains("APPROVE") || response.contains("REJECT")) {
            return 0.9; // High confidence for structured responses
        } else if (response.contains("SAFE") || response.contains("UNSAFE")) {
            return 0.8; // Good confidence for alternative structured responses
        } else {
            return 0.6; // Lower confidence for unstructured responses
        }
    }
    
    /**
     * Extract severity level from response and content
     */
    private static ModerationResult.SeverityLevel extractSeverity(String response, String originalContent) {
        String lowerResponse = response.toLowerCase();
        String lowerContent = originalContent.toLowerCase();
        
        // Check explicit severity in response
        Matcher severityMatcher = SEVERITY_PATTERN.matcher(response);
        if (severityMatcher.find()) {
            String severity = severityMatcher.group(1).toUpperCase();
            try {
                return ModerationResult.SeverityLevel.valueOf(severity);
            } catch (IllegalArgumentException e) {
                logger.debug("Invalid severity level: {}", severity);
            }
        }
        
        // Determine severity based on keywords
        if (lowerResponse.contains("critical") || lowerResponse.contains("severe") || 
            lowerResponse.contains("self-harm") || lowerResponse.contains("suicide") ||
            lowerResponse.contains("violence") || lowerResponse.contains("illegal")) {
            return ModerationResult.SeverityLevel.CRITICAL;
        }
        
        if (lowerResponse.contains("high") || lowerResponse.contains("serious") ||
            lowerResponse.contains("harassment") || lowerResponse.contains("hate") ||
            lowerResponse.contains("threat") || lowerResponse.contains("abuse")) {
            return ModerationResult.SeverityLevel.HIGH;
        }
        
        if (lowerResponse.contains("medium") || lowerResponse.contains("moderate") ||
            lowerResponse.contains("inappropriate") || lowerResponse.contains("spam") ||
            lowerResponse.contains("offensive")) {
            return ModerationResult.SeverityLevel.MEDIUM;
        }
        
        return ModerationResult.SeverityLevel.LOW;
    }
    
    /**
     * Extract content categories from response and content
     */
    private static String[] extractCategories(String response, String originalContent) {
        List<String> categories = new java.util.ArrayList<>();
        
        // Extract from response
        Matcher categoryMatcher = CATEGORY_PATTERN.matcher(response);
        while (categoryMatcher.find()) {
            String category = categoryMatcher.group(1).toLowerCase();
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }
        
        // Extract from original content if not found in response
        if (categories.isEmpty()) {
            categoryMatcher = CATEGORY_PATTERN.matcher(originalContent);
            while (categoryMatcher.find()) {
                String category = categoryMatcher.group(1).toLowerCase();
                if (!categories.contains(category)) {
                    categories.add(category);
                }
            }
        }
        
        return categories.toArray(new String[0]);
    }
    
    /**
     * Validate if response format is supported
     */
    public static boolean isSupportedFormat(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        
        String cleanResponse = response.trim();
        
        return APPROVE_PATTERN.matcher(cleanResponse).find() ||
               REJECT_PATTERN.matcher(cleanResponse).find() ||
               SAFE_PATTERN.matcher(cleanResponse).find() ||
               UNSAFE_PATTERN.matcher(cleanResponse).find() ||
               cleanResponse.toLowerCase().contains("safe") ||
               cleanResponse.toLowerCase().contains("unsafe");
    }
}
