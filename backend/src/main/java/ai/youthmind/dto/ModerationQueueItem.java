package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModerationQueueItem {
    private UUID contentId;
    private String contentType;
    private String title;
    private String contentPreview;
    private UUID authorId;
    private String status;
    private String priority;
    private LocalDateTime submittedAt;
    private String flagReason;
}
