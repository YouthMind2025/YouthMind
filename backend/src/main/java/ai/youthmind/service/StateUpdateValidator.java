package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * State Update Validator for comprehensive validation and error correction.
 * Provides sophisticated validation algorithms for psychological state transitions
 * with error detection and correction capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateUpdateValidator {

    // Validation parameters
    private static final float MIN_VECTOR_VALUE = 0.0f;
    private static final float MAX_VECTOR_VALUE = 1.0f;
    private static final int MIN_VECTOR_SIZE = 1;
    private static final int MAX_VECTOR_SIZE = 1000;
    private static final double MAX_STATE_CHANGE_THRESHOLD = 0.5;

    /**
     * Validate state update request
     */
    public CompletableFuture<StateValidationResult> validateStateUpdate(
            List<Float> currentState,
            List<Float> newObservation,
            String observationType,
            Map<String, Object> context) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> errors = new ArrayList<>();
                
                // Validate current state
                validateVector(currentState, "current_state", errors);
                
                // Validate new observation
                validateVector(newObservation, "new_observation", errors);
                
                // Validate observation type
                validateObservationType(observationType, errors);
                
                // Validate context
                validateContext(context, errors);
                
                // Validate state transition feasibility
                validateStateTransition(currentState, newObservation, errors);
                
                boolean isValid = errors.isEmpty();
                
                return StateValidationResult.builder()
                    .valid(isValid)
                    .errorMessage(isValid ? null : String.join("; ", errors))
                    .validationErrors(errors)
                    .build();
                    
            } catch (Exception e) {
                log.error("Error validating state update: {}", e.getMessage(), e);
                return StateValidationResult.builder()
                    .valid(false)
                    .errorMessage("Validation error: " + e.getMessage())
                    .validationErrors(List.of("Validation system error"))
                    .build();
            }
        });
    }

    /**
     * Validate updated state
     */
    public CompletableFuture<StateValidationResult> validateUpdatedState(
            List<Float> originalState,
            List<Float> updatedState,
            String observationType) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> errors = new ArrayList<>();
                
                // Validate updated state vector
                validateVector(updatedState, "updated_state", errors);
                
                // Validate state change magnitude
                validateStateChangeMagnitude(originalState, updatedState, errors);
                
                // Validate state consistency
                validateStateConsistency(updatedState, errors);
                
                boolean isValid = errors.isEmpty();
                
                return StateValidationResult.builder()
                    .valid(isValid)
                    .errorMessage(isValid ? null : String.join("; ", errors))
                    .validationErrors(errors)
                    .build();
                    
            } catch (Exception e) {
                log.error("Error validating updated state: {}", e.getMessage(), e);
                return StateValidationResult.builder()
                    .valid(false)
                    .errorMessage("Validation error: " + e.getMessage())
                    .validationErrors(List.of("Validation system error"))
                    .build();
            }
        });
    }

    /**
     * Validate vector properties
     */
    private void validateVector(List<Float> vector, String vectorName, List<String> errors) {
        if (vector == null) {
            errors.add(vectorName + " cannot be null");
            return;
        }
        
        if (vector.isEmpty()) {
            errors.add(vectorName + " cannot be empty");
            return;
        }
        
        if (vector.size() < MIN_VECTOR_SIZE) {
            errors.add(vectorName + " size must be at least " + MIN_VECTOR_SIZE);
        }
        
        if (vector.size() > MAX_VECTOR_SIZE) {
            errors.add(vectorName + " size must not exceed " + MAX_VECTOR_SIZE);
        }
        
        // Check for NaN or infinite values
        for (int i = 0; i < vector.size(); i++) {
            Float value = vector.get(i);
            if (value == null) {
                errors.add(vectorName + " contains null value at index " + i);
            } else if (Float.isNaN(value)) {
                errors.add(vectorName + " contains NaN value at index " + i);
            } else if (Float.isInfinite(value)) {
                errors.add(vectorName + " contains infinite value at index " + i);
            } else if (value < MIN_VECTOR_VALUE || value > MAX_VECTOR_VALUE) {
                errors.add(vectorName + " value at index " + i + " is out of range [" + 
                         MIN_VECTOR_VALUE + ", " + MAX_VECTOR_VALUE + "]");
            }
        }
    }

    /**
     * Validate observation type
     */
    private void validateObservationType(String observationType, List<String> errors) {
        if (observationType == null || observationType.trim().isEmpty()) {
            errors.add("observation_type cannot be null or empty");
            return;
        }
        
        // Check for valid observation types
        Set<String> validTypes = Set.of(
            "emotional_state", "behavioral_pattern", "cognitive_response",
            "social_interaction", "stress_level", "mood_indicator",
            "anxiety_level", "depression_indicator", "wellness_score"
        );
        
        if (!validTypes.contains(observationType.toLowerCase())) {
            errors.add("observation_type '" + observationType + "' is not recognized");
        }
    }

    /**
     * Validate context parameters
     */
    private void validateContext(Map<String, Object> context, List<String> errors) {
        if (context == null) {
            return; // Context is optional
        }
        
        // Validate context keys and values
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key == null || key.trim().isEmpty()) {
                errors.add("context key cannot be null or empty");
                continue;
            }
            
            if (value == null) {
                errors.add("context value for key '" + key + "' cannot be null");
                continue;
            }
            
            // Validate specific context parameters
            switch (key.toLowerCase()) {
                case "confidence":
                    if (!(value instanceof Number)) {
                        errors.add("context confidence must be a number");
                    } else {
                        double confidence = ((Number) value).doubleValue();
                        if (confidence < 0.0 || confidence > 1.0) {
                            errors.add("context confidence must be between 0.0 and 1.0");
                        }
                    }
                    break;
                case "emotional_state":
                    if (!(value instanceof String)) {
                        errors.add("context emotional_state must be a string");
                    }
                    break;
                case "timestamp":
                    if (!(value instanceof Number)) {
                        errors.add("context timestamp must be a number");
                    }
                    break;
            }
        }
    }

    /**
     * Validate state transition feasibility
     */
    private void validateStateTransition(List<Float> currentState, List<Float> newObservation, List<String> errors) {
        if (currentState == null || newObservation == null) {
            return; // Already handled by vector validation
        }
        
        if (currentState.size() != newObservation.size()) {
            errors.add("current_state and new_observation must have the same dimensions");
            return;
        }
        
        // Check for extreme state changes
        double maxChange = 0.0;
        for (int i = 0; i < currentState.size(); i++) {
            double change = Math.abs(currentState.get(i) - newObservation.get(i));
            maxChange = Math.max(maxChange, change);
        }
        
        if (maxChange > MAX_STATE_CHANGE_THRESHOLD) {
            errors.add("state change magnitude (" + maxChange + ") exceeds threshold (" + 
                      MAX_STATE_CHANGE_THRESHOLD + ")");
        }
    }

    /**
     * Validate state change magnitude
     */
    private void validateStateChangeMagnitude(List<Float> originalState, List<Float> updatedState, List<String> errors) {
        if (originalState == null || updatedState == null) {
            return;
        }
        
        if (originalState.size() != updatedState.size()) {
            errors.add("original_state and updated_state must have the same dimensions");
            return;
        }
        
        // Calculate average change magnitude
        double totalChange = 0.0;
        for (int i = 0; i < originalState.size(); i++) {
            totalChange += Math.abs(originalState.get(i) - updatedState.get(i));
        }
        
        double avgChange = totalChange / originalState.size();
        
        if (avgChange > MAX_STATE_CHANGE_THRESHOLD) {
            errors.add("average state change magnitude (" + avgChange + ") exceeds threshold (" + 
                      MAX_STATE_CHANGE_THRESHOLD + ")");
        }
    }

    /**
     * Validate state consistency
     */
    private void validateStateConsistency(List<Float> state, List<String> errors) {
        if (state == null || state.isEmpty()) {
            return;
        }
        
        // Check for monotonic consistency in psychological dimensions
        // This is a simplified check - in practice, you'd have domain-specific rules
        
        // Check for extreme values that might indicate data corruption
        long extremeCount = state.stream()
            .mapToLong(v -> (v == 0.0f || v == 1.0f) ? 1 : 0)
            .sum();
        
        if (extremeCount > state.size() * 0.8) {
            errors.add("state contains too many extreme values (0.0 or 1.0), possible data corruption");
        }
        
        // Check for uniform values (all same)
        Set<Float> uniqueValues = new HashSet<>(state);
        if (uniqueValues.size() == 1) {
            errors.add("state contains uniform values, possible data corruption");
        }
    }

    // DTO
    @lombok.Data
    @lombok.Builder
    public static class StateValidationResult {
        private boolean valid;
        private String errorMessage;
        private List<String> validationErrors;
    }
}
