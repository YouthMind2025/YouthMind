package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Local dictionary-based moderation service for first-layer content filtering.
 * Provides fast, offline content screening before AI analysis.
 */
@Service
public class LocalModerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalModerationService.class);
    
    // Profanity dictionary with severity levels
    private static final Map<String, Integer> PROFANITY_DICTIONARY = new HashMap<>();
    
    // Context-aware patterns for safe usage
    private static final List<Pattern> SAFE_CONTEXT_PATTERNS = new ArrayList<>();
    
    // Word masking patterns
    private static final Map<String, String> WORD_MASKING_MAP = new HashMap<>();
    
    static {
        initializeProfanityDictionary();
        initializeSafeContextPatterns();
        initializeWordMasking();
    }
    
    /**
     * Initialize profanity dictionary with severity levels
     * 1 = Mild (f*ck, sh*t)
     * 2 = Moderate (damn, hell)
     * 3 = Strong (explicit profanity)
     * 4 = Severe (hate speech, threats)
     */
    private static void initializeProfanityDictionary() {
        // Mild profanity
        PROFANITY_DICTIONARY.put("fuck", 1);
        PROFANITY_DICTIONARY.put("fucking", 1);
        PROFANITY_DICTIONARY.put("shit", 1);
        PROFANITY_DICTIONARY.put("damn", 2);
        PROFANITY_DICTIONARY.put("hell", 2);
        PROFANITY_DICTIONARY.put("bitch", 2);
        PROFANITY_DICTIONARY.put("ass", 2);
        
        // Strong profanity
        PROFANITY_DICTIONARY.put("cunt", 3);
        PROFANITY_DICTIONARY.put("whore", 3);
        PROFANITY_DICTIONARY.put("slut", 3);
        
        // Hate speech and severe
        PROFANITY_DICTIONARY.put("nigger", 4);
        PROFANITY_DICTIONARY.put("faggot", 4);
        PROFANITY_DICTIONARY.put("retard", 4);
        PROFANITY_DICTIONARY.put("kys", 4); // Kill yourself
        PROFANITY_DICTIONARY.put("kill yourself", 4);
        
        // Additional variations
        PROFANITY_DICTIONARY.put("fck", 1);
        PROFANITY_DICTIONARY.put("f*ck", 1);
        PROFANITY_DICTIONARY.put("f**k", 1);
        PROFANITY_DICTIONARY.put("sh*t", 1);
        PROFANITY_DICTIONARY.put("s**t", 1);
    }
    
    /**
     * Initialize patterns that indicate safe context for profanity
     */
    private static void initializeSafeContextPatterns() {
        // Patterns where profanity might be acceptable (e.g., "I did fucking well")
        SAFE_CONTEXT_PATTERNS.add(Pattern.compile("\\b(I|we|you|they)\\s+(did|are|were|will)\\s+\\w*\\s+(well|good|great|amazing|fantastic)\\b", Pattern.CASE_INSENSITIVE));
        SAFE_CONTEXT_PATTERNS.add(Pattern.compile("\\b(that|this|it)\\s+(is|was|will be)\\s+(so|really|very)\\s+(good|great|amazing|fantastic)\\b", Pattern.CASE_INSENSITIVE));
        SAFE_CONTEXT_PATTERNS.add(Pattern.compile("\\b(feeling|feels)\\s+(so|really|very)\\s+(good|great|amazing|fantastic)\\b", Pattern.CASE_INSENSITIVE));
        
        // Educational or therapeutic contexts
        SAFE_CONTEXT_PATTERNS.add(Pattern.compile("\\b(learning|understanding|dealing with|working through)\\b", Pattern.CASE_INSENSITIVE));
        SAFE_CONTEXT_PATTERNS.add(Pattern.compile("\\b(mental health|therapy|counseling|support)\\b", Pattern.CASE_INSENSITIVE));
        
        // Self-expression contexts
        SAFE_CONTEXT_PATTERNS.add(Pattern.compile("\\b(I feel|I'm feeling|I am feeling)\\b", Pattern.CASE_INSENSITIVE));
        SAFE_CONTEXT_PATTERNS.add(Pattern.compile("\\b(struggling|having a hard time|going through)\\b", Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Initialize word masking patterns
     */
    private static void initializeWordMasking() {
        WORD_MASKING_MAP.put("fuck", "f*ck");
        WORD_MASKING_MAP.put("fucking", "f*cking");
        WORD_MASKING_MAP.put("shit", "sh*t");
        WORD_MASKING_MAP.put("damn", "d*mn");
        WORD_MASKING_MAP.put("bitch", "b*tch");
        WORD_MASKING_MAP.put("ass", "a**");
        WORD_MASKING_MAP.put("hell", "h*ll");
    }
    
    /**
     * Analyze content using local dictionary-based moderation
     * 
     * @param content The content to analyze
     * @return LocalModerationAnalysis result
     */
    public LocalModerationAnalysis analyzeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new LocalModerationAnalysis(true, 0, new ArrayList<>(), new ArrayList<>(), content);
        }
        
        logger.debug("Analyzing content with local moderation: {}", content.substring(0, Math.min(100, content.length())));
        
        List<DetectedWord> detectedWords = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        int maxSeverity = 0;
        
        String lowerContent = content.toLowerCase();
        
        // Check for profanity
        for (Map.Entry<String, Integer> entry : PROFANITY_DICTIONARY.entrySet()) {
            String word = entry.getKey();
            int severity = entry.getValue();
            
            if (lowerContent.contains(word)) {
                DetectedWord detectedWord = new DetectedWord(word, severity, findWordContext(content, word));
                
                // Check if word is in safe context
                if (isInSafeContext(content, word)) {
                    detectedWord.setSafeContext(true);
                    detectedWord.setSeverity(Math.max(1, severity - 1)); // Reduce severity for safe context
                }
                
                detectedWords.add(detectedWord);
                maxSeverity = Math.max(maxSeverity, detectedWord.getSeverity());
                
                // Add category based on severity
                if (severity >= 4) {
                    categories.add("HATE_SPEECH");
                } else if (severity >= 3) {
                    categories.add("EXPLICIT");
                } else if (severity >= 2) {
                    categories.add("INAPPROPRIATE");
                } else {
                    categories.add("MILD_PROFANITY");
                }
            }
        }
        
        // Check for additional patterns
        checkForAdditionalPatterns(content, detectedWords, categories);
        
        boolean isSafe = maxSeverity <= 2; // Allow mild profanity
        double confidence = calculateConfidence(detectedWords, content);
        
        return new LocalModerationAnalysis(isSafe, maxSeverity, detectedWords, categories, content);
    }
    
    /**
     * Check for additional harmful patterns
     */
    private void checkForAdditionalPatterns(String content, List<DetectedWord> detectedWords, List<String> categories) {
        String lowerContent = content.toLowerCase();
        
        // Self-harm indicators
        if (lowerContent.contains("kill myself") || lowerContent.contains("end my life") || 
            lowerContent.contains("hurt myself") || lowerContent.contains("suicide")) {
            categories.add("SELF_HARM");
            detectedWords.add(new DetectedWord("self-harm", 4, "Potential self-harm language detected"));
        }
        
        // Violence indicators
        if (lowerContent.contains("kill you") || lowerContent.contains("hurt you") || 
            lowerContent.contains("beat you") || lowerContent.contains("attack")) {
            categories.add("VIOLENCE");
            detectedWords.add(new DetectedWord("violence", 4, "Potential violence language detected"));
        }
        
        // Spam indicators
        if (lowerContent.contains("click here") || lowerContent.contains("free money") || 
            lowerContent.contains("make money fast") || lowerContent.contains("bit.ly")) {
            categories.add("SPAM");
            detectedWords.add(new DetectedWord("spam", 3, "Potential spam content detected"));
        }
    }
    
    /**
     * Check if a word appears in a safe context
     */
    private boolean isInSafeContext(String content, String word) {
        for (Pattern pattern : SAFE_CONTEXT_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Find the context around a detected word
     */
    private String findWordContext(String content, String word) {
        int index = content.toLowerCase().indexOf(word);
        if (index == -1) return "";
        
        int start = Math.max(0, index - 20);
        int end = Math.min(content.length(), index + word.length() + 20);
        
        return content.substring(start, end);
    }
    
    /**
     * Calculate confidence based on detected words and content
     */
    private double calculateConfidence(List<DetectedWord> detectedWords, String content) {
        if (detectedWords.isEmpty()) {
            return 1.0; // High confidence for clean content
        }
        
        double totalSeverity = detectedWords.stream()
            .mapToDouble(DetectedWord::getSeverity)
            .sum();
        
        double avgSeverity = totalSeverity / detectedWords.size();
        double contentLength = content.length();
        
        // Adjust confidence based on severity and content length
        double baseConfidence = Math.max(0.1, 1.0 - (avgSeverity / 4.0));
        double lengthAdjustment = Math.min(1.0, contentLength / 100.0);
        
        return Math.min(1.0, baseConfidence * lengthAdjustment);
    }
    
    /**
     * Mask profanity in content
     * 
     * @param content The content to mask
     * @return Masked content
     */
    public String maskContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String maskedContent = content;
        
        for (Map.Entry<String, String> entry : WORD_MASKING_MAP.entrySet()) {
            String originalWord = entry.getKey();
            String maskedWord = entry.getValue();
            
            // Use case-insensitive replacement
            maskedContent = maskedContent.replaceAll("(?i)\\b" + Pattern.quote(originalWord) + "\\b", maskedWord);
        }
        
        return maskedContent;
    }
    
    /**
     * Check if content needs masking
     */
    public boolean needsMasking(String content) {
        LocalModerationAnalysis analysis = analyzeContent(content);
        return !analysis.getDetectedWords().isEmpty() && analysis.getMaxSeverity() >= 1;
    }
    
    /**
     * Get detected words that need masking
     */
    public List<String> getWordsToMask(String content) {
        LocalModerationAnalysis analysis = analyzeContent(content);
        return analysis.getDetectedWords().stream()
            .filter(word -> word.getSeverity() >= 1)
            .map(DetectedWord::getWord)
            .distinct()
            .toList();
    }
    
    /**
     * Result of local moderation analysis
     */
    public static class LocalModerationAnalysis {
        private final boolean isSafe;
        private final int maxSeverity;
        private final List<DetectedWord> detectedWords;
        private final List<String> categories;
        private final String originalContent;
        
        public LocalModerationAnalysis(boolean isSafe, int maxSeverity, List<DetectedWord> detectedWords, 
                                     List<String> categories, String originalContent) {
            this.isSafe = isSafe;
            this.maxSeverity = maxSeverity;
            this.detectedWords = detectedWords;
            this.categories = categories;
            this.originalContent = originalContent;
        }
        
        // Getters
        public boolean isSafe() { return isSafe; }
        public int getMaxSeverity() { return maxSeverity; }
        public List<DetectedWord> getDetectedWords() { return detectedWords; }
        public List<String> getCategories() { return categories; }
        public String getOriginalContent() { return originalContent; }
        
        public boolean hasDetectedWords() { return !detectedWords.isEmpty(); }
        public boolean needsMasking() { return maxSeverity >= 1; }
    }
    
    /**
     * Represents a detected word with context
     */
    public static class DetectedWord {
        private final String word;
        private int severity;
        private final String context;
        private boolean safeContext = false;
        
        public DetectedWord(String word, int severity, String context) {
            this.word = word;
            this.severity = severity;
            this.context = context;
        }
        
        // Getters and setters
        public String getWord() { return word; }
        public int getSeverity() { return severity; }
        public String getContext() { return context; }
        public boolean isSafeContext() { return safeContext; }
        
        public void setSeverity(int severity) { this.severity = severity; }
        public void setSafeContext(boolean safeContext) { this.safeContext = safeContext; }
    }
}
