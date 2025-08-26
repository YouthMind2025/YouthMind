package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenPair {
    private String accessToken;
    private Timestamp accessTokenExpiry;
    private String refreshToken;
    private Timestamp refreshTokenExpiry;
}
