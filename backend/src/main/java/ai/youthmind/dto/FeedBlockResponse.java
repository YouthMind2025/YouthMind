package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extended feed response for block-based feeds with personalized recommendations
 * Extends FeedResponse to maintain iOS compatibility while adding block-specific metadata
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Extended feed response for block-based feeds with personalized recommendations")
public class FeedBlockResponse extends FeedResponse {
    
    @Schema(description = "Current block number (1-based)", example = "1")
    private Integer blockNumber;
    
    @Schema(description = "Metadata about this feed block including emotion distribution and generation info")
    private BlockMetadata blockMetadata;
    
    @Schema(description = "Whether the next block is available for prefetching", example = "true")
    private Boolean nextBlockAvailable;
    
    @Schema(description = "Next block number for prefetching", example = "2")
    private Integer nextBlockNumber;
    
    /**
     * Default constructor - initializes with empty posts list
     */
    public FeedBlockResponse() {
        super();
    }
    
    /**
     * Constructor with posts list
     * 
     * @param posts List of posts in this block
     */
    public FeedBlockResponse(java.util.List<PostDto> posts) {
        super();
        this.setPosts(posts);
    }
    
    /**
     * Check if this block is personalized (not chronological)
     * 
     * @return true if block is personalized
     */
    public boolean isPersonalized() {
        return blockMetadata != null && Boolean.TRUE.equals(blockMetadata.getIsPersonalized());
    }
    
    /**
     * Check if this block has expired
     * 
     * @return true if block has expired
     */
    public boolean isBlockExpired() {
        return blockMetadata != null && blockMetadata.isExpired();
    }
    
    /**
     * Get the dominant emotion in this block
     * 
     * @return dominant emotion or null
     */
    public String getDominantEmotion() {
        return blockMetadata != null ? blockMetadata.getDominantEmotion() : null;
    }
    
    /**
     * Get the total number of posts in this block
     * 
     * @return post count
     */
    public int getBlockPostCount() {
        return blockMetadata != null ? blockMetadata.getTotalPostCount() : 0;
    }
    
    /**
     * Get the R vector version used for this block
     * 
     * @return R vector version or null
     */
    public Integer getRVectorVersion() {
        return blockMetadata != null ? blockMetadata.getRVectorVersion() : null;
    }
    
    /**
     * Check if this is the first block for the user
     * 
     * @return true if this is block number 1
     */
    public boolean isFirstBlock() {
        return blockNumber != null && blockNumber == 1;
    }
    
    /**
     * Get the age of this block in hours
     * 
     * @return hours since generation
     */
    public long getBlockAgeInHours() {
        return blockMetadata != null ? blockMetadata.getAgeInHours() : 0;
    }
    
    /**
     * Get time until block expiration in hours
     * 
     * @return hours until expiration
     */
    public long getTimeUntilExpirationInHours() {
        return blockMetadata != null ? blockMetadata.getTimeUntilExpirationInHours() : 0;
    }
    
    /**
     * Get emotion distribution as percentages
     * 
     * @return map of emotion -> percentage
     */
    public java.util.Map<String, Double> getEmotionDistributionPercentages() {
        return blockMetadata != null ? blockMetadata.getEmotionDistributionPercentages() : java.util.Map.of();
    }
    
    /**
     * Set block metadata and update related fields
     * 
     * @param blockMetadata The block metadata to set
     */
    public void setBlockMetadata(BlockMetadata blockMetadata) {
        this.blockMetadata = blockMetadata;
        
        // Update pagination fields to reflect block-based structure
        if (blockMetadata != null) {
            this.setCurrentPage(0); // Blocks are always page 0
            this.setTotalPages(1); // Each block is self-contained
            this.setTotalElements(blockMetadata.getTotalPostCount());
            this.setPageSize(blockMetadata.getTotalPostCount());
            this.setFirst(true); // Each block is the "first" page
            this.setLast(!Boolean.TRUE.equals(nextBlockAvailable)); // Last if no next block
        }
    }
    
    /**
     * Set next block availability and update pagination
     * 
     * @param nextBlockAvailable Whether next block is available
     */
    public void setNextBlockAvailable(Boolean nextBlockAvailable) {
        this.nextBlockAvailable = nextBlockAvailable;
        this.setHasNext(Boolean.TRUE.equals(nextBlockAvailable));
        this.setLast(!Boolean.TRUE.equals(nextBlockAvailable));
    }
}