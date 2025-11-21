
package ai.youthmind.analytics.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ViewEventRequest {
    @NotNull
    private UUID userId;
    private UUID postId;
    private long viewDurationSeconds;
    private String metadataJson;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }
    public long getViewDurationSeconds() { return viewDurationSeconds; }
    public void setViewDurationSeconds(long viewDurationSeconds) { this.viewDurationSeconds = viewDurationSeconds; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
