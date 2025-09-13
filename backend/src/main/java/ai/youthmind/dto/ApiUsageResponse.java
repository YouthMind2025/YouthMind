package ai.youthmind.dto;

import ai.youthmind.entity.AiApiUsage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for AI API usage response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageResponse {

    private UUID id;
    private UUID userId;
    private String sessionId;
    private AiApiUsage.RequestType requestType;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal costPerToken;
    private BigDecimal totalCost;
    private Integer requestDurationMs;
    private Boolean success;
    private String errorMessage;
    private Integer requestPayloadSize;
    private Integer responsePayloadSize;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
