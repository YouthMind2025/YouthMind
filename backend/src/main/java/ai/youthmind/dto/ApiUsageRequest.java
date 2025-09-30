package ai.youthmind.dto;

import ai.youthmind.entity.AiApiUsage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for recording AI API usage requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageRequest {

    @NotNull(message = "Request type is required")
    private AiApiUsage.RequestType requestType;

    @NotBlank(message = "Model name is required")
    private String modelName;

    @PositiveOrZero(message = "Prompt tokens must be non-negative")
    @Builder.Default
    private Integer promptTokens = 0;

    @PositiveOrZero(message = "Completion tokens must be non-negative")
    @Builder.Default
    private Integer completionTokens = 0;

    @PositiveOrZero(message = "Cost per token must be non-negative")
    @Builder.Default
    private BigDecimal costPerToken = BigDecimal.ZERO;

    @PositiveOrZero(message = "Request duration must be non-negative")
    @Builder.Default
    private Integer requestDurationMs = 0;

    @NotNull(message = "Success status is required")
    @Builder.Default
    private Boolean success = true;

    private String errorMessage;

    @PositiveOrZero(message = "Request payload size must be non-negative")
    @Builder.Default
    private Integer requestPayloadSize = 0;

    @PositiveOrZero(message = "Response payload size must be non-negative")
    @Builder.Default
    private Integer responsePayloadSize = 0;

    private String sessionId;

    private UUID userId;

    private String ipAddress;

    private String userAgent;
}
