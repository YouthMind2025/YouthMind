package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for preparing psychological training datasets
 * Implements sophisticated data processing and augmentation algorithms
 */
@Service
@Transactional
public class TrainingDataProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingDataProcessor.class);
    
    // Data processing constants
    private static final double DEFAULT_TRAIN_SPLIT = 0.7;
    private static final double DEFAULT_VALIDATION_SPLIT = 0.15;
    private static final double DEFAULT_TEST_SPLIT = 0.15;
    private static final int MIN_SAMPLES_PER_CLASS = 10;
    private static final int MAX_AUGMENTATION_FACTOR = 3;
    
    // Redis keys
    private static final String TRAINING_DATA_KEY = "training_data:";
    private static final String DATA_PROCESSING_KEY = "data_processing:";
    private static final String DATA_STATISTICS_KEY = "data_statistics:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Prepare psychological training datasets
     * @param trainingConfig Training configuration
     * @param userId User ID for personalization
     * @return Prepared training data
     */
    public Map<String, Object> prepareData(Map<String, Object> trainingConfig, UUID userId) {
        try {
            logger.info("Preparing training data for user {} with config: {}", userId, trainingConfig);
            
            // Extract configuration
            String modelType = (String) trainingConfig.get("modelType");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawData = (List<Map<String, Object>>) trainingConfig.get("trainingData");
            
            // Validate data
            Map<String, Object> validationResult = validateData(rawData);
            if (!(Boolean) validationResult.get("isValid")) {
                throw new IllegalArgumentException("Invalid training data: " + validationResult.get("message"));
            }
            
            // Clean and preprocess data
            List<Map<String, Object>> cleanedData = cleanData(rawData);
            
            // Augment data if needed
            List<Map<String, Object>> augmentedData = augmentData(cleanedData, trainingConfig);
            
            // Split data into train/validation/test sets
            Map<String, Object> splitData = splitData(augmentedData, trainingConfig);
            
            // Calculate data statistics
            Map<String, Object> statistics = calculateDataStatistics(splitData);
            
            // Create prepared data result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("modelType", modelType);
            result.put("rawDataSize", rawData.size());
            result.put("cleanedDataSize", cleanedData.size());
            result.put("augmentedDataSize", augmentedData.size());
            result.put("splitData", splitData);
            result.put("statistics", statistics);
            result.put("validationResult", validationResult);
            result.put("preparationMethod", "COMPREHENSIVE_PROCESSING");
            result.put("timestamp", LocalDateTime.now());
            
            // Store prepared data
            storePreparedData(userId, result);
            
            logger.info("Training data prepared for user {}: {} samples", userId, augmentedData.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error preparing training data for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to prepare training data: " + e.getMessage());
        }
    }
    
    /**
     * Validate training data quality
     * @param data Training data to validate
     * @return Validation result
     */
    public Map<String, Object> validateData(List<Map<String, Object>> data) {
        try {
            logger.debug("Validating training data with {} samples", data.size());
            
            Map<String, Object> validationResult = new HashMap<>();
            
            // Check data size
            if (data == null || data.isEmpty()) {
                validationResult.put("isValid", false);
                validationResult.put("message", "No training data provided");
                return validationResult;
            }
            
            if (data.size() < MIN_SAMPLES_PER_CLASS) {
                validationResult.put("isValid", false);
                validationResult.put("message", "Insufficient training data: minimum " + MIN_SAMPLES_PER_CLASS + " samples required");
                return validationResult;
            }
            
            // Check data structure
            Map<String, Object> structureValidation = validateDataStructure(data);
            if (!(Boolean) structureValidation.get("isValid")) {
                validationResult.put("isValid", false);
                validationResult.put("message", "Invalid data structure: " + structureValidation.get("message"));
                return validationResult;
            }
            
            // Check data quality
            Map<String, Object> qualityValidation = validateDataQuality(data);
            if (!(Boolean) qualityValidation.get("isValid")) {
                validationResult.put("isValid", false);
                validationResult.put("message", "Poor data quality: " + qualityValidation.get("message"));
                return validationResult;
            }
            
            // Check class balance
            Map<String, Object> balanceValidation = validateClassBalance(data);
            if (!(Boolean) balanceValidation.get("isValid")) {
                validationResult.put("isValid", false);
                validationResult.put("message", "Unbalanced classes: " + balanceValidation.get("message"));
                return validationResult;
            }
            
            validationResult.put("isValid", true);
            validationResult.put("structureValidation", structureValidation);
            validationResult.put("qualityValidation", qualityValidation);
            validationResult.put("balanceValidation", balanceValidation);
            validationResult.put("totalSamples", data.size());
            
            logger.debug("Training data validation completed");
            return validationResult;
            
        } catch (Exception e) {
            logger.error("Error validating training data: {}", e.getMessage(), e);
            return Map.of(
                "isValid", false,
                "message", "Validation error: " + e.getMessage()
            );
        }
    }
    
    /**
     * Augment training data
     * @param data Original training data
     * @param config Augmentation configuration
     * @return Augmented training data
     */
    public List<Map<String, Object>> augmentData(List<Map<String, Object>> data, Map<String, Object> config) {
        try {
            logger.debug("Augmenting training data with {} samples", data.size());
            
            List<Map<String, Object>> augmentedData = new ArrayList<>(data);
            
            // Check if augmentation is needed
            int augmentationFactor = (Integer) config.getOrDefault("augmentationFactor", 1);
            if (augmentationFactor <= 1) {
                return augmentedData;
            }
            
            // Apply various augmentation techniques
            if (config.containsKey("textAugmentation")) {
                List<Map<String, Object>> textAugmented = performTextAugmentation(data, augmentationFactor);
                augmentedData.addAll(textAugmented);
            }
            
            if (config.containsKey("syntheticGeneration")) {
                List<Map<String, Object>> syntheticData = generateSyntheticData(data, augmentationFactor);
                augmentedData.addAll(syntheticData);
            }
            
            if (config.containsKey("noiseInjection")) {
                List<Map<String, Object>> noisyData = injectNoise(data, augmentationFactor);
                augmentedData.addAll(noisyData);
            }
            
            logger.debug("Data augmentation completed: {} original + {} augmented = {} total", 
                data.size(), augmentedData.size() - data.size(), augmentedData.size());
            
            return augmentedData;
            
        } catch (Exception e) {
            logger.error("Error augmenting training data: {}", e.getMessage(), e);
            return data; // Return original data if augmentation fails
        }
    }
    
    /**
     * Split data into train/validation/test sets
     * @param data Training data to split
     * @param config Split configuration
     * @return Split data sets
     */
    public Map<String, Object> splitData(List<Map<String, Object>> data, Map<String, Object> config) {
        try {
            logger.debug("Splitting data into train/validation/test sets");
            
            // Get split ratios
            double trainSplit = (Double) config.getOrDefault("trainSplit", DEFAULT_TRAIN_SPLIT);
            double validationSplit = (Double) config.getOrDefault("validationSplit", DEFAULT_VALIDATION_SPLIT);
            double testSplit = (Double) config.getOrDefault("testSplit", DEFAULT_TEST_SPLIT);
            
            // Validate split ratios
            if (Math.abs(trainSplit + validationSplit + testSplit - 1.0) > 0.001) {
                throw new IllegalArgumentException("Split ratios must sum to 1.0");
            }
            
            // Shuffle data
            List<Map<String, Object>> shuffledData = new ArrayList<>(data);
            Collections.shuffle(shuffledData);
            
            // Calculate split indices
            int totalSize = shuffledData.size();
            int trainSize = (int) (totalSize * trainSplit);
            int validationSize = (int) (totalSize * validationSplit);
            
            // Split data
            List<Map<String, Object>> trainData = shuffledData.subList(0, trainSize);
            List<Map<String, Object>> validationData = shuffledData.subList(trainSize, trainSize + validationSize);
            List<Map<String, Object>> testData = shuffledData.subList(trainSize + validationSize, totalSize);
            
            // Create split result
            Map<String, Object> splitResult = new HashMap<>();
            splitResult.put("trainData", trainData);
            splitResult.put("validationData", validationData);
            splitResult.put("testData", testData);
            splitResult.put("trainSize", trainData.size());
            splitResult.put("validationSize", validationData.size());
            splitResult.put("testSize", testData.size());
            splitResult.put("trainRatio", trainSplit);
            splitResult.put("validationRatio", validationSplit);
            splitResult.put("testRatio", testSplit);
            
            logger.debug("Data split completed: train={}, validation={}, test={}", 
                trainData.size(), validationData.size(), testData.size());
            
            return splitResult;
            
        } catch (Exception e) {
            logger.error("Error splitting data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to split data: " + e.getMessage());
        }
    }
    
    /**
     * Get prepared training data for a user
     * @param userId User ID
     * @return Prepared training data
     */
    public Map<String, Object> getPreparedData(UUID userId) {
        try {
            logger.debug("Getting prepared training data for user {}", userId);
            
            String dataKey = TRAINING_DATA_KEY + userId;
            Map<String, Object> preparedData = (Map<String, Object>) redisTemplate.opsForValue().get(dataKey);
            
            return preparedData != null ? preparedData : Map.of(
                "status", "NO_DATA",
                "userId", userId.toString(),
                "timestamp", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error getting prepared training data for user {}: {}", userId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to get prepared data: " + e.getMessage(),
                "userId", userId.toString(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private List<Map<String, Object>> cleanData(List<Map<String, Object>> rawData) {
        List<Map<String, Object>> cleanedData = new ArrayList<>();
        
        for (Map<String, Object> sample : rawData) {
            try {
                // Clean text content
                String content = (String) sample.get("content");
                if (content != null) {
                    content = cleanTextContent(content);
                    sample.put("content", content);
                }
                
                // Clean psychological vector
                @SuppressWarnings("unchecked")
                List<Double> vector = (List<Double>) sample.get("psychologicalVector");
                if (vector != null) {
                    vector = cleanPsychologicalVector(vector);
                    sample.put("psychologicalVector", vector);
                }
                
                // Remove invalid samples
                if (isValidSample(sample)) {
                    cleanedData.add(sample);
                }
                
            } catch (Exception e) {
                logger.warn("Error cleaning sample: {}", e.getMessage());
                // Skip invalid samples
            }
        }
        
        return cleanedData;
    }
    
    private String cleanTextContent(String content) {
        if (content == null) {
            return "";
        }
        
        // Remove extra whitespace
        content = content.trim().replaceAll("\\s+", " ");
        
        // Remove special characters (keep basic punctuation)
        content = content.replaceAll("[^a-zA-Z0-9\\s.,!?;:]", "");
        
        // Normalize case
        content = content.toLowerCase();
        
        return content;
    }
    
    private List<Double> cleanPsychologicalVector(List<Double> vector) {
        if (vector == null) {
            return new ArrayList<>();
        }
        
        List<Double> cleanedVector = new ArrayList<>();
        
        for (Double value : vector) {
            if (value == null) {
                cleanedVector.add(0.0);
            } else if (value < 0.0) {
                cleanedVector.add(0.0);
            } else if (value > 1.0) {
                cleanedVector.add(1.0);
            } else {
                cleanedVector.add(value);
            }
        }
        
        return cleanedVector;
    }
    
    private boolean isValidSample(Map<String, Object> sample) {
        // Check if sample has required fields
        if (!sample.containsKey("content") || !sample.containsKey("psychologicalVector")) {
            return false;
        }
        
        String content = (String) sample.get("content");
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        List<Double> vector = (List<Double>) sample.get("psychologicalVector");
        if (vector == null || vector.size() != 43) {
            return false;
        }
        
        return true;
    }
    
    private Map<String, Object> validateDataStructure(List<Map<String, Object>> data) {
        Map<String, Object> result = new HashMap<>();
        
        // Check if all samples have required fields
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> sample = data.get(i);
            
            if (!sample.containsKey("content")) {
                result.put("isValid", false);
                result.put("message", "Sample " + i + " missing 'content' field");
                return result;
            }
            
            if (!sample.containsKey("psychologicalVector")) {
                result.put("isValid", false);
                result.put("message", "Sample " + i + " missing 'psychologicalVector' field");
                return result;
            }
            
            @SuppressWarnings("unchecked")
            List<Double> vector = (List<Double>) sample.get("psychologicalVector");
            if (vector == null || vector.size() != 43) {
                result.put("isValid", false);
                result.put("message", "Sample " + i + " has invalid psychological vector size");
                return result;
            }
        }
        
        result.put("isValid", true);
        result.put("message", "Data structure is valid");
        return result;
    }
    
    private Map<String, Object> validateDataQuality(List<Map<String, Object>> data) {
        Map<String, Object> result = new HashMap<>();
        
        int validSamples = 0;
        int emptyContentSamples = 0;
        int invalidVectorSamples = 0;
        
        for (Map<String, Object> sample : data) {
            String content = (String) sample.get("content");
            if (content == null || content.trim().isEmpty()) {
                emptyContentSamples++;
                continue;
            }
            
            @SuppressWarnings("unchecked")
            List<Double> vector = (List<Double>) sample.get("psychologicalVector");
            if (vector == null || vector.size() != 43) {
                invalidVectorSamples++;
                continue;
            }
            
            validSamples++;
        }
        
        double qualityScore = (double) validSamples / data.size();
        
        if (qualityScore < 0.8) {
            result.put("isValid", false);
            result.put("message", String.format("Low data quality: %.2f%% valid samples", qualityScore * 100));
            return result;
        }
        
        result.put("isValid", true);
        result.put("qualityScore", qualityScore);
        result.put("validSamples", validSamples);
        result.put("emptyContentSamples", emptyContentSamples);
        result.put("invalidVectorSamples", invalidVectorSamples);
        return result;
    }
    
    private Map<String, Object> validateClassBalance(List<Map<String, Object>> data) {
        Map<String, Object> result = new HashMap<>();
        
        // Analyze class distribution (assuming we have labels)
        Map<String, Integer> classDistribution = new HashMap<>();
        
        for (Map<String, Object> sample : data) {
            String label = (String) sample.getOrDefault("label", "unknown");
            classDistribution.put(label, classDistribution.getOrDefault(label, 0) + 1);
        }
        
        // Check for class imbalance
        int maxCount = classDistribution.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int minCount = classDistribution.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        
        double imbalanceRatio = maxCount > 0 ? (double) minCount / maxCount : 0.0;
        
        if (imbalanceRatio < 0.1) {
            result.put("isValid", false);
            result.put("message", "Severe class imbalance detected");
            return result;
        }
        
        result.put("isValid", true);
        result.put("classDistribution", classDistribution);
        result.put("imbalanceRatio", imbalanceRatio);
        return result;
    }
    
    private List<Map<String, Object>> performTextAugmentation(List<Map<String, Object>> data, int factor) {
        List<Map<String, Object>> augmentedData = new ArrayList<>();
        
        for (Map<String, Object> sample : data) {
            for (int i = 0; i < factor; i++) {
                Map<String, Object> augmentedSample = new HashMap<>(sample);
                
                // Apply text augmentation techniques
                String originalContent = (String) sample.get("content");
                String augmentedContent = augmentTextContent(originalContent);
                augmentedSample.put("content", augmentedContent);
                augmentedSample.put("augmented", true);
                augmentedSample.put("augmentationType", "text");
                
                augmentedData.add(augmentedSample);
            }
        }
        
        return augmentedData;
    }
    
    private String augmentTextContent(String content) {
        // Simple text augmentation techniques
        StringBuilder augmented = new StringBuilder(content);
        
        // Randomly insert spaces or modify punctuation
        if (Math.random() < 0.3) {
            augmented.append(".");
        }
        
        return augmented.toString();
    }
    
    private List<Map<String, Object>> generateSyntheticData(List<Map<String, Object>> data, int factor) {
        List<Map<String, Object>> syntheticData = new ArrayList<>();
        
        for (Map<String, Object> sample : data) {
            for (int i = 0; i < factor; i++) {
                Map<String, Object> syntheticSample = new HashMap<>(sample);
                
                // Generate synthetic content
                String syntheticContent = generateSyntheticContent((String) sample.get("content"));
                syntheticSample.put("content", syntheticContent);
                
                // Generate synthetic psychological vector
                @SuppressWarnings("unchecked")
                List<Double> originalVector = (List<Double>) sample.get("psychologicalVector");
                List<Double> syntheticVector = generateSyntheticVector(originalVector);
                syntheticSample.put("psychologicalVector", syntheticVector);
                
                syntheticSample.put("synthetic", true);
                syntheticSample.put("generationType", "synthetic");
                
                syntheticData.add(syntheticSample);
            }
        }
        
        return syntheticData;
    }
    
    private String generateSyntheticContent(String originalContent) {
        // Simple synthetic content generation
        return "Generated: " + originalContent;
    }
    
    private List<Double> generateSyntheticVector(List<Double> originalVector) {
        List<Double> syntheticVector = new ArrayList<>();
        
        for (Double value : originalVector) {
            // Add small random noise
            double noise = (Math.random() - 0.5) * 0.1;
            double syntheticValue = Math.max(0.0, Math.min(1.0, value + noise));
            syntheticVector.add(syntheticValue);
        }
        
        return syntheticVector;
    }
    
    private List<Map<String, Object>> injectNoise(List<Map<String, Object>> data, int factor) {
        List<Map<String, Object>> noisyData = new ArrayList<>();
        
        for (Map<String, Object> sample : data) {
            for (int i = 0; i < factor; i++) {
                Map<String, Object> noisySample = new HashMap<>(sample);
                
                // Inject noise into psychological vector
                @SuppressWarnings("unchecked")
                List<Double> originalVector = (List<Double>) sample.get("psychologicalVector");
                List<Double> noisyVector = injectVectorNoise(originalVector);
                noisySample.put("psychologicalVector", noisyVector);
                
                noisySample.put("noisy", true);
                noisySample.put("noiseType", "gaussian");
                
                noisyData.add(noisySample);
            }
        }
        
        return noisyData;
    }
    
    private List<Double> injectVectorNoise(List<Double> originalVector) {
        List<Double> noisyVector = new ArrayList<>();
        
        for (Double value : originalVector) {
            // Add Gaussian noise
            double noise = Math.random() * 0.05 - 0.025; // ±2.5% noise
            double noisyValue = Math.max(0.0, Math.min(1.0, value + noise));
            noisyVector.add(noisyValue);
        }
        
        return noisyVector;
    }
    
    private Map<String, Object> calculateDataStatistics(Map<String, Object> splitData) {
        Map<String, Object> statistics = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trainData = (List<Map<String, Object>>) splitData.get("trainData");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validationData = (List<Map<String, Object>>) splitData.get("validationData");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> testData = (List<Map<String, Object>>) splitData.get("testData");
        
        // Calculate content length statistics
        Map<String, Object> contentStats = calculateContentStatistics(trainData);
        statistics.put("contentStatistics", contentStats);
        
        // Calculate vector statistics
        Map<String, Object> vectorStats = calculateVectorStatistics(trainData);
        statistics.put("vectorStatistics", vectorStats);
        
        // Calculate class distribution
        Map<String, Object> classStats = calculateClassStatistics(trainData);
        statistics.put("classStatistics", classStats);
        
        statistics.put("totalSamples", trainData.size() + validationData.size() + testData.size());
        statistics.put("trainSamples", trainData.size());
        statistics.put("validationSamples", validationData.size());
        statistics.put("testSamples", testData.size());
        
        return statistics;
    }
    
    private Map<String, Object> calculateContentStatistics(List<Map<String, Object>> data) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Integer> lengths = data.stream()
            .map(sample -> ((String) sample.get("content")).length())
            .collect(Collectors.toList());
        
        stats.put("avgLength", lengths.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        stats.put("minLength", lengths.stream().mapToInt(Integer::intValue).min().orElse(0));
        stats.put("maxLength", lengths.stream().mapToInt(Integer::intValue).max().orElse(0));
        
        return stats;
    }
    
    private Map<String, Object> calculateVectorStatistics(List<Map<String, Object>> data) {
        Map<String, Object> stats = new HashMap<>();
        
        // Calculate mean and variance for each dimension
        List<List<Double>> vectors = data.stream()
            .map(sample -> (List<Double>) sample.get("psychologicalVector"))
            .collect(Collectors.toList());
        
        if (!vectors.isEmpty()) {
            int dimensions = vectors.get(0).size();
            List<Double> means = new ArrayList<>();
            List<Double> variances = new ArrayList<>();
            
            for (int i = 0; i < dimensions; i++) {
                final int dim = i;
                double mean = vectors.stream()
                    .mapToDouble(vector -> vector.get(dim))
                    .average().orElse(0.0);
                means.add(mean);
                
                double variance = vectors.stream()
                    .mapToDouble(vector -> Math.pow(vector.get(dim) - mean, 2))
                    .average().orElse(0.0);
                variances.add(variance);
            }
            
            stats.put("dimensionMeans", means);
            stats.put("dimensionVariances", variances);
        }
        
        return stats;
    }
    
    private Map<String, Object> calculateClassStatistics(List<Map<String, Object>> data) {
        Map<String, Object> stats = new HashMap<>();
        
        Map<String, Integer> classCounts = new HashMap<>();
        for (Map<String, Object> sample : data) {
            String label = (String) sample.getOrDefault("label", "unknown");
            classCounts.put(label, classCounts.getOrDefault(label, 0) + 1);
        }
        
        stats.put("classDistribution", classCounts);
        stats.put("numClasses", classCounts.size());
        
        return stats;
    }
    
    private void storePreparedData(UUID userId, Map<String, Object> data) {
        try {
            String dataKey = TRAINING_DATA_KEY + userId;
            redisTemplate.opsForValue().set(dataKey, data, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.warn("Error storing prepared data for user {}: {}", userId, e.getMessage());
        }
    }
}
