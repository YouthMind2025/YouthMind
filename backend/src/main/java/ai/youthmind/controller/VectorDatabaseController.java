package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.service.VectorDatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Vector Database operations using REST API.
 */
@Slf4j
@RestController
@RequestMapping("/api/vector-db")
public class VectorDatabaseController {

    @Autowired
    private VectorDatabaseService vectorDatabaseService;

    /**
     * Initialize the vector database with default collections.
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializeDatabase() {
        try {
            boolean success = vectorDatabaseService.initializeDatabase().get();
            
            Map<String, Object> result = Map.of(
                "initialized", success,
                "message", success ? "Vector database initialized successfully" : "Failed to initialize vector database"
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("Error initializing vector database: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(Map.of("error", "Failed to initialize vector database: " + e.getMessage()), 500));
        }
    }

    /**
     * Get database health status.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> getHealthStatus() {
        try {
            VectorDatabaseService.DatabaseHealthStatus health = vectorDatabaseService.getHealthStatus().get();
            return ResponseEntity.ok(ApiResponse.success(health));

        } catch (Exception e) {
            log.error("Error getting health status: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(Map.of("error", "Failed to get health status: " + e.getMessage()), 500));
        }
    }

    /**
     * Get collection statistics.
     */
    @GetMapping("/collections/{collectionName}/stats")
    public ResponseEntity<ApiResponse<Object>> getCollectionStats(
            @PathVariable String collectionName) {
        try {
            VectorDatabaseService.CollectionStats stats = vectorDatabaseService.getCollectionStats(collectionName).get();
            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            log.error("Error getting collection stats: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(Map.of("error", "Failed to get collection stats: " + e.getMessage()), 500));
        }
    }

    /**
     * Search for similar vectors.
     */
    @PostMapping("/collections/{collectionName}/search")
    public ResponseEntity<ApiResponse<Object>> searchSimilar(
            @PathVariable String collectionName,
            @RequestBody SearchRequest request) {
        try {
            List<VectorDatabaseService.VectorSearchResult> results = vectorDatabaseService.searchSimilarContent(
                collectionName,
                request.getVector(),
                request.getLimit(),
                request.getThreshold()
            ).get();
            
            return ResponseEntity.ok(ApiResponse.success(results));

        } catch (Exception e) {
            log.error("Error searching vectors: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(Map.of("error", "Failed to search vectors: " + e.getMessage()), 500));
        }
    }

    /**
     * Store a vector embedding.
     */
    @PostMapping("/collections/{collectionName}/vectors")
    public ResponseEntity<ApiResponse<Map<String, Object>>> storeVector(
            @PathVariable String collectionName,
            @RequestBody StoreVectorRequest request) {
        try {
            boolean success = vectorDatabaseService.storeVector(
                collectionName,
                request.getPointId(),
                request.getVector(),
                request.getPayload()
            ).get();
            
            Map<String, Object> result = Map.of(
                "stored", success,
                "pointId", request.getPointId(),
                "message", success ? "Vector stored successfully" : "Failed to store vector"
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("Error storing vector: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(Map.of("error", "Failed to store vector: " + e.getMessage()), 500));
        }
    }

    /**
     * Test endpoint for comprehensive vector database testing.
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testVectorDatabase() {
        try {
            log.info("Starting comprehensive vector database test...");
            
            // Test 1: Initialize database
            boolean initSuccess = vectorDatabaseService.initializeDatabase().get();
            log.info("Database initialization: {}", initSuccess ? "SUCCESS" : "FAILED");
            
            // Test 2: Health check
            VectorDatabaseService.DatabaseHealthStatus health = vectorDatabaseService.getHealthStatus().get();
            log.info("Health check: {}", health.isConnectionHealthy() ? "SUCCESS" : "FAILED");
            
            // Test 3: Store test vector
            List<Float> testVector = List.of(0.1f, 0.2f, 0.3f, 0.4f);
            Map<String, Object> testPayload = Map.of(
                "content", "test content",
                "type", "test",
                "timestamp", System.currentTimeMillis()
            );
            
            boolean storeSuccess = vectorDatabaseService.storeVector(
                "posts", "test-point-1", testVector, testPayload
            ).get();
            log.info("Vector storage: {}", storeSuccess ? "SUCCESS" : "FAILED");
            
            // Test 4: Search test
            List<VectorDatabaseService.VectorSearchResult> searchResults = vectorDatabaseService.searchSimilarContent(
                "posts", testVector, 5, 0.5
            ).get();
            log.info("Vector search: {} results found", searchResults.size());
            
            // Test 5: Collection stats
            VectorDatabaseService.CollectionStats stats = vectorDatabaseService.getCollectionStats("posts").get();
            log.info("Collection stats: {}", stats.isExists() ? "SUCCESS" : "FAILED");
            
            boolean overallSuccess = initSuccess && health.isConnectionHealthy() && storeSuccess;
            
            Map<String, Object> testResults = Map.of(
                "overallSuccess", overallSuccess,
                "tests", Map.of(
                    "initialization", initSuccess,
                    "healthCheck", health.isConnectionHealthy(),
                    "vectorStorage", storeSuccess,
                    "vectorSearch", !searchResults.isEmpty(),
                    "collectionStats", stats.isExists()
                ),
                "healthStatus", health,
                "collectionStats", stats,
                "searchResultsCount", searchResults.size(),
                "message", overallSuccess ? "All tests passed successfully" : "Some tests failed"
            );
            
            log.info("Vector database test completed: {}", overallSuccess ? "SUCCESS" : "FAILED");
            return ResponseEntity.ok(ApiResponse.success(testResults));

        } catch (Exception e) {
            log.error("Error during vector database test: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(Map.of("error", "Test failed: " + e.getMessage()), 500));
        }
    }

    // Request DTOs
    @lombok.Data
    public static class SearchRequest {
        private List<Float> vector;
        private int limit = 10;
        private double threshold = 0.7;
    }

    @lombok.Data
    public static class StoreVectorRequest {
        private String pointId;
        private List<Float> vector;
        private Map<String, Object> payload;
    }
}
