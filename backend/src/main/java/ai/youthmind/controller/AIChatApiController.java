package ai.youthmind.controller;

import ai.youthmind.dto.*;
import ai.youthmind.service.AIChatService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "AI Chat API", description = "AI-powered chat endpoints for message processing, suggestions, and analysis")
public class AIChatApiController {
    
    @Autowired
    private AIChatService aiChatService;
    
    @PostMapping("/message")
    @Operation(
        summary = "Send message to AI chat",
        description = "Send a message to the AI chat system and receive an AI-generated response with personality adaptation and safety filtering"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message processed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Safety filter blocked message"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AIChatResponse>> sendMessage(
            @Valid @RequestBody @Parameter(description = "AI chat request", required = true) AIChatRequest request) {
        
        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Process the AI chat request
            AIChatResponse response = aiChatService.processChatRequest(request, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(null, HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    @PostMapping("/suggestions")
    @Operation(
        summary = "Get AI chat suggestions",
        description = "Generate AI-powered suggestions for follow-up questions, responses, topics, or actions based on the user's message"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suggestions generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Safety filter blocked message"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AIChatSuggestionsResponse>> getSuggestions(
            @Valid @RequestBody @Parameter(description = "AI chat suggestions request", required = true) AIChatSuggestionsRequest request) {
        
        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Generate AI suggestions
            AIChatSuggestionsResponse response = aiChatService.generateSuggestions(request, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(null, HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    @PostMapping("/analyze")
    @Operation(
        summary = "Analyze AI chat message",
        description = "Perform comprehensive analysis of the user's message including sentiment, emotion, intent, and provide recommendations"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Analysis completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Safety filter blocked message"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<AIChatAnalyzeResponse>> analyzeMessage(
            @Valid @RequestBody @Parameter(description = "AI chat analysis request", required = true) AIChatAnalyzeRequest request) {
        
        try {
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Perform AI analysis
            AIChatAnalyzeResponse response = aiChatService.analyzeMessage(request, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(null, HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
