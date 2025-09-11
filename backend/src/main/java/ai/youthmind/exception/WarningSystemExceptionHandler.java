package ai.youthmind.exception;

import ai.youthmind.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for warning system related exceptions.
 */
@RestControllerAdvice
public class WarningSystemExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WarningSystemExceptionHandler.class);
    
    @ExceptionHandler(WarningSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleWarningSystemException(WarningSystemException ex) {
        logger.error("Warning system error: {} - Error Code: {}", ex.getMessage(), ex.getErrorCode(), ex);
        
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .message(ex.getMessage())
            .errorCode(ex.getErrorCode())
            .build();
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Determines the appropriate HTTP status code based on the error code.
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        switch (errorCode) {
            case "USER_NOT_FOUND":
            case "WARNING_NOT_FOUND":
            case "MODERATION_STATUS_NOT_FOUND":
                return HttpStatus.NOT_FOUND;
                
            case "USER_ALREADY_BANNED":
            case "USER_NOT_BANNED":
            case "WARNING_ALREADY_ACKNOWLEDGED":
            case "WARNING_EXPIRED":
            case "CANNOT_WARN_BANNED_USER":
                return HttpStatus.CONFLICT;
                
            case "WARNING_NOT_OWNED_BY_USER":
            case "INSUFFICIENT_PERMISSIONS":
                return HttpStatus.FORBIDDEN;
                
            case "INVALID_WARNING_TYPE":
            case "INVALID_SEVERITY":
            case "INVALID_MODERATION_STATUS":
                return HttpStatus.BAD_REQUEST;
                
            case "WARNING_SYSTEM_DISABLED":
            case "SYSTEM_CONFIGURATION_ERROR":
                return HttpStatus.SERVICE_UNAVAILABLE;
                
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
