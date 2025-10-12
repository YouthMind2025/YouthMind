package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Entity for tracking user feed state and preferences
 * Maintains state across feed sessions and tracks user progress
 */
@Entity
@Table(name = "user_feed_state", indexes = {
    @Index(name = "idx_user_feed_state_user_id", columnList = "userId"),
    @Index(name = "idx_user_feed_state_last_accessed", columnList = "lastAccessedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFeedState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Column(name = "current_block_number", nullable = false)
    @NotNull(message = "Current block number is required")
    @Min(value = 0, message = "Current block number must be at least 0")
    private Integer currentBlockNumber = 0;
    
    @Column(name = "last_accessed_at", nullable = false)
    @NotNull(message = "Last accessed timestamp is required")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "shown_post_ids", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Shown post IDs set is required")
    private Set<UUID> shownPostIds = new HashSet<>();
    
    @Column(name = "current_r_vector_version", nullable = false)
    @NotNull(message = "Current R vector version is required")
    @Min(value = 1, message = "R vector version must be at least 1")
    private Integer currentRVectorVersion = 1;
    
    @Column(name = "total_blocks_generated", nullable = false)
    @NotNull(message = "Total blocks generated is required")
    @Min(value = 0, message = "Total blocks generated must be at least 0")
    private Integer totalBlocksGenerated = 0;
    
    @Column(name = "block_r_vector_mapping", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<Integer, Integer> blockRVectorMapping = new HashMap<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Set lastAccessedAt if not already set
        if (lastAccessedAt == null) {
            lastAccessedAt = LocalDateTime.now();
        }
        
        // Initialize shownPostIds if null
        if (shownPostIds == null) {
            shownPostIds = new HashSet<>();
        }
        
        // Initialize blockRVectorMapping if null
        if (blockRVectorMapping == null) {
            blockRVectorMapping = new HashMap<>();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Increment the current block number to move to the next block
     * 
     * @return the new block number
     */
    public int incrementBlockNumber() {
        this.currentBlockNumber++;
        return this.currentBlockNumber;
    }
    
    /**
     * Add multiple post IDs to the shown posts set
     * 
     * @param postIds List of post IDs to add
     */
    public void addShownPosts(java.util.List<UUID> postIds) {
        if (postIds != null && !postIds.isEmpty()) {
            if (this.shownPostIds == null) {
                this.shownPostIds = new HashSet<>();
            }
            this.shownPostIds.addAll(postIds);
        }
    }
    
    /**
     * Add a single post ID to the shown posts set
     * 
     * @param postId Post ID to add
     */
    public void addShownPost(UUID postId) {
        if (postId != null) {
            if (this.shownPostIds == null) {
                this.shownPostIds = new HashSet<>();
            }
            this.shownPostIds.add(postId);
        }
    }
    
    /**
     * Check if a post has been shown to the user
     * 
     * @param postId Post ID to check
     * @return true if the post has been shown
     */
    public boolean hasSeenPost(UUID postId) {
        return postId != null && shownPostIds != null && shownPostIds.contains(postId);
    }
    
    /**
     * Reset the user feed state to initial values
     * Useful for testing or when user requests a fresh start
     */
    public void resetState() {
        this.currentBlockNumber = 0;
        this.shownPostIds = new HashSet<>();
        this.totalBlocksGenerated = 0;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * Update the last accessed timestamp to current time
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * Alias for incrementBlockNumber() for API consistency
     * Increments the current block number to move to the next block
     * 
     * @return the new block number
     */
    public int incrementBlock() {
        return incrementBlockNumber();
    }
    
    /**
     * Get the current R vector version being used
     * 
     * @return the current R vector version
     */
    public Integer getCurrentRVersion() {
        return this.currentRVectorVersion;
    }
    
    /**
     * Alias for updateLastAccessed() for API consistency
     * Updates the last accessed timestamp (scroll time) to current time
     */
    public void updateScrollTime() {
        updateLastAccessed();
    }
    
    /**
     * Increment the total blocks generated counter
     * 
     * @return the new total count
     */
    public int incrementTotalBlocksGenerated() {
        this.totalBlocksGenerated++;
        return this.totalBlocksGenerated;
    }
    
    /**
     * Update the current R vector version
     * 
     * @param newVersion New R vector version
     */
    public void updateRVectorVersion(Integer newVersion) {
        if (newVersion != null && newVersion >= 1) {
            this.currentRVectorVersion = newVersion;
        }
    }
    
    /**
     * Get the number of posts shown to the user
     * 
     * @return size of the shownPostIds set
     */
    public int getShownPostCount() {
        return shownPostIds != null ? shownPostIds.size() : 0;
    }
    
    /**
     * Check if the user has seen any posts
     * 
     * @return true if shownPostIds is not empty
     */
    public boolean hasSeenAnyPosts() {
        return shownPostIds != null && !shownPostIds.isEmpty();
    }
    
    /**
     * Get the age of the last access in hours
     * 
     * @return hours since last access
     */
    public long getLastAccessAgeInHours() {
        if (lastAccessedAt == null) {
            return 0;
        }
        return java.time.Duration.between(lastAccessedAt, LocalDateTime.now()).toHours();
    }
    
    /**
     * Check if the user feed state is stale (not accessed recently)
     * 
     * @param staleThresholdHours Hours threshold for considering state stale
     * @return true if last access is older than threshold
     */
    public boolean isStale(long staleThresholdHours) {
        return getLastAccessAgeInHours() > staleThresholdHours;
    }
    
    /**
     * Set the R vector version used for a specific block
     * 
     * @param blockNumber The block number
     * @param rVectorVersion The R vector version used for that block
     */
    public void setRVectorVersionForBlock(Integer blockNumber, Integer rVectorVersion) {
        if (blockNumber != null && rVectorVersion != null) {
            if (blockRVectorMapping == null) {
                blockRVectorMapping = new HashMap<>();
            }
            blockRVectorMapping.put(blockNumber, rVectorVersion);
        }
    }
    
    /**
     * Get the R vector version used for a specific block
     * 
     * @param blockNumber The block number
     * @return The R vector version used for that block, or null if not found
     */
    public Integer getRVectorVersionForBlock(Integer blockNumber) {
        if (blockNumber == null || blockRVectorMapping == null) {
            return null;
        }
        return blockRVectorMapping.get(blockNumber);
    }
    
    /**
     * Clear old block mappings to prevent storage from growing indefinitely
     * Keeps only the latest N blocks
     * 
     * @param keepLatestBlocks Number of latest blocks to keep (default 10)
     */
    public void clearOldBlockMappings(Integer keepLatestBlocks) {
        if (blockRVectorMapping == null || blockRVectorMapping.isEmpty()) {
            return;
        }
        
        int keepCount = keepLatestBlocks != null ? keepLatestBlocks : 10;
        
        // Get all block numbers and sort them
        var sortedBlocks = blockRVectorMapping.keySet().stream()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
        
        // If we have more blocks than we want to keep, remove the oldest ones
        if (sortedBlocks.size() > keepCount) {
            int blocksToRemove = sortedBlocks.size() - keepCount;
            for (int i = 0; i < blocksToRemove; i++) {
                blockRVectorMapping.remove(sortedBlocks.get(i));
            }
        }
    }
}


