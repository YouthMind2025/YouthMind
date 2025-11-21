package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * State Vector Smoothing Service for noise reduction and signal processing.
 * Provides sophisticated smoothing algorithms for psychological state vectors
 * to reduce noise and improve signal quality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateVectorSmoothingService {

    // Smoothing parameters
    private static final double DEFAULT_SMOOTHING_FACTOR = 0.2;
    private static final int DEFAULT_WINDOW_SIZE = 3;
    private static final double MIN_SMOOTHING_FACTOR = 0.0;
    private static final double MAX_SMOOTHING_FACTOR = 1.0;

    /**
     * Smooth vector using specified smoothing factor
     */
    public CompletableFuture<List<Float>> smoothVector(List<Float> vector, double smoothingFactor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (vector == null || vector.isEmpty()) {
                    return new ArrayList<>();
                }
                
                // Clamp smoothing factor to valid range
                final double finalSmoothingFactor = Math.max(MIN_SMOOTHING_FACTOR, Math.min(MAX_SMOOTHING_FACTOR, smoothingFactor));
                
                if (finalSmoothingFactor == 0.0) {
                    return new ArrayList<>(vector); // No smoothing
                }
                
                // Apply exponential smoothing
                return applyExponentialSmoothing(vector, finalSmoothingFactor);
                
            } catch (Exception e) {
                log.error("Error smoothing vector: {}", e.getMessage(), e);
                return new ArrayList<>(vector); // Return original vector on error
            }
        });
    }

    /**
     * Apply exponential smoothing to vector (synchronous version)
     */
    private List<Float> applyExponentialSmoothingSync(List<Float> vector, double smoothingFactor) {
        try {
            if (vector == null || vector.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Clamp smoothing factor to valid range
            smoothingFactor = Math.max(MIN_SMOOTHING_FACTOR, Math.min(MAX_SMOOTHING_FACTOR, smoothingFactor));
            
            if (smoothingFactor == 0.0) {
                return new ArrayList<>(vector); // No smoothing
            }
            
            // Apply exponential smoothing
            List<Float> smoothed = new ArrayList<>();
            smoothed.add(vector.get(0)); // First value unchanged
            
            for (int i = 1; i < vector.size(); i++) {
                float previousSmoothed = smoothed.get(i - 1);
                float currentValue = vector.get(i);
                
                // Exponential smoothing formula: S_t = α * X_t + (1 - α) * S_{t-1}
                float smoothedValue = (float) (smoothingFactor * currentValue + (1 - smoothingFactor) * previousSmoothed);
                smoothed.add(smoothedValue);
            }
            
            return smoothed;
            
        } catch (Exception e) {
            log.error("Error smoothing vector: {}", e.getMessage(), e);
            return new ArrayList<>(vector); // Return original vector on error
        }
    }

    /**
     * Apply exponential smoothing to vector
     */
    private List<Float> applyExponentialSmoothing(List<Float> vector, double smoothingFactor) {
        List<Float> smoothed = new ArrayList<>();
        smoothed.add(vector.get(0)); // First value unchanged
        
        for (int i = 1; i < vector.size(); i++) {
            float previousSmoothed = smoothed.get(i - 1);
            float currentValue = vector.get(i);
            
            // Exponential smoothing formula: S_t = α * X_t + (1 - α) * S_{t-1}
            float smoothedValue = (float) (smoothingFactor * currentValue + (1 - smoothingFactor) * previousSmoothed);
            smoothed.add(smoothedValue);
        }
        
        return smoothed;
    }

    /**
     * Apply moving average smoothing
     */
    public CompletableFuture<List<Float>> applyMovingAverageSmoothing(List<Float> vector, int windowSize) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (vector == null || vector.isEmpty()) {
                    return new ArrayList<>();
                }
                
                if (windowSize <= 1) {
                    return new ArrayList<>(vector); // No smoothing
                }
                
                List<Float> smoothed = new ArrayList<>();
                
                for (int i = 0; i < vector.size(); i++) {
                    int start = Math.max(0, i - windowSize / 2);
                    int end = Math.min(vector.size(), i + windowSize / 2 + 1);
                    
                    double sum = 0.0;
                    int count = 0;
                    
                    for (int j = start; j < end; j++) {
                        sum += vector.get(j);
                        count++;
                    }
                    
                    smoothed.add((float) (sum / count));
                }
                
                return smoothed;
                
            } catch (Exception e) {
                log.error("Error applying moving average smoothing: {}", e.getMessage(), e);
                return new ArrayList<>(vector);
            }
        });
    }

    /**
     * Apply Gaussian smoothing (synchronous version)
     */
    private List<Float> applyGaussianSmoothingSync(List<Float> vector, double sigma) {
        try {
            if (vector == null || vector.isEmpty()) {
                return new ArrayList<>();
            }
            
            if (sigma <= 0) {
                return new ArrayList<>(vector); // No smoothing
            }
            
            List<Float> smoothed = new ArrayList<>();
            int windowSize = (int) Math.ceil(3 * sigma); // 3-sigma rule
            
            for (int i = 0; i < vector.size(); i++) {
                double weightedSum = 0.0;
                double weightSum = 0.0;
                
                for (int j = Math.max(0, i - windowSize); j <= Math.min(vector.size() - 1, i + windowSize); j++) {
                    double distance = Math.abs(i - j);
                    double weight = Math.exp(-(distance * distance) / (2 * sigma * sigma));
                    
                    weightedSum += vector.get(j) * weight;
                    weightSum += weight;
                }
                
                smoothed.add((float) (weightedSum / weightSum));
            }
            
            return smoothed;
            
        } catch (Exception e) {
            log.error("Error applying Gaussian smoothing: {}", e.getMessage(), e);
            return new ArrayList<>(vector);
        }
    }

    /**
     * Apply Gaussian smoothing
     */
    public CompletableFuture<List<Float>> applyGaussianSmoothing(List<Float> vector, double sigma) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (vector == null || vector.isEmpty()) {
                    return new ArrayList<>();
                }
                
                if (sigma <= 0) {
                    return new ArrayList<>(vector); // No smoothing
                }
                
                List<Float> smoothed = new ArrayList<>();
                int windowSize = (int) Math.ceil(3 * sigma); // 3-sigma rule
                
                for (int i = 0; i < vector.size(); i++) {
                    double weightedSum = 0.0;
                    double weightSum = 0.0;
                    
                    for (int j = Math.max(0, i - windowSize); j <= Math.min(vector.size() - 1, i + windowSize); j++) {
                        double distance = Math.abs(i - j);
                        double weight = Math.exp(-(distance * distance) / (2 * sigma * sigma));
                        
                        weightedSum += vector.get(j) * weight;
                        weightSum += weight;
                    }
                    
                    smoothed.add((float) (weightedSum / weightSum));
                }
                
                return smoothed;
                
            } catch (Exception e) {
                log.error("Error applying Gaussian smoothing: {}", e.getMessage(), e);
                return new ArrayList<>(vector);
            }
        });
    }

    /**
     * Apply median filtering for outlier removal (synchronous version)
     */
    private List<Float> applyMedianFilteringSync(List<Float> vector, int windowSize) {
        try {
            if (vector == null || vector.isEmpty()) {
                return new ArrayList<>();
            }
            
            if (windowSize <= 1) {
                return new ArrayList<>(vector); // No filtering
            }
            
            List<Float> filtered = new ArrayList<>();
            
            for (int i = 0; i < vector.size(); i++) {
                int start = Math.max(0, i - windowSize / 2);
                int end = Math.min(vector.size(), i + windowSize / 2 + 1);
                
                List<Float> window = new ArrayList<>();
                for (int j = start; j < end; j++) {
                    window.add(vector.get(j));
                }
                
                Collections.sort(window);
                float median = window.get(window.size() / 2);
                filtered.add(median);
            }
            
            return filtered;
            
        } catch (Exception e) {
            log.error("Error applying median filtering: {}", e.getMessage(), e);
            return new ArrayList<>(vector);
        }
    }

    /**
     * Apply median filtering for outlier removal
     */
    public CompletableFuture<List<Float>> applyMedianFiltering(List<Float> vector, int windowSize) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (vector == null || vector.isEmpty()) {
                    return new ArrayList<>();
                }
                
                if (windowSize <= 1) {
                    return new ArrayList<>(vector); // No filtering
                }
                
                List<Float> filtered = new ArrayList<>();
                
                for (int i = 0; i < vector.size(); i++) {
                    int start = Math.max(0, i - windowSize / 2);
                    int end = Math.min(vector.size(), i + windowSize / 2 + 1);
                    
                    List<Float> window = new ArrayList<>();
                    for (int j = start; j < end; j++) {
                        window.add(vector.get(j));
                    }
                    
                    Collections.sort(window);
                    float median = window.get(window.size() / 2);
                    filtered.add(median);
                }
                
                return filtered;
                
            } catch (Exception e) {
                log.error("Error applying median filtering: {}", e.getMessage(), e);
                return new ArrayList<>(vector);
            }
        });
    }

    /**
     * Apply adaptive smoothing based on vector characteristics
     */
    public CompletableFuture<List<Float>> applyAdaptiveSmoothing(List<Float> vector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (vector == null || vector.isEmpty()) {
                    return new ArrayList<>();
                }
                
                // Calculate vector characteristics
                double variance = calculateVariance(vector);
                double mean = vector.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
                
                // Determine smoothing strategy based on characteristics
                if (variance < 0.01) {
                    // Low variance - minimal smoothing
                    return applyExponentialSmoothing(vector, 0.1);
                } else if (variance < 0.1) {
                    // Medium variance - moderate smoothing
                    return applyExponentialSmoothing(vector, 0.3);
                } else {
                    // High variance - aggressive smoothing
                    return applyExponentialSmoothing(vector, 0.5);
                }
                
            } catch (Exception e) {
                log.error("Error applying adaptive smoothing: {}", e.getMessage(), e);
                return new ArrayList<>(vector);
            }
        });
    }

    /**
     * Calculate variance of vector
     */
    private double calculateVariance(List<Float> vector) {
        double mean = vector.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
        return vector.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
    }

    /**
     * Apply multi-level smoothing pipeline
     */
    public CompletableFuture<List<Float>> applySmoothingPipeline(
            List<Float> vector, 
            SmoothingPipelineConfig config) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Float> result = new ArrayList<>(vector);
                
                // Step 1: Outlier removal with median filtering
                if (config.isEnableMedianFiltering()) {
                    result = applyMedianFilteringSync(result, config.getMedianWindowSize());
                }
                
                // Step 2: Noise reduction with exponential smoothing
                if (config.isEnableExponentialSmoothing()) {
                    result = applyExponentialSmoothingSync(result, config.getExponentialSmoothingFactor());
                }
                
                // Step 3: Signal enhancement with Gaussian smoothing
                if (config.isEnableGaussianSmoothing()) {
                    result = applyGaussianSmoothingSync(result, config.getGaussianSigma());
                }
                
                return result;
                
            } catch (Exception e) {
                log.error("Error applying smoothing pipeline: {}", e.getMessage(), e);
                return new ArrayList<>(vector);
            }
        });
    }

    // DTO
    @lombok.Data
    @lombok.Builder
    public static class SmoothingPipelineConfig {
        private boolean enableMedianFiltering = true;
        private int medianWindowSize = 3;
        private boolean enableExponentialSmoothing = true;
        private double exponentialSmoothingFactor = 0.2;
        private boolean enableGaussianSmoothing = false;
        private double gaussianSigma = 1.0;
    }
}
