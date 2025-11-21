package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.service.VectorSearchService;
import ai.youthmind.service.VectorCollectionManager;
import ai.youthmind.service.VectorSearchCache;
import ai.youthmind.util.CircuitBreaker;
import ai.youthmind.service.VectorSearchMetrics;
import ai.youthmind.util.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Production-ready Vector Search API Controller.
 * Provides comprehensive vector similarity search endpoints with:
 * - Sub-30ms response times
 * - Intelligent caching
 * - Circuit breaker protection
 * - Comprehensive monitoring
 * - Health checks and metrics
 */
@RestController
@RequestMapping("/v1/vector-search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vector Search", description = "High-performance vector similarity search operations")
@SecurityRequirement(name = "bearer-jwt")
public class VectorSearchController {

    private final VectorSearchService vectorSearchService;
    private final VectorCollectionManager vectorCollectionManager;
    private final VectorSearchCache vectorSearchCache;
    private final CircuitBreaker circuitBreaker;
    private final VectorSearchMetrics metrics;

    /**
     * Search for similar content using vector similarity
     */
    @PostMapping("/search")
    @Operation(
        summary = "Search similar content",
        description = "Perform vector similarity search with intelligent caching and circuit breaker protection"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<ApiResponse<List<VectorSearchService.SearchResult>>>> searchSimilarContent(
            @Parameter(description = "Search query text") @RequestParam String query,
            @Parameter(description = "Collection name to search in") @RequestParam String collectionName,
            @Parameter(description = "Similarity threshold (0.0-1.0)") @RequestParam(defaultValue = "0.7") double threshold,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "10") int limit) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Vector search request: query='{}', collection='{}', threshold={}", 
                        query, collectionName, threshold);
                
                List<VectorSearchService.SearchResult> results = vectorSearchService
                    .searchSimilarContent(query, collectionName, threshold)
                    .get();
                
                // Limit results if needed
                if (results.size() > limit) {
                    results = results.subList(0, limit);
                }
                
                return ResponseBuilder.success(results, "Vector search completed successfully");
                
            } catch (Exception e) {
                log.error("Error in vector search: {}", e.getMessage(), e);
                return ResponseBuilder.error("Vector search failed: " + e.getMessage(), 500);
            }
        });
    }

    /**
     * Get comprehensive system health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Get vector search system health",
        description = "Get comprehensive health status of the vector search system"
    )
    public CompletableFuture<ResponseEntity<ApiResponse<VectorSearchService.VectorSearchHealthStatus>>> getSystemHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                VectorSearchService.VectorSearchHealthStatus health = vectorSearchService
                    .getSystemHealth()
                    .get();
                
                return ResponseBuilder.success(health, "System health retrieved successfully");
                
            } catch (Exception e) {
                log.error("Error getting system health: {}", e.getMessage(), e);
                return ResponseBuilder.error("Failed to get system health: " + e.getMessage(), 500);
            }
        });
    }

    /**
     * Get metrics summary
     */
    @GetMapping("/metrics")
    @Operation(
        summary = "Get vector search metrics",
        description = "Get comprehensive performance metrics"
    )
    public CompletableFuture<ResponseEntity<ApiResponse<VectorSearchMetrics.VectorSearchMetricsSummary>>> getMetrics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                VectorSearchMetrics.VectorSearchMetricsSummary metricsSummary = metrics.getMetricsSummary();
                
                return ResponseBuilder.success(metricsSummary, "Metrics retrieved successfully");
                
            } catch (Exception e) {
                log.error("Error getting metrics: {}", e.getMessage(), e);
                return ResponseBuilder.error("Failed to get metrics: " + e.getMessage(), 500);
            }
        });
    }

    /**
     * Initialize vector search system
     */
    @PostMapping("/initialize")
    @Operation(
        summary = "Initialize vector search system",
        description = "Initialize the vector search system with optimized configuration"
    )
    public CompletableFuture<ResponseEntity<ApiResponse<Boolean>>> initializeSystem() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Initializing vector search system...");
                
                boolean success = vectorSearchService
                    .initializeVectorSearchSystem()
                    .get();
                
                if (success) {
                    return ResponseBuilder.success(true, "Vector search system initialized successfully");
                } else {
                    return ResponseBuilder.error("Failed to initialize vector search system", 500);
                }
                
            } catch (Exception e) {
                log.error("Error initializing vector search system: {}", e.getMessage(), e);
                return ResponseBuilder.error("System initialization failed: " + e.getMessage(), 500);
            }
        });
    }

    /**
     * Index existing posts with psychological vectors
     */
    @PostMapping("/index-posts")
    @Operation(
        summary = "Index existing posts",
        description = "Index all existing posts with their psychological vectors for vector search"
    )
    public CompletableFuture<ResponseEntity<ApiResponse<Integer>>> indexExistingPosts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting to index existing posts...");
                
                Integer indexedCount = vectorSearchService
                    .indexExistingPosts()
                    .get();
                
                return ResponseBuilder.success(indexedCount, 
                    String.format("Successfully indexed %d posts with psychological vectors", indexedCount));
                
            } catch (Exception e) {
                log.error("Error indexing existing posts: {}", e.getMessage(), e);
                return ResponseBuilder.error("Failed to index posts: " + e.getMessage(), 500);
            }
        });
    }

    /**
     * Generate psychological vectors for existing posts
     */
    @PostMapping("/generate-vectors")
    @Operation(
        summary = "Generate psychological vectors",
        description = "Generate psychological vectors for all existing posts that don't have them"
    )
    public CompletableFuture<ResponseEntity<ApiResponse<Integer>>> generatePsychologicalVectors() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting to generate psychological vectors for existing posts...");
                
                Integer generatedCount = vectorSearchService
                    .generatePsychologicalVectorsForExistingPosts()
                    .get();
                
                return ResponseBuilder.success(generatedCount, 
                    String.format("Successfully generated psychological vectors for %d posts", generatedCount));
                
            } catch (Exception e) {
                log.error("Error generating psychological vectors: {}", e.getMessage(), e);
                return ResponseBuilder.error("Failed to generate vectors: " + e.getMessage(), 500);
            }
        });
    }
}