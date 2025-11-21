package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response for async task operations
 * Contains taskId for tracking and initial status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncTaskResponse<T> {
    
    private String taskId;
    private String status; // PROCESSING, COMPLETED, FAILED
    private String message;
    private T data; // The actual response data (if completed immediately)
    private LocalDateTime timestamp;
    
    /**
     * Create a processing response with taskId
     */
    public static <T> AsyncTaskResponse<T> processing(String taskId, String message) {
        return AsyncTaskResponse.<T>builder()
            .taskId(taskId)
            .status("PROCESSING")
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a completed response with data
     */
    public static <T> AsyncTaskResponse<T> completed(String taskId, T data, String message) {
        return AsyncTaskResponse.<T>builder()
            .taskId(taskId)
            .status("COMPLETED")
            .data(data)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a failed response
     */
    public static <T> AsyncTaskResponse<T> failed(String taskId, String message) {
        return AsyncTaskResponse.<T>builder()
            .taskId(taskId)
            .status("FAILED")
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
}




