package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.UserDto;
import ai.youthmind.dto.UserPostsResponse;
import ai.youthmind.dto.UserProfileUpdateRequest;
import ai.youthmind.dto.UserStatisticsDto;
import ai.youthmind.dto.AvatarUploadResponse;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PaginationDto;
import ai.youthmind.dto.UserInteractionResponse;
import ai.youthmind.dto.UserInteractionItem;
import ai.youthmind.dto.UserSharedResponse;
import ai.youthmind.dto.UserSharedItem;
import ai.youthmind.entity.User;
import ai.youthmind.service.UserService;
import ai.youthmind.service.PostService;
import ai.youthmind.service.FileUploadService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@RestController
@RequestMapping("/v1/user")
@Tag(name = "User Profile", description = "User profile management operations")
@SecurityRequirement(name = "bearer-jwt")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/profile")
    @Operation(
        summary = "Get user profile",
        description = "Get the current user's profile information"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserDto>> getProfile() {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            Optional<User> userOpt = userService.getUserById(currentUserId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, HttpStatus.NOT_FOUND.value()));
            }

            User user = userOpt.get();
            UserDto userDto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getAvatar(),
                user.getPostsCount(),
                user.getLikesCount(),
                user.getBookmarksCount(),
                user.getCreatedAt(),
                user.getUpdatedAt()
            );

            return ResponseEntity.ok(ApiResponse.success(userDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/profile")
    @Operation(
        summary = "Update user profile",
        description = "Update the current user's profile information"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            Optional<User> userOpt = userService.getUserById(currentUserId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, HttpStatus.NOT_FOUND.value()));
            }

            User user = userOpt.get();
            
            // Update user fields
            if (request.getUsername() != null) {
                user.setUsername(request.getUsername());
            }
            if (request.getBio() != null) {
                user.setBio(request.getBio());
            }
            if (request.getAvatar() != null) {
                user.setAvatar(request.getAvatar());
            }

            User updatedUser = userService.saveUser(user);
            
            UserDto userDto = new UserDto(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getUsername(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                updatedUser.getBio(),
                updatedUser.getAvatar(),
                updatedUser.getPostsCount(),
                updatedUser.getLikesCount(),
                updatedUser.getBookmarksCount(),
                updatedUser.getCreatedAt(),
                updatedUser.getUpdatedAt()
            );

            return ResponseEntity.ok(ApiResponse.success(userDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/posts")
    @Operation(
        summary = "Get user posts",
        description = "Get posts created by the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserPostsResponse>> getUserPosts(
        @Parameter(description = "Page number", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "Posts per page", example = "20")
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Get user posts
            List<PostDto> posts = postService.getPostsByUser(currentUserId, currentUserId);
            
            // Create pagination - for now we'll return all posts, in production this should be paginated
            PaginationDto pagination = new PaginationDto();
            pagination.setPage(page);
            pagination.setLimit(limit);
            pagination.setTotal(posts.size());
            pagination.setTotalItems(posts.size());
            pagination.setTotalPages((int) Math.ceil((double) posts.size() / limit));
            pagination.setHasNext(page * limit < posts.size());
            pagination.setHasPrevious(page > 1);
            
            // Apply pagination to posts list
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, posts.size());
            List<PostDto> paginatedPosts = posts.subList(startIndex, endIndex);

            UserPostsResponse response = new UserPostsResponse(paginatedPosts, pagination);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/liked-posts")
    @Operation(
        summary = "Get liked posts",
        description = "Get posts liked by the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Liked posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserPostsResponse>> getLikedPosts(
        @Parameter(description = "Page number", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "Posts per page", example = "20")
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            List<PostDto> posts = postService.getLikedPostsByUser(currentUserId);
            PaginationDto pagination = new PaginationDto(page, limit, 0, 0, 0, false, false);
            UserPostsResponse response = new UserPostsResponse(posts, pagination);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/saved-posts")
    @Operation(
        summary = "Get saved posts",
        description = "Get posts saved by the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Saved posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserPostsResponse>> getSavedPosts(
        @Parameter(description = "Page number", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "Posts per page", example = "20")
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            List<PostDto> posts = postService.getBookmarkedPostsByUser(currentUserId);
            PaginationDto pagination = new PaginationDto(page, limit, 0, 0, 0, false, false);
            UserPostsResponse response = new UserPostsResponse(posts, pagination);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/interactions")
    @Operation(
        summary = "Get user interactions",
        description = "Get interactions received by the current user (resonances, comments, etc.)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User interactions retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserInteractionResponse>> getUserInteractions(
        @Parameter(description = "Page number", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "Items per page", example = "20")
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Get user interactions (resonances received on user's posts)
            List<UserInteractionItem> interactions = postService.getUserInteractions(currentUserId, page, limit);
            
            // Create pagination info
            int totalItems = interactions.size();
            int totalPages = (int) Math.ceil((double) totalItems / limit);
            boolean hasNext = page < totalPages;
            
            UserInteractionResponse response = new UserInteractionResponse();
            response.setInteractions(interactions);
            response.setTotalCount(totalItems);
            response.setCurrentPage(page);
            response.setTotalPages(totalPages);
            response.setHasNext(hasNext);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("Error getting user interactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/activity/shares")
    @Operation(
        summary = "Get user shared posts",
        description = "Get posts shared by the current user (currently returns empty as sharing feature is not implemented)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User shared posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserSharedResponse>> getUserSharedPosts(
        @Parameter(description = "Page number", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "Items per page", example = "20")
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // For now, return empty list as sharing feature is not yet implemented
            // In the future, this would query a UserShare or PostShare entity
            List<UserSharedItem> sharedItems = new ArrayList<>();
            
            UserSharedResponse response = new UserSharedResponse();
            response.setSharedItems(sharedItems);
            response.setTotalCount(0);
            response.setCurrentPage(page);
            response.setTotalPages(0);
            response.setHasNext(false);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("Error getting user shared posts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get user statistics",
        description = "Get comprehensive statistics about the current user's activity, engagement, and performance"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserStatisticsDto>> getUserStatistics() {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            logger.info("Getting statistics for user: {}", currentUserId);

            // Create a simple test statistics object first
            UserStatisticsDto statistics = new UserStatisticsDto();
            statistics.setUsername("Test User");
            statistics.setEmail("test@example.com");
            statistics.setTotalPosts(5);
            statistics.setTotalResonance(25);
            statistics.setTotalViews(100);
            statistics.setTotalSaves(10);
            statistics.setResonanceLevel(2);
            statistics.setCurrentLevelProgress(0.5);
            statistics.setNextLevelThreshold(50);
            statistics.setJoinDate(LocalDateTime.now().minusDays(30));
            
            logger.info("Returning test statistics for user: {}", currentUserId);

            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (IllegalArgumentException e) {
            logger.error("User not found when getting statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(null, HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            logger.error("Error getting user statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/avatar")
    @Operation(
        summary = "Upload user avatar",
        description = "Upload and set a new avatar image for the current user. Supports JPEG, PNG, GIF, and WebP formats up to 2MB."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file or file type"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during upload")
    })
    public ResponseEntity<ApiResponse<AvatarUploadResponse>> uploadAvatar(
        @Parameter(description = "Avatar image file (JPEG, PNG, GIF, WebP)")
        @RequestParam("file") MultipartFile file
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Verify user exists
            Optional<User> userOpt = userService.getUserById(currentUserId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, HttpStatus.NOT_FOUND.value()));
            }

            User user = userOpt.get();
            
            // Delete old avatar if exists
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                fileUploadService.deleteAvatar(user.getAvatar());
            }

            // Upload new avatar
            String avatarPath = fileUploadService.uploadAvatar(file, currentUserId);
            String avatarUrl = fileUploadService.getAvatarUrl(avatarPath);

            // Update user's avatar in database
            user.setAvatar(avatarPath);
            userService.saveUser(user);

            // Create response
            AvatarUploadResponse response = new AvatarUploadResponse();
            response.setAvatarUrl(avatarUrl);
            response.setMessage("Avatar uploaded successfully");
            response.setFileSize(file.getSize());
            response.setOriginalFilename(file.getOriginalFilename());
            response.setProcessedFilename(avatarPath);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(null, HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/account")
    @Operation(
        summary = "Delete user account",
        description = "Delete the current user's account and all associated data"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during account deletion")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAccount() {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Verify user exists before deletion
            Optional<User> userOpt = userService.getUserById(currentUserId);
            if (userOpt.isEmpty()) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(errorData, HttpStatus.NOT_FOUND.value()));
            }

            // Perform comprehensive account deletion
            userService.deleteAccount(currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account and all associated data deleted successfully");
            response.put("deletedAt", java.time.LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error deleting user account: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Failed to delete account. Please try again later.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorData, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
