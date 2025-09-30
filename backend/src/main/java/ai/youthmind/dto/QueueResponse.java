package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for queue response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponse {
    
    private String requestId;
    private String status;
    private String message;
    private LocalDateTime timestamp;
    private Object result;
    private String errorMessage;
    private Integer estimatedWaitTimeSeconds;
}
