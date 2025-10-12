package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to track post view with duration")
public class PostViewDurationRequest {
    
    @NotNull(message = "Duration in seconds is required")
    @Min(value = 0, message = "Duration must be non-negative")
    @Schema(description = "Duration in seconds the user viewed the post", example = "20")
    private Integer durationSeconds;
}
