package ai.youthmind.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request data for creating a new service deployment configuration")
public class ServiceDeploymentConfigCreateRequest {
    
    @Schema(description = "Service version", example = "1.0.0", required = false)
    private String version = "1.0.0";
    
    @NotNull(message = "Replicas is required")
    @Min(value = 1, message = "Replicas must be at least 1")
    @Schema(description = "Number of replicas for the service", example = "2", required = true)
    private Integer replicas = 2;
    
    @NotBlank(message = "CPU is required")
    @Schema(description = "CPU resource requirement", example = "500m", required = true)
    private String cpu = "500m";
    
    @NotBlank(message = "Memory is required")
    @Schema(description = "Memory resource requirement", example = "512Mi", required = true)
    private String memory = "512Mi";
    
}
