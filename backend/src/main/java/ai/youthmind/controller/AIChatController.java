package ai.youthmind.controller;

import ai.youthmind.dto.AIChatRequest;
import ai.youthmind.dto.AIChatResponse;
import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.dto.PsychologicalAnalysisRequest;
import ai.youthmind.dto.PsychologicalAnalysisResponse;
import ai.youthmind.service.AIChatService;
import ai.youthmind.service.PsychologicalStateCacheService;
import ai.youthmind.service.AIServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/v1/ai-chat")
@Tag(name = "AI Chat", description = "AI-powered chat and conversation management")
public class AIChatController {
    
    @Autowired
    private AIChatService aiChatService;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @PostMapping("/send-message")
    @Operation(
        summary = "Send message to AI chat",
        description = "Send a message to the AI chat system with enhanced personality adaptation and safety filtering"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message processed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Safety filter blocked message")
    })
    public ResponseEntity<ApiResponse<AIChatResponse>> sendMessage(
            @Valid @RequestBody @Parameter(description = "AI chat request", required = true) AIChatRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Process the AI chat request
            AIChatResponse response = aiChatService.processChatRequest(request, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @GetMapping("/personality-profile")
    @Operation(
        summary = "Get user personality profile",
        description = "Get the current user's AI personality profile and preferences"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality profile retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserPersonalityProfile>> getPersonalityProfile() {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Get personality profile (this would be implemented in the service)
            UserPersonalityProfile profile = aiChatService.getUserPersonalityProfile(currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(profile));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @PutMapping("/personality-profile")
    @Operation(
        summary = "Update user personality profile",
        description = "Update the current user's AI personality profile and preferences"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality profile updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserPersonalityProfile>> updatePersonalityProfile(
            @Valid @RequestBody @Parameter(description = "Updated personality profile", required = true) UserPersonalityProfile profile) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Update personality profile (this would be implemented in the service)
            UserPersonalityProfile updatedProfile = aiChatService.updateUserPersonalityProfile(currentUserId, profile);
            
            return ResponseEntity.ok(ApiResponse.success(updatedProfile));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @PostMapping("/test-safety-filter")
    @Operation(
        summary = "Test safety filter",
        description = "Test the safety filter with a sample message (for development/testing)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Safety filter test completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<String>> testSafetyFilter(
            @RequestBody @Parameter(description = "Test message", required = true) String testMessage) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Test safety filter
            String result = aiChatService.testSafetyFilter(testMessage, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @PostMapping("/analyze-personality")
    @Operation(
        summary = "Analyze user personality",
        description = "Use DeepSeek API to analyze user personality based on a sample message"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality analysis completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<String>> analyzePersonality(
            @RequestBody @Parameter(description = "Sample message for personality analysis", required = true) String sampleMessage) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Analyze personality using DeepSeek API
            String result = aiChatService.analyzeUserPersonality(currentUserId, sampleMessage);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    // ===== PSYCHOLOGICAL STATE ENDPOINTS =====
    
    @GetMapping("/psychological-state/{userId}")
    @Operation(
        summary = "Get user psychological state",
        description = "Retrieve the current psychological state vector and analysis for a user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological state retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPsychologicalState(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            // Get current user for authorization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Get psychological state from cache
            Map<String, Object> psychologicalState = psychologicalStateCacheService.getPsychologicalState(userId);
            
            if (psychologicalState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Map<String, Object>>error(null, 404));
            }
            
            return ResponseEntity.ok(ApiResponse.success(psychologicalState));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @PutMapping("/psychological-state/{userId}")
    @Operation(
        summary = "Update user psychological state",
        description = "Update the psychological state vector for a user based on new content"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological state updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePsychologicalState(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @RequestBody @Parameter(description = "Content to analyze for state update", required = true) String content) {
        
        try {
            // Get current user for authorization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Update psychological state based on content (simplified)
            psychologicalStateCacheService.warmUpCache(userId);
            Map<String, Object> updatedState = psychologicalStateCacheService.getPsychologicalState(userId);
            
            return ResponseEntity.ok(ApiResponse.success(updatedState));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @PostMapping("/psychological-state/analyze")
    @Operation(
        summary = "Analyze psychological state",
        description = "Perform comprehensive psychological analysis on content"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological analysis completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PsychologicalAnalysisResponse>> analyzePsychologicalState(
            @Valid @RequestBody @Parameter(description = "Psychological analysis request", required = true) PsychologicalAnalysisRequest request) {
        
        try {
            // Get current user for authorization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();
            
            // Perform comprehensive psychological analysis
            PsychologicalAnalysisResponse response = aiServiceClient.performPsychologicalAnalysis(request);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
}
