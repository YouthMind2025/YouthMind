package ai.youthmind.controller;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Conversation;
import ai.youthmind.entity.Message;
import ai.youthmind.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/v1/conversations")
@Tag(name = "Conversations", description = "Conversation management operations")
@SecurityRequirement(name = "bearer-jwt")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping
    @Operation(
        summary = "Get conversations",
        description = "Get paginated conversations for the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversations retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<ConversationListResponse>> getConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Get conversations with pagination
            Pageable pageable = PageRequest.of(page - 1, limit);
            ConversationListResponse response = conversationService.getConversationsByUserId(currentUserId, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PostMapping
    @Operation(
        summary = "Create conversation",
        description = "Create a new conversation"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Conversation created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(@Valid @RequestBody ConversationCreateRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Create conversation
            Conversation conversation = new Conversation();
            conversation.setUserId(currentUserId);
            conversation.setTitle(request.getTitle());
            conversation.setIsPinned(false);
            conversation.setCreatedAt(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());

            Conversation savedConversation = conversationService.saveConversation(conversation);
            
            // Convert to DTO
            ConversationDto conversationDto = convertToConversationDto(savedConversation);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(conversationDto, 201));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @GetMapping("/{conversationId}")
    @Operation(
        summary = "Get single conversation",
        description = "Get a specific conversation with its messages"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to access this conversation")
    })
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(@PathVariable UUID conversationId) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if conversation exists
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Conversation conversation = conversationOpt.get();
            
            // Check if user owns the conversation
            if (!conversation.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Convert to DTO
            ConversationDto conversationDto = convertToConversationDto(conversation);
            
            return ResponseEntity.ok(ApiResponse.success(conversationDto));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(
        summary = "Add message to conversation",
        description = "Add a new message to a conversation"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message added successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to add message to this conversation")
    })
    public ResponseEntity<ApiResponse<MessageDto>> addMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MessageCreateRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if conversation exists
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Conversation conversation = conversationOpt.get();
            
            // Check if user owns the conversation
            if (!conversation.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Create message
            Message message = new Message();
            message.setConversationId(conversationId);
            message.setText(request.getText());
            message.setSender(request.getSender());
            message.setTimestamp(LocalDateTime.now());

            Message savedMessage = conversationService.saveMessage(message);
            
            // Update conversation timestamp
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationService.saveConversation(conversation);
            
            // Convert to DTO
            MessageDto messageDto = convertToMessageDto(savedMessage);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(messageDto, 201));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @PostMapping("/{conversationId}/ai-chat")
    @Operation(
        summary = "Send message with AI response",
        description = "Send a message to a conversation and get an AI-generated response with personality adaptation and safety filtering"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI response generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or safety filter blocked message"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to access this conversation")
    })
    public ResponseEntity<ApiResponse<AIChatResponse>> sendMessageWithAIResponse(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MessageCreateRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if conversation exists
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Conversation conversation = conversationOpt.get();
            
            // Check if user owns the conversation
            if (!conversation.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Process message with AI response
            AIChatResponse aiResponse = conversationService.addMessageWithAIResponse(conversationId, request, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(aiResponse));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PutMapping("/{conversationId}")
    @Operation(
        summary = "Update conversation",
        description = "Update conversation title and pin status"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to update this conversation")
    })
    public ResponseEntity<ApiResponse<ConversationDto>> updateConversation(
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationUpdateRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if conversation exists
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Conversation conversation = conversationOpt.get();
            
            // Check if user owns the conversation
            if (!conversation.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Update conversation
            conversation.setTitle(request.getTitle());
            conversation.setIsPinned(request.isPinned());
            conversation.setUpdatedAt(LocalDateTime.now());

            Conversation updatedConversation = conversationService.saveConversation(conversation);
            
            // Convert to DTO
            ConversationDto conversationDto = convertToConversationDto(updatedConversation);
            
            return ResponseEntity.ok(ApiResponse.success(conversationDto));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(
        summary = "Get messages for conversation",
        description = "Get all messages for a specific conversation with pagination"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to access this conversation")
    })
    public ResponseEntity<ApiResponse<MessageListResponse>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if conversation exists
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Conversation conversation = conversationOpt.get();
            
            // Check if user owns the conversation
            if (!conversation.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Get messages with pagination
            Pageable pageable = PageRequest.of(page - 1, limit);
            Page<Message> messagePage = conversationService.getMessagesByConversationIdWithPagination(conversationId, pageable);
            
            // Convert to DTOs
            List<MessageDto> messageDtos = messagePage.getContent().stream()
                .map(this::convertToMessageDto)
                .collect(Collectors.toList());
            
            // Create pagination info
            PaginationDto pagination = new PaginationDto(
                page,
                limit,
                (int) messagePage.getTotalElements(),
                (int) messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.hasNext(),
                messagePage.hasPrevious()
            );
            
            MessageListResponse response = new MessageListResponse(messageDtos, pagination);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @DeleteMapping("/{conversationId}")
    @Operation(
        summary = "Delete conversation",
        description = "Delete a specific conversation and all its messages"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to delete this conversation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during deletion")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteConversation(@PathVariable UUID conversationId) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "Unauthorized access");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(errorData, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if conversation exists
            Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId);
            if (conversationOpt.isEmpty()) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "Conversation not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(errorData, 404));
            }

            Conversation conversation = conversationOpt.get();
            
            // Check if user owns the conversation
            if (!conversation.getUserId().equals(currentUserId)) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "Not authorized to delete this conversation");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(errorData, 403));
            }

            // Delete conversation
            conversationService.deleteConversation(conversationId, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conversation and all messages deleted successfully");
            response.put("deletedAt", java.time.LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error deleting conversation: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Failed to delete conversation. Please try again later.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorData, 500));
        }
    }

    private ConversationDto convertToConversationDto(Conversation conversation) {
        // Get messages for this conversation
        List<MessageDto> messages = conversationService.getMessagesByConversationId(conversation.getId());

        return new ConversationDto(
            conversation.getId(),
            conversation.getTitle(),
            messages,
            conversation.getIsPinned(),
            conversation.getCreatedAt(),
            conversation.getUpdatedAt()
        );
    }

    private MessageDto convertToMessageDto(Message message) {
        return new MessageDto(
            message.getId(),
            message.getText(),
            message.getSender(),
            message.getTimestamp(),
            message.getConversationId()
        );
    }
}
