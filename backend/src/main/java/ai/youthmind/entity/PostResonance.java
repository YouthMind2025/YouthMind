package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's resonance (like) on a post.
 * Maps to the post_resonances table.
 */
@Entity
@Table(name = "post_resonances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostResonance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "resonanced_at", nullable = false)
    private LocalDateTime resonancedAt;

    /**
     * Convenience constructor for creating a new resonance.
     *
     * @param userId The ID of the user who resonanced the post
     * @param postId The ID of the post that was resonanced
     */
    public PostResonance(UUID userId, UUID postId) {
        this.userId = userId;
        this.postId = postId;
        this.resonancedAt = LocalDateTime.now();
    }
} 