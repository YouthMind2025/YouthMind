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
public class PostDto {
    private UUID id;
    private UUID userId;
    private String title;
    private String content;
    private String emotionTag;
    private String topics;
    private String preImg;
    private Integer likes;
    private Integer bookmarks;
    private Integer views;
    private String aiMetadata;
    private boolean flagged;
    private String flagReason;
    private LocalDateTime flaggedAt;
    private LocalDateTime createdAt;
    
    // Additional fields for complete representation
    private UserDto author;
    private List<CommentDto> comments;
    private boolean isLiked;
    private boolean isBookmarked;
    private Integer commentsCount;
    
    // Additional fields for existing service compatibility
    private String anonymousUserId;
    private Integer understandingCount;
    private boolean hasUserUnderstood;
} 