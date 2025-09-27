package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Vector Clipping Service for maintaining values within specified ranges.
 * Provides sophisticated clipping algorithms for psychological state vectors
 * to maintain values within [0,1] range and other constraints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorClippingService {

    // Default clipping parameters
    private static final float DEFAULT_MIN_VALUE = 0.0f;
    private static final float DEFAULT_MAX_VALUE = 1.0f;
    private static final float SOFT_CLIPPING_FACTOR = 0.1f;

    /**
     * Clip vector to [0,1] range using hard clipping
     */
    public CompletableFuture<List<Float>> clipVector(List<Float> vector) {
        return clipVector(vector, DEFAULT_MIN_VALUE, DEFAULT_MAX_VALUE, ClippingStrategy.HARD);
    }

    /**
     * Clip vector using specified strategy and range
     */
    public CompletableFuture<List<Float>> clipVector(
            List<Float> vector, 
            float minValue, 
            float maxValue, 
            ClippingStrategy strategy) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (strategy) {
                    case HARD:
                        return hardClip(vector, minValue, maxValue);
                    case SOFT:
                        return softClip(vector, minValue, maxValue);
                    case TANH:
                        return tanhClip(vector, minValue, maxValue);
                    case SIGMOID:
                        return sigmoidClip(vector, minValue, maxValue);
                    default:
                        return hardClip(vector, minValue, maxValue);
                }
            } catch (Exception e) {
                log.error("Error clipping vector: {}", e.getMessage(), e);
                return new ArrayList<>(vector); // Return original vector on error
            }
        });
    }

    /**
     * Hard clipping: values outside range are set to min/max
     */
    private List<Float> hardClip(List<Float> vector, float minValue, float maxValue) {
        return vector.stream()
            .map(v -> Math.max(minValue, Math.min(maxValue, v)))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Soft clipping: gradual compression of values outside range
     */
    private List<Float> softClip(List<Float> vector, float minValue, float maxValue) {
        return vector.stream()
            .map(v -> {
                if (v < minValue) {
                    return minValue + SOFT_CLIPPING_FACTOR * (v - minValue);
                } else if (v > maxValue) {
                    return maxValue + SOFT_CLIPPING_FACTOR * (v - maxValue);
                } else {
                    return v;
                }
            })
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Tanh clipping: uses hyperbolic tangent for smooth clipping
     */
    private List<Float> tanhClip(List<Float> vector, float minValue, float maxValue) {
        float range = maxValue - minValue;
        float center = (maxValue + minValue) / 2.0f;
        
        return vector.stream()
            .map(v -> {
                // Normalize to [-1, 1] range
                float normalized = (v - center) / (range / 2.0f);
                // Apply tanh and scale back
                float clipped = (float) Math.tanh(normalized);
                return center + clipped * (range / 2.0f);
            })
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Sigmoid clipping: uses sigmoid function for smooth clipping
     */
    private List<Float> sigmoidClip(List<Float> vector, float minValue, float maxValue) {
        return vector.stream()
            .map(v -> {
                // Normalize to [0, 1] range
                float normalized = (v - minValue) / (maxValue - minValue);
                // Apply sigmoid
                float sigmoid = (float) (1.0 / (1.0 + Math.exp(-normalized)));
                // Scale back to original range
                return minValue + sigmoid * (maxValue - minValue);
            })
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Adaptive clipping based on vector statistics
     */
    public CompletableFuture<List<Float>> adaptiveClip(List<Float> vector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (vector.isEmpty()) {
                    return new ArrayList<>();
                }
                
                // Calculate statistics
                float mean = (float) vector.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
                float stdDev = calculateStandardDeviation(vector, mean);
                
                // Determine clipping range based on statistics
                float adaptiveMin = Math.max(DEFAULT_MIN_VALUE, mean - 3 * stdDev);
                float adaptiveMax = Math.min(DEFAULT_MAX_VALUE, mean + 3 * stdDev);
                
                // Use soft clipping for adaptive range
                return softClip(vector, adaptiveMin, adaptiveMax);
                
            } catch (Exception e) {
                log.error("Error in adaptive clipping: {}", e.getMessage(), e);
                return hardClip(vector, DEFAULT_MIN_VALUE, DEFAULT_MAX_VALUE);
            }
        });
    }

    /**
     * Calculate standard deviation
     */
    private float calculateStandardDeviation(List<Float> vector, float mean) {
        double variance = vector.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return (float) Math.sqrt(variance);
    }

    /**
     * Clipping strategies enum
     */
    public enum ClippingStrategy {
        HARD,    // Hard clipping to min/max values
        SOFT,    // Soft clipping with gradual compression
        TANH,    // Tanh-based smooth clipping
        SIGMOID  // Sigmoid-based smooth clipping
    }
}
