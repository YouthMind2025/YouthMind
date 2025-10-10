package ai.youthmind.events.model;

/**
 * Enumeration of emotions for the recommendation engine.
 * Matches the existing emotion values used in Post entity: angry, sad, happy, conflicted, surprised, fear
 */
public enum Emotion {
    HAPPY("happy", "Positive emotions like happiness, contentment, joy"),
    SAD("sad", "Negative emotions like grief, disappointment, melancholy"),
    ANGRY("angry", "Intense negative emotions like rage, frustration, irritation"),
    FEAR("fear", "Anxiety, worry, apprehension, nervousness"),
    SURPRISED("surprised", "Unexpected emotions, shock, amazement"),
    CONFLICTED("conflicted", "Mixed or contradictory emotions, uncertainty");

    private final String value;
    private final String description;

    Emotion(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse emotion from string value.
     * Returns a default emotion (HAPPY) instead of throwing exception for invalid values.
     */
    public static Emotion fromValue(String value) {
        if (value == null) {
            return HAPPY; // Default fallback
        }
        
        for (Emotion emotion : Emotion.values()) {
            if (emotion.value.equalsIgnoreCase(value)) {
                return emotion;
            }
        }
        
        // Fallback to HAPPY for unknown emotions instead of throwing exception
        return HAPPY;
    }

    /**
     * Check if a string represents a valid emotion
     */
    public static boolean isValid(String value) {
        if (value == null) return false;
        
        for (Emotion emotion : Emotion.values()) {
            if (emotion.value.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return value;
    }
}
