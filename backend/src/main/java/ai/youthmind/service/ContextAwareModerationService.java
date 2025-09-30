package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Context-aware moderation service that analyzes content in context
 * to determine if profanity or potentially harmful content is actually safe.
 */
@Service
public class ContextAwareModerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextAwareModerationService.class);
    
    @Autowired
    private LocalModerationService localModerationService;
    
    // Context patterns for safe profanity usage
    private static final List<ContextPattern> SAFE_CONTEXT_PATTERNS = new ArrayList<>();
    
    // Gen Z language patterns that are generally acceptable
    private static final List<Pattern> GEN_Z_PATTERNS = new ArrayList<>();
    
    // Therapeutic and supportive context indicators
    private static final List<Pattern> THERAPEUTIC_CONTEXT_PATTERNS = new ArrayList<>();
    
    static {
        initializeContextPatterns();
        initializeGenZPatterns();
        initializeTherapeuticPatterns();
    }
    
    /**
     * Initialize context patterns for safe profanity usage
     */
    private static void initializeContextPatterns() {
        // Positive achievement contexts
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(I|we|you|they)\\s+(did|are|were|will)\\s+\\w*\\s+(well|good|great|amazing|fantastic|incredible|awesome)\\b", Pattern.CASE_INSENSITIVE),
            "positive_achievement", 0.8
        ));
        
        // Emotional expression contexts
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(feeling|feels|feel)\\s+(so|really|very|extremely)\\s+(good|great|amazing|fantastic|happy|excited|relieved)\\b", Pattern.CASE_INSENSITIVE),
            "emotional_expression", 0.9
        ));
        
        // Frustration expression (common in mental health contexts)
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(I'm|I am|we're|we are)\\s+(so|really|very)\\s+(frustrated|angry|upset|annoyed|mad)\\b", Pattern.CASE_INSENSITIVE),
            "frustration_expression", 0.7
        ));
        
        // Support and encouragement contexts
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(you|they)\\s+(did|are doing|are)\\s+(so|really|very)\\s+(well|good|great|amazing)\\b", Pattern.CASE_INSENSITIVE),
            "support_expression", 0.9
        ));
        
        // Self-reflection and growth contexts
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(learning|understanding|realizing|figuring out|working through)\\b", Pattern.CASE_INSENSITIVE),
            "self_reflection", 0.8
        ));
        
        // Mental health and therapy contexts
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(mental health|therapy|counseling|support|treatment|recovery|healing)\\b", Pattern.CASE_INSENSITIVE),
            "mental_health_context", 0.95
        ));
        
        // Personal struggle contexts
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(struggling|having a hard time|going through|dealing with|working on)\\b", Pattern.CASE_INSENSITIVE),
            "personal_struggle", 0.8
        ));
        
        // Casual conversation contexts
        SAFE_CONTEXT_PATTERNS.add(new ContextPattern(
            Pattern.compile("\\b(btw|by the way|honestly|tbh|to be honest|imo|in my opinion)\\b", Pattern.CASE_INSENSITIVE),
            "casual_conversation", 0.6
        ));
    }
    
    /**
     * Initialize Gen Z language patterns
     */
    private static void initializeGenZPatterns() {
        // Common Gen Z expressions that might contain mild profanity
        GEN_Z_PATTERNS.add(Pattern.compile("\\b(no cap|fr|for real|ngl|not gonna lie)\\b", Pattern.CASE_INSENSITIVE));
        GEN_Z_PATTERNS.add(Pattern.compile("\\b(this is|that's|it's)\\s+(so|really|very)\\s+(fire|lit|sick|dope|slaps)\\b", Pattern.CASE_INSENSITIVE));
        GEN_Z_PATTERNS.add(Pattern.compile("\\b(I'm|I am)\\s+(dead|dying)\\s+(laughing|from)\\b", Pattern.CASE_INSENSITIVE));
        GEN_Z_PATTERNS.add(Pattern.compile("\\b(period|periodt|facts|no lies detected)\\b", Pattern.CASE_INSENSITIVE));
        GEN_Z_PATTERNS.add(Pattern.compile("\\b(stan|staning|stanning)\\b", Pattern.CASE_INSENSITIVE));
        GEN_Z_PATTERNS.add(Pattern.compile("\\b(goals|mood|vibe|vibes|energy)\\b", Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Initialize therapeutic context patterns
     */
    private static void initializeTherapeuticPatterns() {
        THERAPEUTIC_CONTEXT_PATTERNS.add(Pattern.compile("\\b(session|appointment|meeting)\\s+(with|with my)\\s+(therapist|counselor|doctor)\\b", Pattern.CASE_INSENSITIVE));
        THERAPEUTIC_CONTEXT_PATTERNS.add(Pattern.compile("\\b(talking about|discussing|working on)\\s+(my|our)\\s+(feelings|emotions|thoughts)\\b", Pattern.CASE_INSENSITIVE));
        THERAPEUTIC_CONTEXT_PATTERNS.add(Pattern.compile("\\b(coping|dealing with|managing)\\s+(stress|anxiety|depression|trauma)\\b", Pattern.CASE_INSENSITIVE));
        THERAPEUTIC_CONTEXT_PATTERNS.add(Pattern.compile("\\b(support group|group therapy|peer support)\\b", Pattern.CASE_INSENSITIVE));
        THERAPEUTIC_CONTEXT_PATTERNS.add(Pattern.compile("\\b(breakthrough|progress|improvement|recovery)\\b", Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Analyze content with context awareness
     * 
     * @param content The content to analyze
     * @return ContextAwareAnalysis result
     */
    public ContextAwareAnalysis analyzeWithContext(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ContextAwareAnalysis(true, 1.0, new ArrayList<>(), new ArrayList<>(), content);
        }
        
        logger.debug("Performing context-aware analysis: {}", content.substring(0, Math.min(100, content.length())));
        
        // First, get local moderation analysis
        LocalModerationService.LocalModerationAnalysis localAnalysis = localModerationService.analyzeContent(content);
        
        if (localAnalysis.isSafe()) {
            return new ContextAwareAnalysis(true, 1.0, new ArrayList<>(), new ArrayList<>(), content);
        }
        
        // Analyze context for each detected word
        List<ContextualWord> contextualWords = new ArrayList<>();
        List<String> contextTypes = new ArrayList<>();
        double overallContextScore = 0.0;
        int contextCount = 0;
        
        for (LocalModerationService.DetectedWord detectedWord : localAnalysis.getDetectedWords()) {
            ContextualWord contextualWord = analyzeWordContext(content, detectedWord);
            contextualWords.add(contextualWord);
            
            if (contextualWord.getContextScore() > 0) {
                overallContextScore += contextualWord.getContextScore();
                contextCount++;
                contextTypes.addAll(contextualWord.getContextTypes());
            }
        }
        
        // Calculate overall safety based on context
        boolean isSafe = determineOverallSafety(localAnalysis, contextualWords, overallContextScore, contextCount);
        double confidence = calculateContextConfidence(localAnalysis, contextualWords, overallContextScore, contextCount);
        
        return new ContextAwareAnalysis(isSafe, confidence, contextualWords, contextTypes, content);
    }
    
    /**
     * Analyze context for a specific detected word
     */
    private ContextualWord analyzeWordContext(String content, LocalModerationService.DetectedWord detectedWord) {
        String word = detectedWord.getWord();
        int originalSeverity = detectedWord.getSeverity();
        List<String> contextTypes = new ArrayList<>();
        double contextScore = 0.0;
        
        // Check against safe context patterns
        for (ContextPattern pattern : SAFE_CONTEXT_PATTERNS) {
            if (pattern.getPattern().matcher(content).find()) {
                contextTypes.add(pattern.getContextType());
                contextScore += pattern.getScore();
            }
        }
        
        // Check Gen Z patterns
        for (Pattern genZPattern : GEN_Z_PATTERNS) {
            if (genZPattern.matcher(content).find()) {
                contextTypes.add("gen_z_language");
                contextScore += 0.6;
            }
        }
        
        // Check therapeutic patterns
        for (Pattern therapeuticPattern : THERAPEUTIC_CONTEXT_PATTERNS) {
            if (therapeuticPattern.matcher(content).find()) {
                contextTypes.add("therapeutic_context");
                contextScore += 0.8;
            }
        }
        
        // Check for positive sentiment indicators
        if (hasPositiveSentiment(content)) {
            contextTypes.add("positive_sentiment");
            contextScore += 0.5;
        }
        
        // Check for personal sharing context
        if (isPersonalSharingContext(content)) {
            contextTypes.add("personal_sharing");
            contextScore += 0.7;
        }
        
        // Adjust severity based on context
        int adjustedSeverity = originalSeverity;
        if (contextScore > 0.7) {
            adjustedSeverity = Math.max(1, originalSeverity - 2); // Significant reduction
        } else if (contextScore > 0.4) {
            adjustedSeverity = Math.max(1, originalSeverity - 1); // Moderate reduction
        }
        
        return new ContextualWord(word, originalSeverity, adjustedSeverity, contextScore, contextTypes, detectedWord.getContext());
    }
    
    /**
     * Check if content has positive sentiment
     */
    private boolean hasPositiveSentiment(String content) {
        String lowerContent = content.toLowerCase();
        String[] positiveWords = {"good", "great", "amazing", "fantastic", "awesome", "wonderful", "excellent", "love", "happy", "excited", "grateful", "thankful", "proud", "accomplished", "achieved", "success", "progress", "improvement", "better", "healing", "recovery"};
        
        int positiveCount = 0;
        for (String word : positiveWords) {
            if (lowerContent.contains(word)) {
                positiveCount++;
            }
        }
        
        return positiveCount >= 2; // At least 2 positive words
    }
    
    /**
     * Check if content is in personal sharing context
     */
    private boolean isPersonalSharingContext(String content) {
        String lowerContent = content.toLowerCase();
        String[] personalIndicators = {"i feel", "i'm feeling", "i am feeling", "my experience", "i went through", "i struggled", "i learned", "i realized", "i understand", "i know how", "i've been", "i have been"};
        
        for (String indicator : personalIndicators) {
            if (lowerContent.contains(indicator)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determine overall safety based on context analysis
     */
    private boolean determineOverallSafety(LocalModerationService.LocalModerationAnalysis localAnalysis, 
                                         List<ContextualWord> contextualWords, 
                                         double overallContextScore, int contextCount) {
        // If no profanity detected, it's safe
        if (localAnalysis.getDetectedWords().isEmpty()) {
            return true;
        }
        
        // If high context score, likely safe
        if (overallContextScore > 1.5) {
            return true;
        }
        
        // Check if all words have been significantly reduced in severity
        boolean allWordsReduced = contextualWords.stream()
            .allMatch(word -> word.getAdjustedSeverity() <= 2);
        
        if (allWordsReduced && contextCount > 0) {
            return true;
        }
        
        // Check for severe content that shouldn't be allowed regardless of context
        boolean hasSevereContent = contextualWords.stream()
            .anyMatch(word -> word.getOriginalSeverity() >= 4 && word.getAdjustedSeverity() >= 3);
        
        if (hasSevereContent) {
            return false;
        }
        
        // Default to local analysis result
        return localAnalysis.isSafe();
    }
    
    /**
     * Calculate confidence based on context analysis
     */
    private double calculateContextConfidence(LocalModerationService.LocalModerationAnalysis localAnalysis, 
                                            List<ContextualWord> contextualWords, 
                                            double overallContextScore, int contextCount) {
        if (localAnalysis.getDetectedWords().isEmpty()) {
            return 1.0; // High confidence for clean content
        }
        
        double baseConfidence = 0.5; // Start with moderate confidence
        
        // Increase confidence based on context score
        if (overallContextScore > 1.0) {
            baseConfidence += 0.3;
        } else if (overallContextScore > 0.5) {
            baseConfidence += 0.2;
        }
        
        // Increase confidence if multiple context types found
        if (contextCount > 1) {
            baseConfidence += 0.1;
        }
        
        // Increase confidence if all words reduced in severity
        boolean allWordsReduced = contextualWords.stream()
            .allMatch(word -> word.getAdjustedSeverity() < word.getOriginalSeverity());
        
        if (allWordsReduced) {
            baseConfidence += 0.2;
        }
        
        return Math.min(1.0, baseConfidence);
    }
    
    /**
     * Context pattern for safe usage
     */
    private static class ContextPattern {
        private final Pattern pattern;
        private final String contextType;
        private final double score;
        
        public ContextPattern(Pattern pattern, String contextType, double score) {
            this.pattern = pattern;
            this.contextType = contextType;
            this.score = score;
        }
        
        public Pattern getPattern() { return pattern; }
        public String getContextType() { return contextType; }
        public double getScore() { return score; }
    }
    
    /**
     * Result of context-aware analysis
     */
    public static class ContextAwareAnalysis {
        private final boolean isSafe;
        private final double confidence;
        private final List<ContextualWord> contextualWords;
        private final List<String> contextTypes;
        private final String originalContent;
        
        public ContextAwareAnalysis(boolean isSafe, double confidence, List<ContextualWord> contextualWords, 
                                  List<String> contextTypes, String originalContent) {
            this.isSafe = isSafe;
            this.confidence = confidence;
            this.contextualWords = contextualWords;
            this.contextTypes = contextTypes;
            this.originalContent = originalContent;
        }
        
        // Getters
        public boolean isSafe() { return isSafe; }
        public double getConfidence() { return confidence; }
        public List<ContextualWord> getContextualWords() { return contextualWords; }
        public List<String> getContextTypes() { return contextTypes; }
        public String getOriginalContent() { return originalContent; }
        
        public boolean hasContextualWords() { return !contextualWords.isEmpty(); }
        public boolean needsMasking() { return contextualWords.stream().anyMatch(word -> word.getAdjustedSeverity() >= 1); }
    }
    
    /**
     * Represents a word with contextual analysis
     */
    public static class ContextualWord {
        private final String word;
        private final int originalSeverity;
        private final int adjustedSeverity;
        private final double contextScore;
        private final List<String> contextTypes;
        private final String context;
        
        public ContextualWord(String word, int originalSeverity, int adjustedSeverity, 
                             double contextScore, List<String> contextTypes, String context) {
            this.word = word;
            this.originalSeverity = originalSeverity;
            this.adjustedSeverity = adjustedSeverity;
            this.contextScore = contextScore;
            this.contextTypes = contextTypes;
            this.context = context;
        }
        
        // Getters
        public String getWord() { return word; }
        public int getOriginalSeverity() { return originalSeverity; }
        public int getAdjustedSeverity() { return adjustedSeverity; }
        public double getContextScore() { return contextScore; }
        public List<String> getContextTypes() { return contextTypes; }
        public String getContext() { return context; }
        
        public boolean wasSeverityReduced() { return adjustedSeverity < originalSeverity; }
    }
}
