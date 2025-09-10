package ai.youthmind.exception;

import ai.youthmind.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for AI service related errors
 */
@RestControllerAdvice
public class AIServiceExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AIServiceExceptionHandler.class);
    
    /**
     * Handle AI service exceptions
     */
    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAIServiceException(AIServiceException ex) {
        logger.error("AI Service Error [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", ex.getErrorCode());
        errorDetails.put("serviceName", ex.getServiceName());
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("timestamp", LocalDateTime.now());
        
        // Determine HTTP status based on error code
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        
        return ResponseEntity.status(status)
            .body(ApiResponse.error(errorDetails, status.value()));
    }
    
    /**
     * Handle general exceptions that might occur in AI service calls
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGeneralException(Exception ex) {
        // Only handle if it's related to AI services (check stack trace)
        if (isAIServiceRelated(ex)) {
            logger.error("Unexpected AI Service Error: {}", ex.getMessage(), ex);
            
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "UNEXPECTED_ERROR");
            errorDetails.put("serviceName", "DeepSeek");
            errorDetails.put("message", "An unexpected error occurred in the AI service");
            errorDetails.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorDetails, 500));
        }
        
        // Let other exception handlers deal with non-AI service exceptions
        return null;
    }
    
    /**
     * Determine HTTP status based on error code
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "CONFIG_ERROR", "API_KEY_ERROR" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "TIMEOUT_ERROR" -> HttpStatus.REQUEST_TIMEOUT;
            case "RATE_LIMIT_ERROR" -> HttpStatus.TOO_MANY_REQUESTS;
            case "QUOTA_EXCEEDED_ERROR" -> HttpStatus.PAYMENT_REQUIRED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * Check if exception is related to AI services
     */
    private boolean isAIServiceRelated(Exception ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("AIServiceClient") || 
                className.contains("DeepSeek") ||
                className.contains("ModerationController")) {
                return true;
            }
        }
        return false;
    }
}
