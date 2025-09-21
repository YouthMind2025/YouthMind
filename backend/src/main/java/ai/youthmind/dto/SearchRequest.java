package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

/**
 * DTO for search requests - supports both traditional search and vector search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    
    // Traditional search fields (used by existing SearchController and SearchService)
    private String q; // Query string
    private String emotion; // Emotion filter
    private String sort; // Sort order
    @lombok.Builder.Default
    private int page = 1; // Page number (1-based)
    @lombok.Builder.Default
    private int limit = 10; // Page size
    
    // Vector search fields (for new VectorSearchController)
    @NotBlank(message = "Query is required")
    private String query;
    
    @lombok.Builder.Default
    private String collectionName = "posts"; // Default collection
    
    @NotNull(message = "Threshold is required")
    @lombok.Builder.Default
    private Double threshold = 0.7;
    
    private String[] filters;
    private String sortBy;
    @lombok.Builder.Default
    private String sortOrder = "desc";
    
    // Helper method to get query (prioritizes 'q' for backward compatibility)
    public String getSearchQuery() {
        return q != null ? q : query;
    }
}