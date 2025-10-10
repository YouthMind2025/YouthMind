package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.NotificationResponse;
import ai.youthmind.dto.NotificationItem;
import ai.youthmind.service.NotificationService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing user notifications
 */
@RestController
@RequestMapping("/v1/notifications")
@Tag(name = "Notifications", description = "User notification management operations")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @Operation(
        summary = "Get user notifications",
        description = "Get notifications for the current user with optional type filtering"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotifications(
        @Parameter(description = "Page number", example = "1")
        @RequestParam(defaultValue = "1") int page,
        
        @Parameter(description = "Items per page", example = "20")
        @RequestParam(defaultValue = "20") int limit,
        
        @Parameter(description = "Notification type filter", example = "SYSTEM")
        @RequestParam(required = false) String type
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Get notifications based on type
            List<NotificationItem> notifications = notificationService.getUserNotifications(currentUserId, page, limit, type);
            
            // Create pagination info
            int totalItems = notifications.size();
            int totalPages = (int) Math.ceil((double) totalItems / limit);
            boolean hasNext = page < totalPages;
            
            NotificationResponse response = new NotificationResponse();
            response.setNotifications(notifications);
            response.setTotalCount(totalItems);
            response.setCurrentPage(page);
            response.setTotalPages(totalPages);
            response.setHasNext(hasNext);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("Error getting user notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{notificationId}/read")
    @Operation(
        summary = "Mark notification as read",
        description = "Mark a specific notification as read by the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable UUID notificationId
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Mark notification as read
            notificationService.markAsRead(notificationId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            logger.error("Error marking notification as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/read-all")
    @Operation(
        summary = "Mark all notifications as read",
        description = "Mark all notifications as read for the current user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead() {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Mark all notifications as read
            notificationService.markAllAsRead(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            logger.error("Error marking all notifications as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}
