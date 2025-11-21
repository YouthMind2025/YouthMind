package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced Vector Normalization Service with multiple normalization strategies.
 * Provides L2, Min-Max, Z-score, and other normalization algorithms
 * for psychological state vector processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorNormalizationService {

    /**
     * Normalize vector using specified strategy
     */
    public CompletableFuture<List<Float>> normalizeVector(List<Float> vector, NormalizationStrategy strategy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (strategy) {
                    case L2:
                        return normalizeL2(vector);
                    case MIN_MAX:
                        return normalizeMinMax(vector);
                    case Z_SCORE:
                        return normalizeZScore(vector);
                    case ROBUST_SCALER:
                        return normalizeRobustScaler(vector);
                    case UNIT_VECTOR:
                        return normalizeUnitVector(vector);
                    default:
                        return normalizeL2(vector);
                }
            } catch (Exception e) {
                log.error("Error normalizing vector: {}", e.getMessage(), e);
                return new ArrayList<>(vector); // Return original vector on error
            }
        });
    }

    /**
     * L2 normalization (Euclidean norm)
     */
    private List<Float> normalizeL2(List<Float> vector) {
        double norm = Math.sqrt(vector.stream()
            .mapToDouble(v -> v * v)
            .sum());
        
        if (norm == 0) {
            return new ArrayList<>(vector);
        }
        
        return vector.stream()
            .map(v -> (float) (v / norm))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Min-Max normalization to [0,1] range
     */
    private List<Float> normalizeMinMax(List<Float> vector) {
        if (vector.isEmpty()) {
            return new ArrayList<>();
        }
        
        float min = Collections.min(vector);
        float max = Collections.max(vector);
        
        if (max == min) {
            return vector.stream()
                .map(v -> 0.5f)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        return vector.stream()
            .map(v -> (v - min) / (max - min))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Z-score normalization (standardization)
     */
    private List<Float> normalizeZScore(List<Float> vector) {
        if (vector.isEmpty()) {
            return new ArrayList<>();
        }
        
        double mean = vector.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        double variance = vector.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        if (stdDev == 0) {
            return vector.stream()
                .map(v -> 0.0f)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        return vector.stream()
            .map(v -> (float) ((v - mean) / stdDev))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Robust scaler normalization using median and IQR
     */
    private List<Float> normalizeRobustScaler(List<Float> vector) {
        if (vector.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Float> sortedVector = new ArrayList<>(vector);
        Collections.sort(sortedVector);
        
        float median = calculateMedian(sortedVector);
        float q1 = calculatePercentile(sortedVector, 25);
        float q3 = calculatePercentile(sortedVector, 75);
        float iqr = q3 - q1;
        
        if (iqr == 0) {
            return vector.stream()
                .map(v -> 0.0f)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        return vector.stream()
            .map(v -> (v - median) / iqr)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Unit vector normalization (magnitude = 1)
     */
    private List<Float> normalizeUnitVector(List<Float> vector) {
        double magnitude = Math.sqrt(vector.stream()
            .mapToDouble(v -> v * v)
            .sum());
        
        if (magnitude == 0) {
            return new ArrayList<>(vector);
        }
        
        return vector.stream()
            .map(v -> (float) (v / magnitude))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Calculate median of sorted vector
     */
    private float calculateMedian(List<Float> sortedVector) {
        int size = sortedVector.size();
        if (size % 2 == 0) {
            return (sortedVector.get(size / 2 - 1) + sortedVector.get(size / 2)) / 2.0f;
        } else {
            return sortedVector.get(size / 2);
        }
    }

    /**
     * Calculate percentile of sorted vector
     */
    private float calculatePercentile(List<Float> sortedVector, int percentile) {
        int size = sortedVector.size();
        int index = (int) Math.ceil((percentile / 100.0) * size) - 1;
        return sortedVector.get(Math.max(0, Math.min(index, size - 1)));
    }

    /**
     * Normalization strategies enum
     */
    public enum NormalizationStrategy {
        L2,           // Euclidean norm
        MIN_MAX,       // Min-Max scaling to [0,1]
        Z_SCORE,       // Standardization (mean=0, std=1)
        ROBUST_SCALER, // Median and IQR based
        UNIT_VECTOR    // Unit vector (magnitude=1)
    }
}
