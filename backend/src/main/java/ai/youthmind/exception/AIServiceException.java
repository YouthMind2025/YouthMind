package ai.youthmind.exception;

/**
 * Custom exception for AI service related errors
 */
public class AIServiceException extends RuntimeException {
    
    private final String errorCode;
    private final String serviceName;
    
    public AIServiceException(String message) {
        super(message);
        this.errorCode = "AI_SERVICE_ERROR";
        this.serviceName = "DeepSeek";
    }
    
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AI_SERVICE_ERROR";
        this.serviceName = "DeepSeek";
    }
    
    public AIServiceException(String errorCode, String serviceName, String message) {
        super(message);
        this.errorCode = errorCode;
        this.serviceName = serviceName;
    }
    
    public AIServiceException(String errorCode, String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.serviceName = serviceName;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Create a configuration error
     */
    public static AIServiceException configurationError(String message) {
        return new AIServiceException("CONFIG_ERROR", "DeepSeek", message);
    }
    
    /**
     * Create an API key error
     */
    public static AIServiceException apiKeyError(String message) {
        return new AIServiceException("API_KEY_ERROR", "DeepSeek", message);
    }
    
    /**
     * Create a timeout error
     */
    public static AIServiceException timeoutError(String message) {
        return new AIServiceException("TIMEOUT_ERROR", "DeepSeek", message);
    }
    
    /**
     * Create a rate limit error
     */
    public static AIServiceException rateLimitError(String message) {
        return new AIServiceException("RATE_LIMIT_ERROR", "DeepSeek", message);
    }
    
    /**
     * Create a quota exceeded error
     */
    public static AIServiceException quotaExceededError(String message) {
        return new AIServiceException("QUOTA_EXCEEDED_ERROR", "DeepSeek", message);
    }
}
