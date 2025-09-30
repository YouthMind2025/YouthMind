package ai.youthmind.exception;

import java.util.UUID;

/**
 * Custom exception class for warning system related errors.
 */
public class WarningSystemException extends RuntimeException {
    
    private final String errorCode;
    private final UUID userId;
    private final UUID warningId;
    
    public WarningSystemException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.userId = null;
        this.warningId = null;
    }
    
    public WarningSystemException(String message, String errorCode, UUID userId) {
        super(message);
        this.errorCode = errorCode;
        this.userId = userId;
        this.warningId = null;
    }
    
    public WarningSystemException(String message, String errorCode, UUID userId, UUID warningId) {
        super(message);
        this.errorCode = errorCode;
        this.userId = userId;
        this.warningId = warningId;
    }
    
    // Static factory methods for common error scenarios
    public static WarningSystemException userNotFound(UUID userId) {
        return new WarningSystemException(
            "User not found with ID: " + userId,
            "USER_NOT_FOUND",
            userId
        );
    }
    
    public static WarningSystemException userAlreadyBanned(UUID userId) {
        return new WarningSystemException(
            "User is already banned: " + userId,
            "USER_ALREADY_BANNED",
            userId
        );
    }
    
    public static WarningSystemException userNotBanned(UUID userId) {
        return new WarningSystemException(
            "User is not currently banned: " + userId,
            "USER_NOT_BANNED",
            userId
        );
    }
    
    public static WarningSystemException warningNotFound(UUID warningId) {
        return new WarningSystemException(
            "Warning not found with ID: " + warningId,
            "WARNING_NOT_FOUND",
            null,
            warningId
        );
    }
    
    public static WarningSystemException warningNotOwnedByUser(UUID warningId, UUID userId) {
        return new WarningSystemException(
            "Warning " + warningId + " is not owned by user " + userId,
            "WARNING_NOT_OWNED_BY_USER",
            userId,
            warningId
        );
    }
    
    public static WarningSystemException invalidWarningType(String warningType) {
        return new WarningSystemException(
            "Invalid warning type: " + warningType,
            "INVALID_WARNING_TYPE"
        );
    }
    
    public static WarningSystemException invalidSeverity(String severity) {
        return new WarningSystemException(
            "Invalid severity level: " + severity,
            "INVALID_SEVERITY"
        );
    }
    
    public static WarningSystemException invalidModerationStatus(String status) {
        return new WarningSystemException(
            "Invalid moderation status: " + status,
            "INVALID_MODERATION_STATUS"
        );
    }
    
    public static WarningSystemException warningSystemDisabled() {
        return new WarningSystemException(
            "Warning system is currently disabled",
            "WARNING_SYSTEM_DISABLED"
        );
    }
    
    public static WarningSystemException moderationStatusNotFound(UUID userId) {
        return new WarningSystemException(
            "Moderation status not found for user: " + userId,
            "MODERATION_STATUS_NOT_FOUND",
            userId
        );
    }
    
    public static WarningSystemException cannotIssueWarningToBannedUser(UUID userId) {
        return new WarningSystemException(
            "Cannot issue warning to banned user: " + userId,
            "CANNOT_WARN_BANNED_USER",
            userId
        );
    }
    
    public static WarningSystemException warningExpired(UUID warningId) {
        return new WarningSystemException(
            "Warning has expired: " + warningId,
            "WARNING_EXPIRED",
            null,
            warningId
        );
    }
    
    public static WarningSystemException warningAlreadyAcknowledged(UUID warningId) {
        return new WarningSystemException(
            "Warning has already been acknowledged: " + warningId,
            "WARNING_ALREADY_ACKNOWLEDGED",
            null,
            warningId
        );
    }
    
    public static WarningSystemException insufficientPermissions(UUID userId) {
        return new WarningSystemException(
            "Insufficient permissions to perform warning system operation for user: " + userId,
            "INSUFFICIENT_PERMISSIONS",
            userId
        );
    }
    
    public static WarningSystemException systemConfigurationError(String message) {
        return new WarningSystemException(
            "Warning system configuration error: " + message,
            "SYSTEM_CONFIGURATION_ERROR"
        );
    }
    
    // Getters
    public String getErrorCode() {
        return errorCode;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public UUID getWarningId() {
        return warningId;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WarningSystemException{");
        sb.append("errorCode='").append(errorCode).append('\'');
        if (userId != null) {
            sb.append(", userId=").append(userId);
        }
        if (warningId != null) {
            sb.append(", warningId=").append(warningId);
        }
        sb.append(", message='").append(getMessage()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
