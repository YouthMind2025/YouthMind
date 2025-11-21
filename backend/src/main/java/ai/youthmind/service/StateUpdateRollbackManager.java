package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State Update Rollback Manager for error recovery and state restoration.
 * Provides sophisticated rollback mechanisms for psychological state updates
 * with comprehensive error recovery capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateUpdateRollbackManager {

    // Rollback parameters
    private static final int MAX_ROLLBACK_STEPS = 50;
    private static final long ROLLBACK_TIMEOUT_MS = 10000; // 10 seconds
    private static final int MAX_HISTORY_SIZE = 1000;

    // Rollback history tracking
    private final Map<String, List<StateUpdateHistory>> rollbackHistory = new ConcurrentHashMap<>();
    private final Map<String, RollbackSession> activeRollbackSessions = new ConcurrentHashMap<>();

    /**
     * Rollback state update to previous state
     */
    public CompletableFuture<Boolean> rollbackStateUpdate(
            String userId, 
            int stepsBack, 
            List<StateUpdateHistory> stateHistory) {
        
        return CompletableFuture.supplyAsync(() -> {
            String sessionId = generateSessionId();
            
            try {
                log.info("Starting rollback session {} for user {}: {} steps back", sessionId, userId, stepsBack);
                
                // Validate rollback parameters
                if (stepsBack <= 0 || stepsBack > MAX_ROLLBACK_STEPS) {
                    log.error("Invalid rollback steps: {}", stepsBack);
                    return false;
                }
                
                if (stateHistory == null || stateHistory.isEmpty()) {
                    log.error("No state history available for rollback");
                    return false;
                }
                
                // Create rollback session
                RollbackSession session = RollbackSession.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .stepsBack(stepsBack)
                    .startTime(System.currentTimeMillis())
                    .status(RollbackStatus.IN_PROGRESS)
                    .build();
                
                activeRollbackSessions.put(sessionId, session);
                
                // Perform rollback
                boolean success = performRollback(session, stateHistory);
                
                // Update session status
                session.setStatus(success ? RollbackStatus.COMPLETED : RollbackStatus.FAILED);
                session.setEndTime(System.currentTimeMillis());
                
                // Clean up session
                activeRollbackSessions.remove(sessionId);
                
                log.info("Rollback session {} completed: {}", sessionId, success ? "SUCCESS" : "FAILED");
                
                return success;
                
            } catch (Exception e) {
                log.error("Error in rollback session {}: {}", sessionId, e.getMessage(), e);
                
                // Clean up failed session
                RollbackSession session = activeRollbackSessions.get(sessionId);
                if (session != null) {
                    session.setStatus(RollbackStatus.FAILED);
                    session.setEndTime(System.currentTimeMillis());
                    session.setErrorMessage(e.getMessage());
                    activeRollbackSessions.remove(sessionId);
                }
                
                return false;
            }
        });
    }

    /**
     * Perform the actual rollback operation
     */
    private boolean performRollback(RollbackSession session, List<StateUpdateHistory> stateHistory) {
        try {
            String userId = session.getUserId();
            int stepsBack = session.getStepsBack();
            
            // Find the target state to rollback to
            if (stateHistory.size() < stepsBack) {
                log.error("Insufficient history for rollback: {} steps requested, {} available", 
                         stepsBack, stateHistory.size());
                return false;
            }
            
            // Get the target state (going back stepsBack steps)
            StateUpdateHistory targetHistory = stateHistory.get(stateHistory.size() - stepsBack);
            List<Float> targetState = targetHistory.getPreviousState();
            
            // Validate target state
            if (targetState == null || targetState.isEmpty()) {
                log.error("Invalid target state for rollback");
                return false;
            }
            
            // Create rollback entry
            StateUpdateHistory rollbackEntry = StateUpdateHistory.builder()
                .userId(userId)
                .previousState(getCurrentState(userId, stateHistory))
                .newState(new ArrayList<>(targetState))
                .observation(new ArrayList<>(targetState)) // Use target state as observation
                .observationType("rollback")
                .alpha(0.0) // No learning rate for rollback
                .similarityMetrics(null)
                .timestamp(System.currentTimeMillis())
                .context(Map.of("rollback_session", session.getSessionId(), "steps_back", stepsBack))
                .build();
            
            // Add rollback entry to history
            stateHistory.add(rollbackEntry);
            
            // Store rollback history
            rollbackHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(rollbackEntry);
            
            // Clean up old history if needed
            cleanupOldHistory(userId, stateHistory);
            
            log.info("Successfully rolled back user {} to state from {} steps ago", userId, stepsBack);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error performing rollback: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get current state from history
     */
    private List<Float> getCurrentState(String userId, List<StateUpdateHistory> stateHistory) {
        if (stateHistory.isEmpty()) {
            return new ArrayList<>();
        }
        
        StateUpdateHistory lastEntry = stateHistory.get(stateHistory.size() - 1);
        return lastEntry.getNewState();
    }

    /**
     * Clean up old history entries
     */
    private void cleanupOldHistory(String userId, List<StateUpdateHistory> stateHistory) {
        if (stateHistory.size() > MAX_HISTORY_SIZE) {
            // Remove oldest entries, keeping the most recent ones
            int entriesToRemove = stateHistory.size() - MAX_HISTORY_SIZE;
            for (int i = 0; i < entriesToRemove; i++) {
                stateHistory.remove(0);
            }
            
            log.debug("Cleaned up {} old history entries for user {}", entriesToRemove, userId);
        }
    }

    /**
     * Create checkpoint for future rollback
     */
    public CompletableFuture<String> createCheckpoint(String userId, List<Float> currentState) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String checkpointId = generateCheckpointId();
                
                StateUpdateHistory checkpoint = StateUpdateHistory.builder()
                    .userId(userId)
                    .previousState(new ArrayList<>(currentState))
                    .newState(new ArrayList<>(currentState))
                    .observation(new ArrayList<>(currentState))
                    .observationType("checkpoint")
                    .alpha(0.0)
                    .similarityMetrics(null)
                    .timestamp(System.currentTimeMillis())
                    .context(Map.of("checkpoint_id", checkpointId))
                    .build();
                
                // Store checkpoint
                rollbackHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(checkpoint);
                
                log.debug("Created checkpoint {} for user {}", checkpointId, userId);
                
                return checkpointId;
                
            } catch (Exception e) {
                log.error("Error creating checkpoint for user {}: {}", userId, e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Rollback to specific checkpoint
     */
    public CompletableFuture<Boolean> rollbackToCheckpoint(String userId, String checkpointId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<StateUpdateHistory> history = rollbackHistory.get(userId);
                if (history == null || history.isEmpty()) {
                    log.error("No history available for user {}", userId);
                    return false;
                }
                
                // Find checkpoint
                StateUpdateHistory checkpoint = null;
                int checkpointIndex = -1;
                
                for (int i = history.size() - 1; i >= 0; i--) {
                    StateUpdateHistory entry = history.get(i);
                    if ("checkpoint".equals(entry.getObservationType()) && 
                        checkpointId.equals(entry.getContext().get("checkpoint_id"))) {
                        checkpoint = entry;
                        checkpointIndex = i;
                        break;
                    }
                }
                
                if (checkpoint == null) {
                    log.error("Checkpoint {} not found for user {}", checkpointId, userId);
                    return false;
                }
                
                // Calculate steps back to checkpoint
                int stepsBack = history.size() - 1 - checkpointIndex;
                
                return rollbackStateUpdate(userId, stepsBack, history).get();
                
            } catch (Exception e) {
                log.error("Error rolling back to checkpoint {} for user {}: {}", checkpointId, userId, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Get rollback history for user
     */
    public List<StateUpdateHistory> getRollbackHistory(String userId) {
        return rollbackHistory.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * Get active rollback sessions
     */
    public Map<String, RollbackSession> getActiveRollbackSessions() {
        return new HashMap<>(activeRollbackSessions);
    }

    /**
     * Cancel active rollback session
     */
    public boolean cancelRollbackSession(String sessionId) {
        RollbackSession session = activeRollbackSessions.get(sessionId);
        if (session != null) {
            session.setStatus(RollbackStatus.CANCELLED);
            session.setEndTime(System.currentTimeMillis());
            activeRollbackSessions.remove(sessionId);
            log.info("Cancelled rollback session {}", sessionId);
            return true;
        }
        return false;
    }

    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "rollback_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate unique checkpoint ID
     */
    private String generateCheckpointId() {
        return "checkpoint_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Enums and DTOs
    public enum RollbackStatus {
        IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }

    @lombok.Data
    @lombok.Builder
    public static class StateUpdateHistory {
        private String userId;
        private List<Float> previousState;
        private List<Float> newState;
        private List<Float> observation;
        private String observationType;
        private double alpha;
        private StateSimilarityMetrics similarityMetrics;
        private long timestamp;
        private Map<String, Object> context;
    }

    @lombok.Data
    @lombok.Builder
    public static class RollbackSession {
        private String sessionId;
        private String userId;
        private int stepsBack;
        private long startTime;
        private long endTime;
        private RollbackStatus status;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    public static class StateSimilarityMetrics {
        private double cosineSimilarity;
        private double euclideanDistance;
        private double manhattanDistance;
        private double jaccardSimilarity;
    }
}
