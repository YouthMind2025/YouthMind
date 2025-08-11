package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data transfer object for API operations")
public class UserDto {
    
    @Schema(description = "Unique identifier for the user", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    
    @Schema(description = "User's first name", example = "John")
    private String firstName;
    
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    
    @Schema(description = "Timestamp when the user account was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the user account was last updated", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
} 