package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadResponse {
    private String avatarUrl;
    private String message;
    private long fileSize;
    private String originalFilename;
    private String processedFilename;
}
