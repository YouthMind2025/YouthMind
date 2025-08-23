package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateResponse {
    private UUID id;
    private UUID postId;
    private UUID authorId;
    private String content;
    private Integer likes;
    private UUID parentId;
    private LocalDateTime createdAt;
    private UserDto author;
    private boolean isLiked;
    private String authorName;
}
