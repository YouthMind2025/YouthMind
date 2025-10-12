package ai.youthmind.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Emotion enum for post classification and recommendation engine
 * Priority ordering used for tie-breaking when selecting top emotions
 */
public enum Emotion {
    CONFLICTED(1),  // Highest priority
    SURPRISED(2),
    ANGRY(3),
    SAD(4),
    FEAR(5),
    HAPPY(6);       // Lowest priority
    
    private final int priority;
    
    Emotion(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() {
        return priority;
    }
    
    /**
     * Convert string to Emotion enum (case-insensitive)
     * @param emotion String representation of emotion
     * @return Emotion enum or null if invalid
     */
    public static Emotion fromString(String emotion) {
        if (emotion == null) {
            return null;
        }
        try {
            return Emotion.valueOf(emotion.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Get emotions ordered by priority (for tie-breaking)
     * @return List of emotions in priority order
     */
    public static List<Emotion> getPriorityOrder() {
        return Arrays.stream(Emotion.values())
                .sorted(Comparator.comparingInt(Emotion::getPriority))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if string is a valid emotion
     * @param emotion String to validate
     * @return true if valid emotion
     */
    public static boolean isValid(String emotion) {
        return fromString(emotion) != null;
    }
    
    /**
     * Get all emotion names as lowercase strings
     * @return List of emotion names
     */
    public static List<String> getAllNames() {
        return Arrays.stream(Emotion.values())
                .map(e -> e.name().toLowerCase())
                .collect(Collectors.toList());
    }
    
    /**
     * Get lowercase string representation
     * @return lowercase emotion name
     */
    public String toLowerCase() {
        return this.name().toLowerCase();
    }
}

