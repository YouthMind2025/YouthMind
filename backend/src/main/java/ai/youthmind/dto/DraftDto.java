package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftDto {
    private UUID id;
    private String title;
    private String content;
    private String emotion;
    private List<String> topics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
