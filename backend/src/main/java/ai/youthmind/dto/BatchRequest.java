package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * DTO for batch API requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequest<T> {
    
    @NotNull(message = "Batch ID is required")
    private String batchId;
    
    @NotEmpty(message = "Operations list cannot be empty")
    @Size(max = 100, message = "Maximum 100 operations per batch")
    private List<BatchOperation<T>> operations;
    
    @lombok.Builder.Default
    private boolean failFast = false; // If true, stop on first failure
    @lombok.Builder.Default
    private boolean returnResults = true; // If true, return results for each operation
    @lombok.Builder.Default
    private Integer timeoutSeconds = 300; // 5 minutes default timeout
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperation<T> {
        @NotNull(message = "Operation ID is required")
        private String operationId;
        
        @NotNull(message = "Operation type is required")
        private String operationType; // CREATE, UPDATE, DELETE, GET, etc.
        
        private T data;
        private Map<String, Object> parameters;
        private String resourceId; // For UPDATE/DELETE operations
    }
}
