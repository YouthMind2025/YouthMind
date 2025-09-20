package ai.youthmind.service;

import ai.youthmind.config.QdrantConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST-based Qdrant client for vector database operations.
 * Uses Qdrant's REST API instead of gRPC for better stability and version independence.
 */
@Slf4j
@Service
public class QdrantRestClient {

    @Autowired
    private QdrantConfig qdrantConfig;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a collection using REST API.
     */
    public CompletableFuture<Boolean> createCollection(String collectionName, int vectorSize, String distanceMetric) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections/" + collectionName;
                
                Map<String, Object> requestBody = Map.of(
                    "vectors", Map.of(
                        "size", vectorSize,
                        "distance", distanceMetric.toUpperCase()
                    ),
                    "optimizers_config", Map.of(
                        "default_segment_number", qdrantConfig.getHnswM()
                    ),
                    "hnsw_config", Map.of(
                        "m", qdrantConfig.getHnswM(),
                        "ef_construct", qdrantConfig.getHnswEfConstruct()
                    )
                );

                HttpHeaders headers = createHeaders();
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class
                );

                boolean success = response.getStatusCode().is2xxSuccessful();
                log.info("Collection {} creation: {}", collectionName, success ? "SUCCESS" : "FAILED");
                return success;

            } catch (Exception e) {
                log.error("Failed to create collection {}: {}", collectionName, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Check if a collection exists.
     */
    public CompletableFuture<Boolean> collectionExists(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections/" + collectionName;
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
                );

                return response.getStatusCode().is2xxSuccessful();

            } catch (Exception e) {
                log.debug("Collection {} does not exist: {}", collectionName, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get collection information.
     */
    public CompletableFuture<Map<String, Object>> getCollectionInfo(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections/" + collectionName;
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    return objectMapper.readValue(response.getBody(), Map.class);
                }

                return Map.of();

            } catch (Exception e) {
                log.error("Failed to get collection info for {}: {}", collectionName, e.getMessage(), e);
                return Map.of();
            }
        });
    }

    /**
     * List all collections.
     */
    public CompletableFuture<List<String>> listCollections() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections";
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                    List<Map<String, Object>> collections = (List<Map<String, Object>>) result.get("result");
                    return collections.stream()
                        .map(col -> (String) col.get("name"))
                        .toList();
                }

                return List.of();

            } catch (Exception e) {
                log.error("Failed to list collections: {}", e.getMessage(), e);
                return List.of();
            }
        });
    }

    /**
     * Search for similar vectors.
     */
    public CompletableFuture<List<Map<String, Object>>> searchVectors(
            String collectionName, 
            List<Float> queryVector, 
            int limit, 
            double threshold) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections/" + collectionName + "/points/search";
                
                Map<String, Object> requestBody = Map.of(
                    "vector", queryVector,
                    "limit", limit,
                    "score_threshold", threshold,
                    "with_payload", true,
                    "with_vector", false
                );

                HttpHeaders headers = createHeaders();
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                    return (List<Map<String, Object>>) result.get("result");
                }

                return List.of();

            } catch (Exception e) {
                log.error("Failed to search vectors in collection {}: {}", collectionName, e.getMessage(), e);
                return List.of();
            }
        });
    }

    /**
     * Upsert vectors (insert or update).
     */
    public CompletableFuture<Boolean> upsertVectors(
            String collectionName, 
            List<Map<String, Object>> points) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections/" + collectionName + "/points";
                
                Map<String, Object> requestBody = Map.of(
                    "points", points
                );

                HttpHeaders headers = createHeaders();
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class
                );

                boolean success = response.getStatusCode().is2xxSuccessful();
                log.debug("Upsert vectors in collection {}: {}", collectionName, success ? "SUCCESS" : "FAILED");
                return success;

            } catch (Exception e) {
                log.error("Failed to upsert vectors in collection {}: {}", collectionName, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Delete a collection.
     */
    public CompletableFuture<Boolean> deleteCollection(String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections/" + collectionName;
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, String.class
                );

                boolean success = response.getStatusCode().is2xxSuccessful();
                log.info("Collection {} deletion: {}", collectionName, success ? "SUCCESS" : "FAILED");
                return success;

            } catch (Exception e) {
                log.error("Failed to delete collection {}: {}", collectionName, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Get cluster information for health checks.
     */
    public CompletableFuture<Map<String, Object>> getClusterInfo() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/cluster";
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    return objectMapper.readValue(response.getBody(), Map.class);
                }

                return Map.of();

            } catch (Exception e) {
                log.error("Failed to get cluster info: {}", e.getMessage(), e);
                return Map.of();
            }
        });
    }

    /**
     * Test connection to Qdrant.
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = qdrantConfig.getConnectionUrl() + "/collections";
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
                );

                return response.getStatusCode().is2xxSuccessful();

            } catch (Exception e) {
                log.warn("Connection test failed: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Create HTTP headers with API key if configured.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        
        if (qdrantConfig.getApiKey() != null && !qdrantConfig.getApiKey().isEmpty()) {
            headers.set("api-key", qdrantConfig.getApiKey());
        }
        
        return headers;
    }
}
