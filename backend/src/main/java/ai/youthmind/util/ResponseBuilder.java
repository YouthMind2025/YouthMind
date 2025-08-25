package ai.youthmind.util;

import ai.youthmind.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for building consistent API responses across controllers.
 * 
 * This class provides standardized response creation to ensure uniform
 * API behavior and reduce code duplication throughout the application.
 */
public class ResponseBuilder {
    
    /**
     * Build a successful response with data
     * 
     * @param data The response data
     * @param <T> The type of response data
     * @return ResponseEntity with success status
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    /**
     * Build a successful response with data and message
     * 
     * @param data The response data
     * @param message The success message
     * @param <T> The type of response data
     * @return ResponseEntity with success status
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }
    
    /**
     * Build a successful response with data, message, and status code
     * 
     * @param data The response data
     * @param message The success message
     * @param statusCode The HTTP status code
     * @param <T> The type of response data
     * @return ResponseEntity with specified status
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message, int statusCode) {
        return ResponseEntity.status(statusCode).body(ApiResponse.success(data, message, statusCode));
    }
    
    /**
     * Build an error response with message and status code
     * 
     * @param message The error message
     * @param statusCode The HTTP status code
     * @param <T> The type of response data
     * @return ResponseEntity with error status
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(String message, int statusCode) {
        return ResponseEntity.status(statusCode).body(ApiResponse.error(null, message, statusCode));
    }
    
    /**
     * Build an error response with data, message, and status code
     * 
     * @param data The error data (can be null)
     * @param message The error message
     * @param statusCode The HTTP status code
     * @param <T> The type of response data
     * @return ResponseEntity with error status
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(T data, String message, int statusCode) {
        return ResponseEntity.status(statusCode).body(ApiResponse.error(data, message, statusCode));
    }
    
    /**
     * Build a not found response
     * 
     * @param message The not found message
     * @param <T> The type of response data
     * @return ResponseEntity with 404 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND.value());
    }
    
    /**
     * Build an unauthorized response
     * 
     * @param message The unauthorized message
     * @param <T> The type of response data
     * @return ResponseEntity with 401 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return error(message, HttpStatus.UNAUTHORIZED.value());
    }
    
    /**
     * Build a forbidden response
     * 
     * @param message The forbidden message
     * @param <T> The type of response data
     * @return ResponseEntity with 403 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return error(message, HttpStatus.FORBIDDEN.value());
    }
    
    /**
     * Build a bad request response
     * 
     * @param message The bad request message
     * @param <T> The type of response data
     * @return ResponseEntity with 400 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return error(message, HttpStatus.BAD_REQUEST.value());
    }
    
    /**
     * Build an internal server error response
     * 
     * @param message The internal server error message
     * @param <T> The type of response data
     * @return ResponseEntity with 500 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> internalServerError(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
    
    /**
     * Build a created response
     * 
     * @param data The response data
     * @param message The success message
     * @param <T> The type of response data
     * @return ResponseEntity with 201 status
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return success(data, message, HttpStatus.CREATED.value());
    }
} 