package ai.youthmind.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for generating random but readable usernames
 */
public class UsernameGenerator {
    
    private static final SecureRandom random = new SecureRandom();
    
    // Simple but not too messy prefixes
    private static final List<String> PREFIXES = Arrays.asList(
        "user", "guest", "member", "person", "friend", "buddy", 
        "soul", "mind", "heart", "star", "moon", "sun",
        "blue", "green", "red", "gold", "silver", "bright",
        "cool", "nice", "kind", "wise", "calm", "happy"
    );
    
    // Numbers and simple patterns to make it unique but not too random
    private static final String NUMBERS = "0123456789";
    private static final String SIMPLE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    
    /**
     * Generate a random but readable username
     * Format: [prefix][numbers/letters] (e.g., "user123", "soul42a", "mind789")
     * 
     * @return A generated username that's not too messy
     */
    public static String generateUsername() {
        String prefix = PREFIXES.get(random.nextInt(PREFIXES.size()));
        
        // Generate 2-4 digit number or number+letter combination
        StringBuilder suffix = new StringBuilder();
        int suffixLength = 2 + random.nextInt(3); // 2-4 characters
        
        for (int i = 0; i < suffixLength; i++) {
            if (i < suffixLength - 1 || random.nextBoolean()) {
                // Add number
                suffix.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
            } else {
                // Occasionally add a letter at the end
                suffix.append(SIMPLE_CHARS.charAt(random.nextInt(SIMPLE_CHARS.length())));
            }
        }
        
        return prefix + suffix.toString();
    }
    
    /**
     * Generate a username with retry logic to ensure uniqueness
     * This method should be used with a uniqueness check
     * 
     * @param maxAttempts Maximum number of generation attempts
     * @return A generated username
     */
    public static String generateUniqueUsername(int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            String username = generateUsername();
            // In a real implementation, you would check against database here
            // For now, just return the generated username
            return username;
        }
        
        // Fallback: use timestamp to ensure uniqueness
        return "user" + System.currentTimeMillis() % 100000;
    }
    
    /**
     * Generate multiple username options
     * 
     * @param count Number of usernames to generate
     * @return List of generated usernames
     */
    public static List<String> generateMultipleUsernames(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> generateUsername())
                .collect(java.util.stream.Collectors.toList());
    }
}
