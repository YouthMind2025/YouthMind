package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's collection (save) of a post.
 * Maps to the post_collections table.
 */
@Entity
@Table(name = "post_collections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    /**
     * Convenience constructor for creating a new collection.
     *
     * @param userId The ID of the user who collected the post
     * @param postId The ID of the post that was collected
     */
    public PostCollection(UUID userId, UUID postId) {
        this.userId = userId;
        this.postId = postId;
        this.collectedAt = LocalDateTime.now();
    }
} 