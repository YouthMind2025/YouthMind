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
public class CommentDto {
    private UUID id;
    private UUID postId;
    private UUID authorId;
    private String authorName;
    private String content;
    private int likes;
    private boolean isLiked;
    private UUID parentId;
    private List<CommentDto> replies;
    private LocalDateTime createdAt;
}
