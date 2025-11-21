package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftPublishResponse {
    private UUID id;
    private UUID authorId;
    private String authorName;
    private String title;
    private String content;
    private String emotion;
    private String preImg;
    private String topics;
    private Integer resonance;
    private Integer views;
    private Integer collects;
    private boolean isResonanced;
    private boolean isCollected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
