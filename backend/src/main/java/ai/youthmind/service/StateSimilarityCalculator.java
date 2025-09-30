package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Sophisticated State Similarity Calculator with multiple distance metrics.
 * Provides cosine similarity, Euclidean distance, Manhattan distance,
 * and other similarity calculations for psychological state analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateSimilarityCalculator {

    /**
     * Calculate comprehensive similarity metrics between two state vectors
     */
    public CompletableFuture<StateSimilarityMetrics> calculateSimilarity(
            List<Float> vector1, List<Float> vector2) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (vector1.size() != vector2.size()) {
                    throw new IllegalArgumentException("Vector dimensions must match");
                }
                
                double cosineSimilarity = calculateCosineSimilarity(vector1, vector2);
                double euclideanDistance = calculateEuclideanDistance(vector1, vector2);
                double manhattanDistance = calculateManhattanDistance(vector1, vector2);
                double jaccardSimilarity = calculateJaccardSimilarity(vector1, vector2);
                
                return StateSimilarityMetrics.builder()
                    .cosineSimilarity(cosineSimilarity)
                    .euclideanDistance(euclideanDistance)
                    .manhattanDistance(manhattanDistance)
                    .jaccardSimilarity(jaccardSimilarity)
                    .build();
                    
            } catch (Exception e) {
                log.error("Error calculating similarity metrics: {}", e.getMessage(), e);
                return StateSimilarityMetrics.builder()
                    .cosineSimilarity(0.0)
                    .euclideanDistance(Double.MAX_VALUE)
                    .manhattanDistance(Double.MAX_VALUE)
                    .jaccardSimilarity(0.0)
                    .build();
            }
        });
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Calculate Euclidean distance between two vectors
     */
    private double calculateEuclideanDistance(List<Float> vector1, List<Float> vector2) {
        double sumSquaredDiff = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            double diff = vector1.get(i) - vector2.get(i);
            sumSquaredDiff += diff * diff;
        }
        
        return Math.sqrt(sumSquaredDiff);
    }

    /**
     * Calculate Manhattan distance between two vectors
     */
    private double calculateManhattanDistance(List<Float> vector1, List<Float> vector2) {
        double sumAbsDiff = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            sumAbsDiff += Math.abs(vector1.get(i) - vector2.get(i));
        }
        
        return sumAbsDiff;
    }

    /**
     * Calculate Jaccard similarity between two vectors
     */
    private double calculateJaccardSimilarity(List<Float> vector1, List<Float> vector2) {
        // Convert vectors to binary sets for Jaccard calculation
        Set<Integer> set1 = new HashSet<>();
        Set<Integer> set2 = new HashSet<>();
        
        for (int i = 0; i < vector1.size(); i++) {
            if (vector1.get(i) > 0.5) set1.add(i);
            if (vector2.get(i) > 0.5) set2.add(i);
        }
        
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) {
            return 1.0; // Both sets are empty
        }
        
        return (double) intersection.size() / union.size();
    }

    /**
     * Calculate weighted similarity based on multiple metrics
     */
    public CompletableFuture<Double> calculateWeightedSimilarity(
            StateSimilarityMetrics metrics, SimilarityWeights weights) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                double weightedScore = 
                    metrics.getCosineSimilarity() * weights.getCosineWeight() +
                    (1.0 / (1.0 + metrics.getEuclideanDistance())) * weights.getEuclideanWeight() +
                    (1.0 / (1.0 + metrics.getManhattanDistance())) * weights.getManhattanWeight() +
                    metrics.getJaccardSimilarity() * weights.getJaccardWeight();
                
                return Math.max(0.0, Math.min(1.0, weightedScore));
                
            } catch (Exception e) {
                log.error("Error calculating weighted similarity: {}", e.getMessage(), e);
                return 0.0;
            }
        });
    }

    /**
     * Find most similar vector from a collection
     */
    public CompletableFuture<SimilarityMatch> findMostSimilar(
            List<Float> queryVector, 
            Map<String, List<Float>> candidateVectors,
            SimilarityWeights weights) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String bestMatch = null;
                double bestScore = -1.0;
                StateSimilarityMetrics bestMetrics = null;
                
                for (Map.Entry<String, List<Float>> entry : candidateVectors.entrySet()) {
                    StateSimilarityMetrics metrics = calculateSimilarity(queryVector, entry.getValue()).get();
                    double weightedScore = calculateWeightedSimilarity(metrics, weights).get();
                    
                    if (weightedScore > bestScore) {
                        bestScore = weightedScore;
                        bestMatch = entry.getKey();
                        bestMetrics = metrics;
                    }
                }
                
                return SimilarityMatch.builder()
                    .matchId(bestMatch)
                    .similarityScore(bestScore)
                    .metrics(bestMetrics)
                    .build();
                    
            } catch (Exception e) {
                log.error("Error finding most similar vector: {}", e.getMessage(), e);
                return SimilarityMatch.builder()
                    .matchId(null)
                    .similarityScore(0.0)
                    .metrics(null)
                    .build();
            }
        });
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class StateSimilarityMetrics {
        private double cosineSimilarity;
        private double euclideanDistance;
        private double manhattanDistance;
        private double jaccardSimilarity;
    }

    @lombok.Data
    @lombok.Builder
    public static class SimilarityWeights {
        private double cosineWeight = 0.4;
        private double euclideanWeight = 0.3;
        private double manhattanWeight = 0.2;
        private double jaccardWeight = 0.1;
    }

    @lombok.Data
    @lombok.Builder
    public static class SimilarityMatch {
        private String matchId;
        private double similarityScore;
        private StateSimilarityMetrics metrics;
    }
}
