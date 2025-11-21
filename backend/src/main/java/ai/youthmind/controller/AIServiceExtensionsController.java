package ai.youthmind.controller;

import ai.youthmind.dto.PsychologicalAnalysisRequest;
import ai.youthmind.dto.PsychologicalAnalysisResponse;
import ai.youthmind.service.AIServiceClient;
import ai.youthmind.service.AIErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for AI Service Extensions
 * Exposes endpoints for psychological analysis and error handling
 */
@RestController
@RequestMapping("/api/ai-extensions")
public class AIServiceExtensionsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AIServiceExtensionsController.class);
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private AIErrorHandler aiErrorHandler;
    
    /**
     * Perform comprehensive psychological analysis
     * 
     * @param request Psychological analysis request
     * @return Comprehensive psychological analysis response
     */
    @PostMapping("/psychological-analysis")
    public ResponseEntity<PsychologicalAnalysisResponse> performPsychologicalAnalysis(
            @RequestBody PsychologicalAnalysisRequest request) {
        logger.info("Received request for psychological analysis for user: {}", request.getUserId());
        try {
            PsychologicalAnalysisResponse response = aiServiceClient.performPsychologicalAnalysis(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error performing psychological analysis for user {}: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(request, e));
        }
    }
    
    /**
     * Get error statistics for monitoring
     * 
     * @param userId User ID (optional, null for global stats)
     * @return Error statistics
     */
    @GetMapping("/error-statistics")
    public ResponseEntity<Map<String, Object>> getErrorStatistics(
            @RequestParam(required = false) UUID userId) {
        logger.info("Received request for error statistics for user: {}", userId);
        try {
            Map<String, Object> stats = aiErrorHandler.getErrorStatistics(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting error statistics for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get error history for a user
     * 
     * @param userId User ID
     * @param limit Maximum number of errors to return
     * @return Error history
     */
    @GetMapping("/error-history/{userId}")
    public ResponseEntity<Map<String, Object>> getErrorHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "50") int limit) {
        logger.info("Received request for error history for user: {} with limit: {}", userId, limit);
        try {
            Map<String, Object> history = aiErrorHandler.getErrorHistory(userId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error getting error history for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Clear error cooldown for a user
     * 
     * @param userId User ID
     * @return Result of clearing cooldown
     */
    @PostMapping("/clear-cooldown/{userId}")
    public ResponseEntity<Map<String, Object>> clearErrorCooldown(@PathVariable UUID userId) {
        logger.info("Received request to clear error cooldown for user: {}", userId);
        try {
            Map<String, Object> result = aiErrorHandler.clearErrorCooldown(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error clearing cooldown for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test error handling system
     * 
     * @param testErrorType Type of test error to simulate
     * @param userId User ID for testing
     * @return Test result
     */
    @PostMapping("/test-error-handling")
    public ResponseEntity<Map<String, Object>> testErrorHandling(
            @RequestParam String testErrorType,
            @RequestParam UUID userId) {
        logger.info("Received request to test error handling for user: {} with error type: {}", userId, testErrorType);
        try {
            Map<String, Object> result = aiErrorHandler.testErrorHandling(testErrorType, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error testing error handling for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Create a psychological analysis request
     * 
     * @param content Content to analyze
     * @param userId User ID
     * @param analysisConfig Analysis configuration
     * @return Created psychological analysis request
     */
    @PostMapping("/create-analysis-request")
    public ResponseEntity<PsychologicalAnalysisRequest> createAnalysisRequest(
            @RequestParam String content,
            @RequestParam UUID userId,
            @RequestBody(required = false) PsychologicalAnalysisRequest.AnalysisConfig analysisConfig) {
        logger.info("Received request to create analysis request for user: {}", userId);
        try {
            PsychologicalAnalysisRequest request = createPsychologicalAnalysisRequest(content, userId, analysisConfig);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            logger.error("Error creating analysis request for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Get analysis request template
     * 
     * @return Analysis request template
     */
    @GetMapping("/analysis-request-template")
    public ResponseEntity<PsychologicalAnalysisRequest> getAnalysisRequestTemplate() {
        logger.info("Received request for analysis request template");
        try {
            PsychologicalAnalysisRequest template = createAnalysisRequestTemplate();
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            logger.error("Error creating analysis request template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    /**
     * Validate psychological analysis request
     * 
     * @param request Psychological analysis request to validate
     * @return Validation result
     */
    @PostMapping("/validate-analysis-request")
    public ResponseEntity<Map<String, Object>> validateAnalysisRequest(
            @RequestBody PsychologicalAnalysisRequest request) {
        logger.info("Received request to validate analysis request for user: {}", request.getUserId());
        try {
            Map<String, Object> validationResult = validateRequest(request);
            return ResponseEntity.ok(validationResult);
        } catch (Exception e) {
            logger.error("Error validating analysis request for user {}: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get supported analysis types
     * 
     * @return Supported analysis types
     */
    @GetMapping("/supported-analysis-types")
    public ResponseEntity<Map<String, Object>> getSupportedAnalysisTypes() {
        logger.info("Received request for supported analysis types");
        try {
            Map<String, Object> supportedTypes = getAnalysisTypes();
            return ResponseEntity.ok(supportedTypes);
        } catch (Exception e) {
            logger.error("Error getting supported analysis types: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get analysis capabilities
     * 
     * @return Analysis capabilities
     */
    @GetMapping("/analysis-capabilities")
    public ResponseEntity<Map<String, Object>> getAnalysisCapabilities() {
        logger.info("Received request for analysis capabilities");
        try {
            Map<String, Object> capabilities = getCapabilities();
            return ResponseEntity.ok(capabilities);
        } catch (Exception e) {
            logger.error("Error getting analysis capabilities: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private PsychologicalAnalysisResponse createErrorResponse(PsychologicalAnalysisRequest request, Exception error) {
        PsychologicalAnalysisResponse response = new PsychologicalAnalysisResponse();
        response.setRequestId(request.getRequestId());
        response.setUserId(request.getUserId());
        response.setSessionId(request.getSessionId());
        
        // Set error metadata
        PsychologicalAnalysisResponse.AnalysisMetadata metadata = 
            new PsychologicalAnalysisResponse.AnalysisMetadata();
        metadata.setAnalysisId(UUID.randomUUID().toString());
        metadata.setStatus("ERROR");
        metadata.setTimestamp(java.time.LocalDateTime.now());
        metadata.setTechnicalDetails(Map.of("error", error.getMessage()));
        response.setMetadata(metadata);
        
        // Set error processing info
        PsychologicalAnalysisResponse.ProcessingInfo processingInfo = 
            new PsychologicalAnalysisResponse.ProcessingInfo();
        processingInfo.setProcessingStatus("ERROR");
        processingInfo.setErrors(java.util.Arrays.asList(error.getMessage()));
        response.setProcessingInfo(processingInfo);
        
        return response;
    }
    
    private PsychologicalAnalysisRequest createPsychologicalAnalysisRequest(
            String content, 
            UUID userId, 
            PsychologicalAnalysisRequest.AnalysisConfig analysisConfig) {
        
        PsychologicalAnalysisRequest request = new PsychologicalAnalysisRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setUserId(userId);
        request.setSessionId(UUID.randomUUID().toString());
        request.setContent(content);
        request.setContentType("text");
        request.setLanguage("en");
        request.setTimestamp(java.time.LocalDateTime.now());
        request.setSource("api");
        request.setVersion("1.0.0");
        
        // Set default analysis config if not provided
        if (analysisConfig == null) {
            analysisConfig = new PsychologicalAnalysisRequest.AnalysisConfig();
            analysisConfig.setGeneratePsychologicalVector(true);
            analysisConfig.setAnalyzeBuddhistFactors(true);
            analysisConfig.setCalculateIntensityFactor(true);
            analysisConfig.setProcessStateVector(true);
            analysisConfig.setIncludeDetailedAnalysis(true);
            analysisConfig.setIncludeRecommendations(true);
            analysisConfig.setMaxAnalysisDepth(3);
            analysisConfig.setAnalysisMode("standard");
        }
        request.setAnalysisConfig(analysisConfig);
        
        // Set default user profile
        PsychologicalAnalysisRequest.UserProfile userProfile = new PsychologicalAnalysisRequest.UserProfile();
        userProfile.setAgeGroup("young_adult");
        userProfile.setGender("unknown");
        userProfile.setCulturalBackground("general");
        userProfile.setTimezone("UTC");
        userProfile.setPreferredLanguage("en");
        userProfile.setRiskLevel("low");
        request.setUserProfile(userProfile);
        
        // Set default preferences
        PsychologicalAnalysisRequest.AnalysisPreferences preferences = new PsychologicalAnalysisRequest.AnalysisPreferences();
        preferences.setIncludeEmotionalAnalysis(true);
        preferences.setIncludeCognitiveAnalysis(true);
        preferences.setIncludeSocialAnalysis(true);
        preferences.setIncludeStressIndicators(true);
        preferences.setIncludePersonalityTraits(true);
        preferences.setIncludeMentalHealthIndicators(true);
        preferences.setIncludeCopingMechanisms(true);
        preferences.setIncludeResilienceFactors(true);
        preferences.setOutputFormat("json");
        preferences.setDetailLevel("intermediate");
        request.setPreferences(preferences);
        
        return request;
    }
    
    private PsychologicalAnalysisRequest createAnalysisRequestTemplate() {
        PsychologicalAnalysisRequest template = new PsychologicalAnalysisRequest();
        template.setRequestId("template");
        template.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        template.setSessionId("template-session");
        template.setContent("Sample content for analysis");
        template.setContentType("text");
        template.setLanguage("en");
        template.setTimestamp(java.time.LocalDateTime.now());
        template.setSource("api");
        template.setVersion("1.0.0");
        
        // Analysis config template
        PsychologicalAnalysisRequest.AnalysisConfig analysisConfig = new PsychologicalAnalysisRequest.AnalysisConfig();
        analysisConfig.setGeneratePsychologicalVector(true);
        analysisConfig.setAnalyzeBuddhistFactors(true);
        analysisConfig.setCalculateIntensityFactor(true);
        analysisConfig.setProcessStateVector(true);
        analysisConfig.setIncludeDetailedAnalysis(true);
        analysisConfig.setIncludeRecommendations(true);
        analysisConfig.setMaxAnalysisDepth(3);
        analysisConfig.setAnalysisMode("standard");
        template.setAnalysisConfig(analysisConfig);
        
        // User profile template
        PsychologicalAnalysisRequest.UserProfile userProfile = new PsychologicalAnalysisRequest.UserProfile();
        userProfile.setAgeGroup("young_adult");
        userProfile.setGender("unknown");
        userProfile.setCulturalBackground("general");
        userProfile.setTimezone("UTC");
        userProfile.setPreferredLanguage("en");
        userProfile.setRiskLevel("low");
        template.setUserProfile(userProfile);
        
        // Preferences template
        PsychologicalAnalysisRequest.AnalysisPreferences preferences = new PsychologicalAnalysisRequest.AnalysisPreferences();
        preferences.setIncludeEmotionalAnalysis(true);
        preferences.setIncludeCognitiveAnalysis(true);
        preferences.setIncludeSocialAnalysis(true);
        preferences.setIncludeStressIndicators(true);
        preferences.setIncludePersonalityTraits(true);
        preferences.setIncludeMentalHealthIndicators(true);
        preferences.setIncludeCopingMechanisms(true);
        preferences.setIncludeResilienceFactors(true);
        preferences.setOutputFormat("json");
        preferences.setDetailLevel("intermediate");
        template.setPreferences(preferences);
        
        return template;
    }
    
    private Map<String, Object> validateRequest(PsychologicalAnalysisRequest request) {
        Map<String, Object> validation = new java.util.HashMap<>();
        boolean isValid = true;
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        // Validate required fields
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            isValid = false;
            errors.add("Content cannot be null or empty");
        }
        
        if (request.getUserId() == null) {
            isValid = false;
            errors.add("User ID cannot be null");
        }
        
        if (request.getAnalysisConfig() == null) {
            isValid = false;
            errors.add("Analysis configuration cannot be null");
        }
        
        // Validate content length
        if (request.getContent() != null && request.getContent().length() > 10000) {
            isValid = false;
            errors.add("Content length exceeds maximum limit of 10000 characters");
        }
        
        validation.put("isValid", isValid);
        validation.put("errors", errors);
        validation.put("timestamp", java.time.LocalDateTime.now());
        
        return validation;
    }
    
    private Map<String, Object> getAnalysisTypes() {
        Map<String, Object> types = new java.util.HashMap<>();
        
        types.put("psychologicalVector", Map.of(
            "description", "43-dimensional psychological vector based on Buddhist mental factors",
            "dimensions", 43,
            "output", "List<Double>"
        ));
        
        types.put("buddhistFactors", Map.of(
            "description", "Analysis of Buddhist mental factors in content",
            "factors", 43,
            "output", "Map<String, Double>"
        ));
        
        types.put("intensityFactor", Map.of(
            "description", "Prediction of psychological impact intensity",
            "classification", java.util.Arrays.asList("HIGH", "MODERATE", "LOW", "VERY_LOW"),
            "output", "Double"
        ));
        
        types.put("stateVector", Map.of(
            "description", "Processed state vector with normalization and optimization",
            "processing", "normalization, smoothing, validation",
            "output", "List<Double>"
        ));
        
        types.put("comprehensiveAnalysis", Map.of(
            "description", "Comprehensive psychological analysis including all components",
            "components", java.util.Arrays.asList("emotional", "cognitive", "social", "stress", "personality", "mental_health", "coping", "resilience"),
            "output", "ComprehensiveAnalysis"
        ));
        
        return types;
    }
    
    private Map<String, Object> getCapabilities() {
        Map<String, Object> capabilities = new java.util.HashMap<>();
        
        capabilities.put("supportedLanguages", java.util.Arrays.asList("en", "es", "fr", "de"));
        capabilities.put("maxContentLength", 10000);
        capabilities.put("supportedContentTypes", java.util.Arrays.asList("text", "post", "comment", "message", "journal_entry"));
        capabilities.put("analysisModes", java.util.Arrays.asList("quick", "standard", "comprehensive"));
        capabilities.put("detailLevels", java.util.Arrays.asList("basic", "intermediate", "advanced"));
        capabilities.put("outputFormats", java.util.Arrays.asList("json", "detailed", "summary"));
        capabilities.put("realTimeAnalysis", true);
        capabilities.put("batchAnalysis", true);
        capabilities.put("customAnalysis", true);
        
        return capabilities;
    }
}
