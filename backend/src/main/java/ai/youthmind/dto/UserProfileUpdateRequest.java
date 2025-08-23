package ai.youthmind.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    @Size(max = 50, message = "Username must be less than 50 characters")
    private String username;
    
    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;
    
    private String avatar;
}
