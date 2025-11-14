package ai.youthmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of deployment configuration validation")
public class DeploymentValidationResult {
    
    @Schema(description = "Whether the configuration is valid", example = "true")
    @JsonProperty("isValid")
    private boolean isValid;
    
    @Schema(description = "List of validation errors", example = "[\"Service name is required\"]")
    private List<String> errors = new java.util.ArrayList<>();
    
    @Schema(description = "List of validation warnings", example = "[\"Health check initial delay should be non-negative\"]")
    private List<String> warnings = new java.util.ArrayList<>();
    
    @Schema(description = "Timestamp when validation was performed")
    private LocalDateTime timestamp;
}

