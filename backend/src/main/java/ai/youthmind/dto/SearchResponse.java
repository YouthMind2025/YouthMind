package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for search responses - supports both traditional search and vector search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    
    // Traditional search fields (used by existing SearchController and SearchService)
    private List<PostDto> posts; // List of posts
    private PaginationDto pagination; // Pagination information
    private int totalResults; // Total number of results
    
    // Vector search fields (for new VectorSearchController)
    private String query;
    private List<SearchResult> results;
    private Long processingTimeMs;
    private String collectionName;
    private Map<String, Object> metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String id;
        private Double score;
        private Map<String, Object> payload;
        private String content;
        private String contentType;
        private Long contentId;
    }
}