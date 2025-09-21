package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for batch API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse<T> {
    
    private String batchId;
    private BatchStatus status;
    private int totalOperations;
    private int successfulOperations;
    private int failedOperations;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long processingTimeMs;
    private List<BatchOperationResult<T>> results;
    private Map<String, Object> metadata;
    
    public enum BatchStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        PARTIALLY_COMPLETED,
        TIMEOUT
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperationResult<T> {
        private String operationId;
        private boolean success;
        private T data;
        private String errorMessage;
        private String errorCode;
        private Long processingTimeMs;
        private Map<String, Object> metadata;
    }
}
