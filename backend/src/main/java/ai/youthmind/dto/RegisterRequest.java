package ai.youthmind.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    // Username is now optional - if not provided, will be auto-generated
    @Size(max = 50, message = "Username must be less than 50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
    
    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;
} 