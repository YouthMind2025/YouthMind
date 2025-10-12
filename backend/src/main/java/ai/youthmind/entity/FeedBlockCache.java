package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for caching generated feed blocks
 * Stores personalized feed blocks with emotion distribution and post ordering
 */
@Entity
@Table(name = "feed_block_cache", indexes = {
    @Index(name = "idx_feed_block_user_block", columnList = "userId, blockNumber"),
    @Index(name = "idx_feed_block_user_id", columnList = "userId"),
    @Index(name = "idx_feed_block_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedBlockCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Column(name = "block_number", nullable = false)
    @NotNull(message = "Block number is required")
    @Min(value = 1, message = "Block number must be at least 1")
    private Integer blockNumber;
    
    @Column(name = "post_ids", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Post IDs list is required")
    @NotEmpty(message = "Post IDs list cannot be empty")
    private List<UUID> postIds;
    
    @Column(name = "emotion_distribution", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Emotion distribution is required")
    @NotEmpty(message = "Emotion distribution cannot be empty")
    private Map<String, Integer> emotionDistribution;
    
    @Column(name = "r_vector_version", nullable = false)
    @NotNull(message = "R vector version is required")
    @Min(value = 1, message = "R vector version must be at least 1")
    private Integer rVectorVersion;
    
    @Column(name = "generated_at", nullable = false)
    @NotNull(message = "Generated at timestamp is required")
    private LocalDateTime generatedAt;
    
    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expires at timestamp is required")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Set generatedAt if not already set
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        
        // Set default expiration to 24 hours from now if not set
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(24);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this feed block has expired
     * 
     * @return true if current time is after expiresAt
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Get the number of posts in this feed block
     * 
     * @return size of the postIds list
     */
    public int getPostCount() {
        return postIds != null ? postIds.size() : 0;
    }
    
    /**
     * Check if this feed block is valid (not expired and has posts)
     * 
     * @return true if block is valid
     */
    public boolean isValid() {
        return !isExpired() && getPostCount() > 0;
    }
    
    /**
     * Get the age of this feed block in hours
     * 
     * @return hours since generation
     */
    public long getAgeInHours() {
        if (generatedAt == null) {
            return 0;
        }
        return java.time.Duration.between(generatedAt, LocalDateTime.now()).toHours();
    }
    
    /**
     * Get time until expiration in hours
     * 
     * @return hours until expiration (negative if expired)
     */
    public long getTimeUntilExpirationInHours() {
        if (expiresAt == null) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
    }
}


