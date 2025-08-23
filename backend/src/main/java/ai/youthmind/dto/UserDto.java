package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
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
} 