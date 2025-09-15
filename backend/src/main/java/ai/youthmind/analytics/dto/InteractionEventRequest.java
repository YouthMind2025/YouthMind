
package ai.youthmind.analytics.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class InteractionEventRequest {
    @NotNull
    private UUID userId;
    private UUID postId;
    @NotNull
    private String type; // LIKE, COMMENT, SAVE, SHARE
    private String metadataJson;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
