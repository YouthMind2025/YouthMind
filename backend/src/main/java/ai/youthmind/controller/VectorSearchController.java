package ai.youthmind.controller;

import ai.youthmind.dto.SearchRequest;
import ai.youthmind.dto.SearchResponse;
import ai.youthmind.service.VectorSearchService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for vector search operations
 */
@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vector Search", description = "High-performance vector search APIs")
public class VectorSearchController {

    private final VectorSearchService vectorSearchService;
    private final AuthenticationUtils authenticationUtils;

    @PostMapping("/similar")
    @Operation(summary = "Search similar content", description = "Find similar content using vector similarity search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<SearchResponse>> searchSimilarContent(
            @Valid @RequestBody SearchRequest request) {
        
        log.info("Vector search request: query='{}', collection='{}', limit={}", 
            request.getSearchQuery(), request.getCollectionName(), request.getLimit());
        
        CompletableFuture<SearchResponse> future = vectorSearchService.searchSimilarContent(
            request.getSearchQuery(),
            request.getCollectionName(),
            request.getThreshold()
        ).thenApply(results -> {
            List<SearchResponse.SearchResult> searchResults = results.stream()
                .map(this::convertToSearchResponseResult)
                .toList();
            return SearchResponse.builder()
                .query(request.getSearchQuery())
                .results(searchResults)
                .totalResults(searchResults.size())
                .build();
        });
        
        return ResponseEntity.ok(future);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch vector search", description = "Search multiple queries in parallel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<Map<String, SearchResponse>>> batchSearch(
            @Valid @RequestBody List<SearchRequest> requests) {
        
        log.info("Batch vector search request: {} queries", requests.size());
        
        List<String> queries = requests.stream()
            .map(SearchRequest::getSearchQuery)
            .toList();
        
        String collectionName = requests.isEmpty() ? "posts" : requests.get(0).getCollectionName();
        int limit = requests.isEmpty() ? 10 : requests.get(0).getLimit();
        double threshold = requests.isEmpty() ? 0.7 : requests.get(0).getThreshold();
        
        CompletableFuture<Map<String, SearchResponse>> future = vectorSearchService
            .batchSearchSimilarContent(queries, collectionName, limit, threshold)
            .thenApply(results -> {
                Map<String, SearchResponse> responseMap = new java.util.HashMap<>();
                results.forEach((query, searchResults) -> {
                    List<SearchResponse.SearchResult> responseResults = searchResults.stream()
                        .map(this::convertToSearchResponseResult)
                        .toList();
                    responseMap.put(query, SearchResponse.builder()
                        .query(query)
                        .results(responseResults)
                        .totalResults(responseResults.size())
                        .build());
                });
                return responseMap;
            });
        
        return ResponseEntity.ok(future);
    }

    @PostMapping("/index")
    @Operation(summary = "Index content", description = "Index content for vector search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<String>> indexContent(
            @RequestParam String content,
            @RequestParam String contentType,
            @RequestParam Long contentId) {
        
        log.info("Indexing content: type='{}', id={}", contentType, contentId);
        
        CompletableFuture<String> future = vectorSearchService.indexContent(content, contentType, contentId)
            .thenApply(success -> success ? "Content indexed successfully" : "Failed to index content");
        
        return ResponseEntity.ok(future);
    }

    @DeleteMapping("/index/{contentType}/{contentId}")
    @Operation(summary = "Delete indexed content", description = "Remove content from vector index")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<Boolean>> deleteContent(
            @PathVariable String contentType,
            @PathVariable Long contentId) {
        
        log.info("Deleting indexed content: type='{}', id={}", contentType, contentId);
        
        CompletableFuture<Boolean> future = vectorSearchService.removeFromIndex(contentType, contentId);
        
        return ResponseEntity.ok(future);
    }

    @GetMapping("/stats/{collectionName}")
    @Operation(summary = "Get collection statistics", description = "Get vector collection statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<Object>> getCollectionStats(@PathVariable String collectionName) {
        
        CompletableFuture<Object> future = vectorSearchService.getCollectionInfo(collectionName).thenApply(info -> (Object) info);
        
        return ResponseEntity.ok(future);
    }
    
    private SearchResponse.SearchResult convertToSearchResponseResult(VectorSearchService.SearchResult result) {
        return SearchResponse.SearchResult.builder()
            .id(result.getId())
            .score(result.getScore())
            .payload(result.getPayload())
            .content((String) result.getPayload().get("content"))
            .contentType((String) result.getPayload().get("content_type"))
            .contentId(Long.valueOf(result.getPayload().get("content_id").toString()))
            .build();
    }
}
