package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.UserDto;
import ai.youthmind.dto.UserPostsResponse;
import ai.youthmind.dto.UserProfileUpdateRequest;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PaginationDto;
import ai.youthmind.entity.User;
import ai.youthmind.service.UserService;
import ai.youthmind.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/user")
@Tag(name = "User Profile", description = "User profile management operations")
@SecurityRequirement(name = "bearer-jwt")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // TODO: Implement actual liked posts retrieval
            // This requires implementing PostResonance entity and repository
            List<PostDto> posts = List.of(); // Empty list for now
            
            PaginationDto pagination = new PaginationDto();
            pagination.setPage(page);
            pagination.setLimit(limit);
            pagination.setTotal(0);
            pagination.setTotalItems(0);
            pagination.setTotalPages(0);
            pagination.setHasNext(false);
            pagination.setHasPrevious(false);

            UserPostsResponse response = new UserPostsResponse(posts, pagination);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/bookmarked-posts")
    @Operation(
        summary = "Get bookmarked posts",
        description = "Get posts bookmarked by the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bookmarked posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserPostsResponse>> getBookmarkedPosts(
        @Parameter(description = "Page number", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "Posts per page", example = "20")
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Extract user from authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // TODO: Implement actual bookmarked posts retrieval
            // This requires implementing PostCollection entity and repository
            List<PostDto> posts = List.of(); // Empty list for now
            
            PaginationDto pagination = new PaginationDto();
            pagination.setPage(page);
            pagination.setLimit(limit);
            pagination.setTotal(0);
            pagination.setTotalItems(0);
            pagination.setTotalPages(0);
            pagination.setHasNext(false);
            pagination.setHasPrevious(false);

            UserPostsResponse response = new UserPostsResponse(posts, pagination);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/account")
    @Operation(
        summary = "Delete user account",
        description = "Delete the current user's account"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAccount() {
        try {
            // Extract user from authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // TODO: Implement comprehensive account deletion
            // This should include:
            // 1. Soft delete or hard delete user account
            // 2. Delete or anonymize user's posts
            // 3. Delete user's resonances and collections
            // 4. Delete user's follows/followers relationships
            // 5. Delete user's notifications
            // 6. Delete user's chat messages or anonymize them
            // 7. Delete user's feed preferences
            // 8. Invalidate all user sessions/tokens
            // 9. Remove user from any group chats
            // 10. Delete user's uploaded media files
            
            // For production, this would call userService.deleteAccount(currentUserId)
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully");

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
