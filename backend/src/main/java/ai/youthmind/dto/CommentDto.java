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
    // Backend fields (required for functionality)
    private UUID postId;
    private UUID authorId;
    private UUID parentId;
    
    // iOS-aligned fields (sent to frontend)
    private UUID id;
    private String author;  // Maps to iOS 'author' field
    private String content;
    private LocalDateTime createdAt;
    private int likes;
    private List<CommentDto> replies;
    private boolean isLiked;
}
