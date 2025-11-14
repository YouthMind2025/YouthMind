package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRegisterResponseDto {
    private UUID userId;
    private String token;
    private LocalDateTime expiresAt;
    private String role;
    private String message;
}



