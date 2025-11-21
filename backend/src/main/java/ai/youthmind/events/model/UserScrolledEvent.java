package ai.youthmind.events.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event published when a user scrolls through their feed.
 * Triggers block pre-calculation for the next feed block.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserScrolledEvent extends UserInteractionEvent {
    
    /**
     * Current block number the user is viewing
     */
    private Integer currentBlock;
    
    /**
     * Scroll direction (up/down)
     */
    private String scrollDirection;
    
    /**
     * Time spent on current block in seconds
     */
    private Integer timeOnBlock;
    
    /**
     * Whether the user is actively scrolling (vs idle)
     */
    private Boolean isActive;

    public UserScrolledEvent(UUID userId, Integer currentBlock) {
        super(userId, "USER_SCROLLED");
        this.currentBlock = currentBlock;
        this.scrollDirection = "down";
        this.timeOnBlock = null;
        this.isActive = true;
    }

    public UserScrolledEvent(UUID userId, Integer currentBlock, String scrollDirection, Integer timeOnBlock) {
        super(userId, "USER_SCROLLED");
        this.currentBlock = currentBlock;
        this.scrollDirection = scrollDirection;
        this.timeOnBlock = timeOnBlock;
        this.isActive = true;
    }

    @Override
    public String getEventType() {
        return "USER_SCROLLED";
    }

    @Override
    public boolean isValid() {
        return super.isValid() && currentBlock != null && currentBlock >= 0;
    }
}
