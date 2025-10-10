package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Feed response with posts and pagination information")
public class FeedResponse {
    
    @Schema(description = "List of posts in the feed")
    private List<PostDto> posts;
    
    @Schema(description = "Current page number (0-based)")
    private int currentPage;
    
    @Schema(description = "Total number of pages")
    private int totalPages;
    
    @Schema(description = "Total number of posts")
    private long totalElements;
    
    @Schema(description = "Number of posts per page")
    private int pageSize;
    
    @Schema(description = "Whether there are more pages available")
    private boolean hasNext;
    
    @Schema(description = "Whether this is the first page")
    private boolean isFirst;
    
    @Schema(description = "Whether this is the last page")
    private boolean isLast;
    
    // Explicit getters and setters
    public List<PostDto> getPosts() { return posts; }
    public void setPosts(List<PostDto> posts) { this.posts = posts; }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    
    public boolean isFirst() { return isFirst; }
    public void setFirst(boolean isFirst) { this.isFirst = isFirst; }
    
    public boolean isLast() { return isLast; }
    public void setLast(boolean isLast) { this.isLast = isLast; }
} 