package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced psychological analysis batch processor integrating Youseef's approach
 * with our existing infrastructure and 43-dimensional psychological vectors
 */
@Service
public class PsychologicalAnalysisBatchProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(PsychologicalAnalysisBatchProcessor.class);
    
    @Autowired
    private PsychologicalStateCacheService cacheService;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private IntensityPredictor intensityPredictor;
    
    @Autowired
    private StateVectorProcessor stateVectorProcessor;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    // Batch processing queue
    private final Queue<PsychologicalAnalysisTask> analysisQueue = new ConcurrentLinkedQueue<>();
    
    // Batch processing statistics
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);
    
    // Batch configuration
    private static final int BATCH_SIZE = 100;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final long BATCH_DELAY_MS = 2000; // 2 seconds
    
    /**
     * Enqueue psychological analysis task
     */
    public void enqueueAnalysis(UUID userId, String message, String sessionId) {
        try {
            if (queueSize.get() >= MAX_QUEUE_SIZE) {
                logger.warn("Analysis queue is full, dropping task for user: {}", userId);
                return;
            }
            
            PsychologicalAnalysisTask task = new PsychologicalAnalysisTask(
                userId, message, sessionId, LocalDateTime.now()
            );
            
            analysisQueue.offer(task);
            queueSize.incrementAndGet();
            
            logger.debug("Enqueued analysis task for user: {}, queue size: {}", userId, queueSize.get());
            
        } catch (Exception e) {
            logger.error("Error enqueuing analysis task for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Process batch of psychological analysis tasks
     */
    @Scheduled(fixedDelay = BATCH_DELAY_MS)
    public void processBatch() {
        try {
            List<PsychologicalAnalysisTask> batch = new ArrayList<>();
            
            // Drain queue to batch
            int drained = 0;
            while (drained < BATCH_SIZE && !analysisQueue.isEmpty()) {
                PsychologicalAnalysisTask task = analysisQueue.poll();
                if (task != null) {
                    batch.add(task);
                    queueSize.decrementAndGet();
                    drained++;
                }
            }
            
            if (batch.isEmpty()) {
                return; // No tasks to process
            }
            
            logger.info("Processing batch of {} psychological analysis tasks", batch.size());
            
            // Process batch asynchronously
            processBatchAsync(batch);
            
        } catch (Exception e) {
            logger.error("Error processing psychological analysis batch: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process batch asynchronously
     */
    @Async
    public CompletableFuture<Void> processBatchAsync(List<PsychologicalAnalysisTask> batch) {
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (PsychologicalAnalysisTask task : batch) {
                CompletableFuture<Void> future = processTaskAsync(task);
                futures.add(future);
            }
            
            // Wait for all tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            processedCount.addAndGet(batch.size());
            logger.info("Completed batch processing of {} tasks", batch.size());
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Error in async batch processing: {}", e.getMessage(), e);
            errorCount.addAndGet(batch.size());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Process individual task asynchronously
     */
    @Async
    public CompletableFuture<Void> processTaskAsync(PsychologicalAnalysisTask task) {
        try {
            logger.debug("Processing psychological analysis task for user: {}", task.getUserId());
            
            // Generate psychological vector
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(task.getMessage(), task.getUserId());
            @SuppressWarnings("unchecked")
            List<Double> psychologicalVector = (List<Double>) vectorResult.get("vector");
            
            // Analyze Buddhist factors
            Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(task.getMessage(), task.getUserId());
            @SuppressWarnings("unchecked")
            Map<String, Double> buddhistFactors = (Map<String, Double>) factorsResult.get("factors");
            
            // Calculate intensity factor
            double intensityFactor = intensityPredictor.calculateImpactScore(0.5, psychologicalVector, factorsResult);
            
            // Process state vector
            Map<String, Object> processedResult = stateVectorProcessor.processVector(psychologicalVector, task.getUserId());
            @SuppressWarnings("unchecked")
            List<Double> processedStateVector = (List<Double>) processedResult.get("processedVector");
            
            // Cache the results
            cacheService.cachePsychologicalState(task.getUserId(), psychologicalVector, buddhistFactors);
            
            // Update user personality profile
            updatePersonalityProfile(task.getUserId(), psychologicalVector, buddhistFactors, intensityFactor);
            
            logger.debug("Completed psychological analysis task for user: {}", task.getUserId());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Error processing psychological analysis task for user {}: {}", 
                        task.getUserId(), e.getMessage(), e);
            errorCount.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Update personality profile with psychological insights
     */
    private void updatePersonalityProfile(UUID userId, List<Double> psychologicalVector, 
                                        Map<String, Double> buddhistFactors, double intensityFactor) {
        try {
            // Get current personality profile
            Map<String, Object> currentProfile = cacheService.getPersonalityProfile(userId);
            
            if (currentProfile == null) {
                // Create new profile if none exists
                currentProfile = createDefaultPersonalityProfile(userId);
            }
            
            // Update profile with psychological insights
            Map<String, Object> updatedProfile = updateProfileWithInsights(
                currentProfile, psychologicalVector, buddhistFactors, intensityFactor
            );
            
            // Cache updated profile
            cacheService.cachePersonalityProfile(userId, updatedProfile);
            
            logger.debug("Updated personality profile for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error updating personality profile for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Update profile with psychological insights
     */
    private Map<String, Object> updateProfileWithInsights(Map<String, Object> currentProfile, 
                                                         List<Double> psychologicalVector, 
                                                         Map<String, Double> buddhistFactors, 
                                                         double intensityFactor) {
        Map<String, Object> updatedProfile = new HashMap<>(currentProfile);
        
        // Extract key psychological metrics
        double stress = extractPsychologicalMetric(psychologicalVector, "stress");
        double mindfulness = extractPsychologicalMetric(psychologicalVector, "mindfulness");
        double compassion = extractPsychologicalMetric(psychologicalVector, "compassion");
        double equanimity = extractPsychologicalMetric(psychologicalVector, "equanimity");
        
        // Update emotional state
        String emotionalState = determineEmotionalState(stress, mindfulness, compassion);
        updatedProfile.put("emotionalState", emotionalState);
        
        // Update communication style
        String communicationStyle = determineCommunicationStyle(buddhistFactors);
        updatedProfile.put("communicationStyle", communicationStyle);
        
        // Update personality type
        String personalityType = determinePersonalityType(stress, mindfulness, compassion);
        updatedProfile.put("preferredPersonalityType", personalityType);
        
        // Update interests
        List<String> interests = extractInterestsFromPsychologicalState(psychologicalVector);
        updatedProfile.put("interests", interests);
        
        // Update psychological metrics
        Map<String, Double> psychologicalMetrics = new HashMap<>();
        psychologicalMetrics.put("stress", stress);
        psychologicalMetrics.put("mindfulness", mindfulness);
        psychologicalMetrics.put("compassion", compassion);
        psychologicalMetrics.put("equanimity", equanimity);
        psychologicalMetrics.put("intensityFactor", intensityFactor);
        updatedProfile.put("psychologicalMetrics", psychologicalMetrics);
        
        // Update timestamp
        updatedProfile.put("lastUpdated", LocalDateTime.now());
        
        return updatedProfile;
    }
    
    /**
     * Extract psychological metric from vector
     */
    private double extractPsychologicalMetric(List<Double> vector, String metric) {
        Map<String, Integer> metricIndices = new HashMap<>();
        metricIndices.put("stress", 16); // Anger index
        metricIndices.put("mindfulness", 0); // Attention index
        metricIndices.put("compassion", 6); // Faith index
        metricIndices.put("equanimity", 12); // Non-harming index
        metricIndices.put("anxiety", 17); // Greed index
        metricIndices.put("depression", 18); // Anger index
        metricIndices.put("joy", 1); // Contact index
        metricIndices.put("calm", 13); // Light ease index
        metricIndices.put("focus", 2); // Feeling index
        metricIndices.put("wisdom", 11); // Absence of ignorance index
        
        int index = metricIndices.getOrDefault(metric, 0);
        return index < vector.size() ? vector.get(index) : 0.0;
    }
    
    /**
     * Determine emotional state from psychological metrics
     */
    private String determineEmotionalState(double stress, double mindfulness, double compassion) {
        if (stress > 0.7) {
            return "anxious";
        } else if (mindfulness > 0.6 && compassion > 0.5) {
            return "peaceful";
        } else if (compassion > 0.7) {
            return "empathetic";
        } else if (mindfulness > 0.5) {
            return "mindful";
        } else {
            return "neutral";
        }
    }
    
    /**
     * Determine communication style from Buddhist factors
     */
    private String determineCommunicationStyle(Map<String, Double> buddhistFactors) {
        double mindfulness = buddhistFactors.getOrDefault("mindfulness", 0.0);
        double compassion = buddhistFactors.getOrDefault("compassion", 0.0);
        double equanimity = buddhistFactors.getOrDefault("equanimity", 0.0);
        
        if (mindfulness > 0.6 && compassion > 0.5) {
            return "gentle";
        } else if (equanimity > 0.6) {
            return "balanced";
        } else if (compassion > 0.7) {
            return "warm";
        } else {
            return "supportive";
        }
    }
    
    /**
     * Determine personality type from psychological state
     */
    private String determinePersonalityType(double stress, double mindfulness, double compassion) {
        if (stress > 0.7) {
            return "empathetic";
        } else if (mindfulness > 0.6 && compassion > 0.5) {
            return "reflective";
        } else if (compassion > 0.7) {
            return "empathetic";
        } else if (mindfulness > 0.5) {
            return "mindful";
        } else {
            return "supportive";
        }
    }
    
    /**
     * Extract interests from psychological state vector
     */
    private List<String> extractInterestsFromPsychologicalState(List<Double> psychologicalVector) {
        List<String> interests = new ArrayList<>();
        
        if (psychologicalVector.size() > 0 && psychologicalVector.get(0) > 0.5) { // Attention
            interests.add("mindfulness");
        }
        if (psychologicalVector.size() > 6 && psychologicalVector.get(6) > 0.5) { // Faith
            interests.add("spirituality");
        }
        if (psychologicalVector.size() > 7 && psychologicalVector.get(7) > 0.5) { // Vigor
            interests.add("personal growth");
        }
        if (psychologicalVector.size() > 12 && psychologicalVector.get(12) > 0.5) { // Non-harming
            interests.add("compassion");
        }
        if (psychologicalVector.size() > 13 && psychologicalVector.get(13) > 0.5) { // Light ease
            interests.add("wellness");
        }
        
        if (interests.isEmpty()) {
            interests.add("general support");
            interests.add("emotional well-being");
        }
        
        return interests;
    }
    
    /**
     * Create default personality profile
     */
    private Map<String, Object> createDefaultPersonalityProfile(UUID userId) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", userId.toString());
        profile.put("preferredPersonalityType", "supportive");
        profile.put("communicationStyle", "supportive");
        profile.put("emotionalState", "neutral");
        profile.put("interests", List.of("general support", "emotional well-being"));
        profile.put("enableHumor", true);
        profile.put("enableEmpathy", true);
        profile.put("preferredResponseLength", List.of("medium"));
        profile.put("culturalContext", "general");
        profile.put("conversationCount", 0);
        profile.put("createdAt", LocalDateTime.now());
        profile.put("lastUpdated", LocalDateTime.now());
        return profile;
    }
    
    /**
     * Get batch processing statistics
     */
    public Map<String, Object> getBatchStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queueSize", queueSize.get());
        stats.put("processedCount", processedCount.get());
        stats.put("errorCount", errorCount.get());
        stats.put("batchSize", BATCH_SIZE);
        stats.put("maxQueueSize", MAX_QUEUE_SIZE);
        stats.put("batchDelayMs", BATCH_DELAY_MS);
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }
    
    /**
     * Clear batch processing statistics
     */
    public void clearStatistics() {
        processedCount.set(0);
        errorCount.set(0);
        logger.info("Cleared batch processing statistics");
    }
    
    /**
     * Get current queue size
     */
    public int getCurrentQueueSize() {
        return queueSize.get();
    }
    
    /**
     * Check if queue is full
     */
    public boolean isQueueFull() {
        return queueSize.get() >= MAX_QUEUE_SIZE;
    }
    
    /**
     * Psychological analysis task
     */
    public static class PsychologicalAnalysisTask {
        private final UUID userId;
        private final String message;
        private final String sessionId;
        private final LocalDateTime timestamp;
        
        public PsychologicalAnalysisTask(UUID userId, String message, String sessionId, LocalDateTime timestamp) {
            this.userId = userId;
            this.message = message;
            this.sessionId = sessionId;
            this.timestamp = timestamp;
        }
        
        public UUID getUserId() { return userId; }
        public String getMessage() { return message; }
        public String getSessionId() { return sessionId; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
