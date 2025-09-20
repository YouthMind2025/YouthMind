package ai.youthmind.service;

import ai.youthmind.config.QdrantConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified Vector Database Service using REST API.
 * Provides high-level operations for vector database management.
 */
@Slf4j
@Service
public class VectorDatabaseService {

    @Autowired
    private QdrantRestClient qdrantRestClient;

    @Autowired
    private QdrantConfig qdrantConfig;

    /**
     * Initialize vector database with default collections.
     */
    public CompletableFuture<Boolean> initializeDatabase() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Initializing vector database with default collections...");
                
                boolean allSuccess = true;
                
                for (QdrantConfig.CollectionConfig config : qdrantConfig.getCollections().values()) {
                    boolean exists = qdrantRestClient.collectionExists(config.getName()).get();
                    
                    if (!exists) {
                        boolean created = qdrantRestClient.createCollection(
                            config.getName(),
                            config.getVectorSize(),
                            config.getDistanceMetric()
                        ).get();
                        
                        if (created) {
                            log.info("Created collection: {}", config.getName());
                        } else {
                            log.error("Failed to create collection: {}", config.getName());
                            allSuccess = false;
                        }
                    } else {
                        log.info("Collection already exists: {}", config.getName());
                    }
                }
                
                log.info("Vector database initialization: {}", allSuccess ? "SUCCESS" : "FAILED");
                return allSuccess;

            } catch (Exception e) {
                log.error("Error initializing vector database: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Search for similar content.
     */
    public CompletableFuture<List<VectorSearchResult>> searchSimilarContent(
            String collectionName,
            List<Float> queryVector,
            int limit,
            double threshold) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> results = qdrantRestClient.searchVectors(
                    collectionName, queryVector, limit, threshold
                ).get();
                
                return results.stream()
                    .map(this::convertToSearchResult)
                    .toList();

            } catch (Exception e) {
                log.error("Error searching similar content: {}", e.getMessage(), e);
                return List.of();
            }
        });
    }

    /**
     * Store vector embedding.
     */
    public CompletableFuture<Boolean> storeVector(
            String collectionName,
            String pointId,
            List<Float> vector,
            Map<String, Object> payload) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> point = Map.of(
                    "id", pointId,
                    "vector", vector,
                    "payload", payload
                );
                
                return qdrantRestClient.upsertVectors(collectionName, List.of(point)).get();

            } catch (Exception e) {
                log.error("Error storing vector: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Get database health status.
     */
    public CompletableFuture<DatabaseHealthStatus> getHealthStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean connectionHealthy = qdrantRestClient.testConnection().get();
                List<String> collections = qdrantRestClient.listCollections().get();
                Map<String, Object> clusterInfo = qdrantRestClient.getClusterInfo().get();
                
                return DatabaseHealthStatus.builder()
                    .connectionHealthy(connectionHealthy)
                    .collectionsCount(collections.size())
                    .availableCollections(collections)
                    .clusterInfo(clusterInfo)
                    .timestamp(System.currentTimeMillis())
                    .build();

            } catch (Exception e) {
                log.error("Error getting health status: {}", e.getMessage(), e);
                return DatabaseHealthStatus.builder()
                    .connectionHealthy(false)
                    .collectionsCount(0)
                    .availableCollections(List.of())
                    .clusterInfo(Map.of())
                    .timestamp(System.currentTimeMillis())
                    .error(e.getMessage())
                    .build();
            }
        });
    }

    /**
     * Get collection statistics.
     */
    public CompletableFuture<CollectionStats> getCollectionStats(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> info = qdrantRestClient.getCollectionInfo(collectionName).get();
                
                if (info.isEmpty()) {
                    return CollectionStats.builder()
                        .collectionName(collectionName)
                        .exists(false)
                        .build();
                }
                
                Map<String, Object> result = (Map<String, Object>) info.get("result");
                Map<String, Object> status = (Map<String, Object>) result.get("status");
                Map<String, Object> vectorsCount = (Map<String, Object>) result.get("vectors_count");
                Map<String, Object> config = (Map<String, Object>) result.get("config");
                Map<String, Object> params = (Map<String, Object>) config.get("params");
                
                return CollectionStats.builder()
                    .collectionName(collectionName)
                    .exists(true)
                    .status((String) status.get("ok"))
                    .pointsCount(((Number) vectorsCount.get("count")).longValue())
                    .vectorSize(((Number) params.get("size")).intValue())
                    .distanceMetric((String) params.get("distance"))
                    .build();

            } catch (Exception e) {
                log.error("Error getting collection stats for {}: {}", collectionName, e.getMessage(), e);
                return CollectionStats.builder()
                    .collectionName(collectionName)
                    .exists(false)
                    .error(e.getMessage())
                    .build();
            }
        });
    }

    /**
     * Convert search result to our DTO.
     */
    private VectorSearchResult convertToSearchResult(Map<String, Object> result) {
        return VectorSearchResult.builder()
            .pointId((String) result.get("id"))
            .score(((Number) result.get("score")).doubleValue())
            .payload((Map<String, Object>) result.get("payload"))
            .build();
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class VectorSearchResult {
        private String pointId;
        private double score;
        private Map<String, Object> payload;
    }

    @lombok.Data
    @lombok.Builder
    public static class DatabaseHealthStatus {
        private boolean connectionHealthy;
        private int collectionsCount;
        private List<String> availableCollections;
        private Map<String, Object> clusterInfo;
        private long timestamp;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class CollectionStats {
        private String collectionName;
        private boolean exists;
        private String status;
        private long pointsCount;
        private int vectorSize;
        private String distanceMetric;
        private String error;
    }
}
