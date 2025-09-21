package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for queue request submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueRequest {
    
    @NotBlank(message = "Operation is required")
    private String operation;
    
    @NotNull(message = "Data is required")
    private Object data;
    
    @NotBlank(message = "Priority is required")
    @lombok.Builder.Default
    private String priority = "NORMAL"; // HIGH, NORMAL, LOW, BATCH, AI, VECTOR_SEARCH
    
    @NotBlank(message = "User tier is required")
    @lombok.Builder.Default
    private String userTier = "FREE"; // FREE, PREMIUM, ENTERPRISE
    
    @lombok.Builder.Default
    private Integer timeoutSeconds = 300; // 5 minutes default
    private String callbackUrl; // Optional callback for async results
}
