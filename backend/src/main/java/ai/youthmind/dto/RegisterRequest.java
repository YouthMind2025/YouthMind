package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request data")
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's email address", example = "user@example.com", required = true)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "User's password (minimum 8 characters)", example = "password123", required = true)
    private String password;
    
    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "John", required = true)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Schema(description = "User's last name", example = "Doe", required = true)
    private String lastName;
} 