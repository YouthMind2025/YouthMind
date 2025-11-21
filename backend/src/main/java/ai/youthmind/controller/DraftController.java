package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.DraftCreateRequest;
import ai.youthmind.dto.DraftDto;
import ai.youthmind.dto.DraftListResponse;
import ai.youthmind.dto.DraftPublishResponse;
import ai.youthmind.dto.DraftUpdateRequest;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.dto.PaginationDto;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.UserDto;
import ai.youthmind.entity.Draft;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.service.DraftService;
import ai.youthmind.service.ModerationLoggingService;
import ai.youthmind.service.PostModerationService;
import ai.youthmind.service.PostService;
import ai.youthmind.service.UserService;
import ai.youthmind.util.ModerationUtils;
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

@RestController
@RequestMapping("/v1/drafts")
@Tag(name = "Drafts", description = "Draft management operations")
@SecurityRequirement(name = "bearer-jwt")
public class DraftController {

    @Autowired
    private DraftService draftService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PostModerationService postModerationService;
    
    @Autowired
    private ModerationLoggingService moderationLoggingService;

    @GetMapping
    @Operation(
        summary = "Get drafts",
        description = "Get paginated drafts for the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Drafts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<DraftListResponse>> getDrafts(
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

            // Get drafts with pagination
            Pageable pageable = PageRequest.of(page - 1, limit);
            DraftListResponse response = draftService.getDraftsByUserId(currentUserId, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PostMapping
    @Operation(
        summary = "Save draft",
        description = "Create a new draft"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Draft created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<DraftDto>> saveDraft(@Valid @RequestBody DraftCreateRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Create draft
            Draft draft = new Draft();
            draft.setUserId(currentUserId);
            draft.setTitle(request.getTitle());
            draft.setContent(request.getContent());
            draft.setEmotion(request.getEmotion());
            
            // Convert List<String> to comma-separated String
            if (request.getTopics() != null && !request.getTopics().isEmpty()) {
                draft.setTopics(String.join(",", request.getTopics()));
            } else {
                draft.setTopics("");
            }
            
            draft.setCreatedAt(LocalDateTime.now());
            draft.setUpdatedAt(LocalDateTime.now());

            Draft savedDraft = draftService.saveDraft(draft);
            
            // Convert to DTO
            DraftDto draftDto = convertToDraftDto(savedDraft);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(draftDto, 201));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PutMapping("/{draftId}")
    @Operation(
        summary = "Update draft",
        description = "Update an existing draft"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Draft updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Draft not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to update this draft")
    })
    public ResponseEntity<ApiResponse<DraftDto>> updateDraft(
            @PathVariable UUID draftId,
            @Valid @RequestBody DraftUpdateRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if draft exists
            Optional<Draft> draftOpt = draftService.getDraftById(draftId);
            if (draftOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Draft draft = draftOpt.get();
            
            // Check if user owns the draft
            if (!draft.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Update draft
            draft.setTitle(request.getTitle());
            draft.setContent(request.getContent());
            draft.setEmotion(request.getEmotion());
            
            // Convert List<String> to comma-separated String
            if (request.getTopics() != null && !request.getTopics().isEmpty()) {
                draft.setTopics(String.join(",", request.getTopics()));
            } else {
                draft.setTopics("");
            }
            
            draft.setUpdatedAt(LocalDateTime.now());

            Draft updatedDraft = draftService.saveDraft(draft);
            
            // Convert to DTO
            DraftDto draftDto = convertToDraftDto(updatedDraft);
            
            return ResponseEntity.ok(ApiResponse.success(draftDto));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @DeleteMapping("/{draftId}")
    @Operation(
        summary = "Delete draft",
        description = "Delete a specific draft"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Draft deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Draft not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to delete this draft")
    })
    public ResponseEntity<ApiResponse<Object>> deleteDraft(@PathVariable UUID draftId) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if draft exists
            Optional<Draft> draftOpt = draftService.getDraftById(draftId);
            if (draftOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Draft draft = draftOpt.get();
            
            // Check if user owns the draft
            if (!draft.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Delete draft
            draftService.deleteDraft(draftId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success(
                Map.of("success", true, "message", "Draft deleted successfully")
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PostMapping("/{draftId}/publish")
    @Operation(
        summary = "Publish draft",
        description = "Publish a draft as a new post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Draft published successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Draft not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to publish this draft")
    })
    public ResponseEntity<ApiResponse<PostDto>> publishDraft(@PathVariable UUID draftId) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if draft exists
            Optional<Draft> draftOpt = draftService.getDraftById(draftId);
            if (draftOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Draft draft = draftOpt.get();
            
            // Check if user owns the draft
            if (!draft.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Create post from draft
            Post post = new Post();
            post.setUserId(currentUserId);
            post.setTitle(draft.getTitle());
            post.setContent(draft.getContent());
            post.setEmotion(draft.getEmotion());
            post.setTopics(draft.getTopics());
            post.setPreImg(null); // No image for published draft
            post.setResonance(0);
            post.setCollect(0);
            post.setView(0);
            post.setCreatedAt(LocalDateTime.now());

            // AI Moderation Integration - Pre-publish content screening for drafts
            try {
                // Check if user should be exempt from moderation
                if (!postModerationService.isUserExemptFromModeration(currentUserId)) {
                    // Perform AI moderation on draft content
                    ModerationResult moderationResult = postModerationService.moderateDraft(
                        draft.getTitle(), 
                        draft.getContent(), 
                        currentUserId
                    );
                    
                    // Log moderation result
                    moderationLoggingService.logDraftModeration(
                        draft.getTitle(), 
                        draft.getContent(), 
                        currentUserId, 
                        moderationResult, 
                        "draft_publishing"
                    );
                    
                    // Apply moderation actions
                    if (!moderationResult.isSafe()) {
                        // Content flagged - post will be saved but flagged
                        post.setFlagged(true);
                        post.setFlagReason(moderationResult.getReason());
                        post.setFlaggedAt(LocalDateTime.now());
                        
                        // Log warning issuance if applicable
                        if (ModerationUtils.shouldIssueWarning(moderationResult)) {
                        moderationLoggingService.logWarningIssuance(
                            currentUserId, 
                            post.getId(), 
                            ModerationUtils.mapCategoryToWarningTypeString(ModerationUtils.getPrimaryCategory(moderationResult)),
                            ModerationUtils.getSeverityAsString(moderationResult),
                            "AI-detected harmful content in draft: " + moderationResult.getReason()
                        );
                        }
                    } else {
                        // Content is safe
                        post.setFlagged(false);
                        post.setFlagReason(null);
                        post.setFlaggedAt(null);
                    }
                } else {
                    // User is exempt from moderation
                    post.setFlagged(false);
                    post.setFlagReason(null);
                    post.setFlaggedAt(null);
                }
            } catch (Exception e) {
                // Log moderation error but don't block draft publishing
                moderationLoggingService.logModerationError(currentUserId, null, e, "draft_publishing_moderation");
                
                // Default to safe on error to avoid blocking legitimate content
                post.setFlagged(false);
                post.setFlagReason(null);
                post.setFlaggedAt(null);
            }

            Post savedPost = postService.savePost(post);
            
            // Delete the draft
            draftService.deleteDraft(draftId, currentUserId);
            
            // Convert to PostDto
            PostDto postDto = convertToPostDto(savedPost, currentUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postDto, 201));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    private DraftDto convertToDraftDto(Draft draft) {
        return new DraftDto(
            draft.getId(),
            draft.getTitle(),
            draft.getContent(),
            draft.getEmotion(),
            draft.getTopics() != null ? List.of(draft.getTopics().split(",")) : List.of(),
            draft.getCreatedAt(),
            draft.getUpdatedAt()
        );
    }

    private PostDto convertToPostDto(Post post, UUID currentUserId) {
        // Get author info
        Optional<User> authorOpt = userService.getUserById(post.getUserId());
        String authorName = authorOpt.map(user -> 
            (user.getFirstName() + " " + user.getLastName()).trim()).orElse("Unknown User");

        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setEmotion(post.getEmotion());
        dto.setPreImg(post.getPreImg());
        dto.setTopics(post.getTopics());
        dto.setResonance(post.getResonance());
        dto.setView(post.getView());
        dto.setCollect(post.getCollect());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setLiked(false);
        dto.setBookmarked(false);
        
        // Set author info
        if (authorOpt.isPresent()) {
            User author = authorOpt.get();
            UserDto authorDto = new UserDto();
            authorDto.setId(author.getId());
            authorDto.setUsername(author.getUsername());
            authorDto.setFirstName(author.getFirstName());
            authorDto.setLastName(author.getLastName());
            dto.setAuthor(authorDto);
        }
        
        return dto;
    }
    
}
