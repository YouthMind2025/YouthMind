package ai.youthmind.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic API response wrapper that matches iOS ApiResponse<T> structure
 * Provides consistent response format across all endpoints
 */
@Schema(description = "Generic API response wrapper")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    @Schema(description = "The actual data payload from the API")
    private T data;
    
    @Schema(description = "HTTP status code of the response", example = "200")
    private int statusCode;
    
    @Schema(description = "Response headers as a key-value map")
    private Map<String, String> headers;
    
    // Default constructor
    public ApiResponse() {
        this.headers = new HashMap<>();
    }
    
    // Constructor with required fields
    public ApiResponse(T data, int statusCode) {
        this.data = data;
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
    }
    
    // Constructor with all fields
    public ApiResponse(T data, int statusCode, Map<String, String> headers) {
        this.data = data;
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : new HashMap<>();
    }
    
    // Static factory methods for common responses
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, 200);
    }
    
    public static <T> ApiResponse<T> success(T data, int statusCode) {
        return new ApiResponse<>(data, statusCode);
    }
    
    public static <T> ApiResponse<T> error(T data, int statusCode) {
        return new ApiResponse<>(data, statusCode);
    }
    
    // Getters and setters
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }
    
    // Helper method to add a header
    public void addHeader(String key, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(key, value);
    }
    
    // Helper method to check if response indicates success
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
} 