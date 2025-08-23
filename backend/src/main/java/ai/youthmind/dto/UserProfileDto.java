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
public class UserProfileDto {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String bio;
    private String avatar;
    private Integer postsCount;
    private Integer likesCount;
    private Integer bookmarksCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for complete profile
    private List<PostDto> recentPosts;
    private boolean isFollowing;
    private Integer followersCount;
    private Integer followingCount;
}
