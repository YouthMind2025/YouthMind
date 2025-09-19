package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service for masking profanity and inappropriate words in content.
 * Provides various masking strategies and user consent handling.
 */
@Service
public class WordMaskingService {
    
    private static final Logger logger = LoggerFactory.getLogger(WordMaskingService.class);
    
    @Autowired
    private LocalModerationService localModerationService;
    
    @Autowired
    private ContextAwareModerationService contextAwareModerationService;
    
    // Word masking strategies
    public enum MaskingStrategy {
        ASTERISK_MASKING,    // f*ck, sh*t
        PARTIAL_MASKING,    // f***, s***
        FULL_MASKING,       // ****
        REPLACEMENT,        // Replace with alternatives
        CONSENT_BASED       // Ask user for consent
    }
    
    // Word masking map with different strategies
    private static final Map<String, Map<MaskingStrategy, String>> WORD_MASKING_MAP = new HashMap<>();
    
    // Alternative word replacements
    private static final Map<String, String> WORD_REPLACEMENTS = new HashMap<>();
    
    static {
        initializeWordMasking();
        initializeWordReplacements();
    }
    
    /**
     * Initialize word masking patterns for different strategies
     */
    private static void initializeWordMasking() {
        // F-word variations
        Map<MaskingStrategy, String> fuckMasks = new HashMap<>();
        fuckMasks.put(MaskingStrategy.ASTERISK_MASKING, "f*ck");
        fuckMasks.put(MaskingStrategy.PARTIAL_MASKING, "f***");
        fuckMasks.put(MaskingStrategy.FULL_MASKING, "****");
        fuckMasks.put(MaskingStrategy.REPLACEMENT, "freak");
        WORD_MASKING_MAP.put("fuck", fuckMasks);
        WORD_MASKING_MAP.put("fucking", Map.of(
            MaskingStrategy.ASTERISK_MASKING, "f*cking",
            MaskingStrategy.PARTIAL_MASKING, "f******",
            MaskingStrategy.FULL_MASKING, "*******",
            MaskingStrategy.REPLACEMENT, "freaking"
        ));
        
        // S-word variations
        Map<MaskingStrategy, String> shitMasks = new HashMap<>();
        shitMasks.put(MaskingStrategy.ASTERISK_MASKING, "sh*t");
        shitMasks.put(MaskingStrategy.PARTIAL_MASKING, "s***");
        shitMasks.put(MaskingStrategy.FULL_MASKING, "****");
        shitMasks.put(MaskingStrategy.REPLACEMENT, "stuff");
        WORD_MASKING_MAP.put("shit", shitMasks);
        
        // Other common profanity
        WORD_MASKING_MAP.put("damn", Map.of(
            MaskingStrategy.ASTERISK_MASKING, "d*mn",
            MaskingStrategy.PARTIAL_MASKING, "d***",
            MaskingStrategy.FULL_MASKING, "****",
            MaskingStrategy.REPLACEMENT, "darn"
        ));
        
        WORD_MASKING_MAP.put("bitch", Map.of(
            MaskingStrategy.ASTERISK_MASKING, "b*tch",
            MaskingStrategy.PARTIAL_MASKING, "b****",
            MaskingStrategy.FULL_MASKING, "*****",
            MaskingStrategy.REPLACEMENT, "person"
        ));
        
        WORD_MASKING_MAP.put("ass", Map.of(
            MaskingStrategy.ASTERISK_MASKING, "a**",
            MaskingStrategy.PARTIAL_MASKING, "a**",
            MaskingStrategy.FULL_MASKING, "***",
            MaskingStrategy.REPLACEMENT, "butt"
        ));
        
        WORD_MASKING_MAP.put("hell", Map.of(
            MaskingStrategy.ASTERISK_MASKING, "h*ll",
            MaskingStrategy.PARTIAL_MASKING, "h***",
            MaskingStrategy.FULL_MASKING, "****",
            MaskingStrategy.REPLACEMENT, "heck"
        ));
    }
    
    /**
     * Initialize word replacements for cleaner alternatives
     */
    private static void initializeWordReplacements() {
        WORD_REPLACEMENTS.put("fuck", "freak");
        WORD_REPLACEMENTS.put("fucking", "freaking");
        WORD_REPLACEMENTS.put("shit", "stuff");
        WORD_REPLACEMENTS.put("damn", "darn");
        WORD_REPLACEMENTS.put("bitch", "person");
        WORD_REPLACEMENTS.put("ass", "butt");
        WORD_REPLACEMENTS.put("hell", "heck");
        WORD_REPLACEMENTS.put("crap", "stuff");
        WORD_REPLACEMENTS.put("stupid", "silly");
        WORD_REPLACEMENTS.put("idiot", "person");
    }
    
    /**
     * Mask content using the specified strategy
     * 
     * @param content The content to mask
     * @param strategy The masking strategy to use
     * @return MaskingResult with masked content and details
     */
    public MaskingResult maskContent(String content, MaskingStrategy strategy) {
        if (content == null || content.trim().isEmpty()) {
            return new MaskingResult(content, new ArrayList<>(), new ArrayList<>(), false);
        }
        
        logger.debug("Masking content with strategy: {}", strategy);
        
        // First, analyze content to find words that need masking
        LocalModerationService.LocalModerationAnalysis localAnalysis = localModerationService.analyzeContent(content);
        ContextAwareModerationService.ContextAwareAnalysis contextAnalysis = contextAwareModerationService.analyzeWithContext(content);
        
        if (!localAnalysis.hasDetectedWords()) {
            return new MaskingResult(content, new ArrayList<>(), new ArrayList<>(), false);
        }
        
        String maskedContent = content;
        List<MaskedWord> maskedWords = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Process each detected word
        for (LocalModerationService.DetectedWord detectedWord : localAnalysis.getDetectedWords()) {
            String word = detectedWord.getWord();
            int severity = detectedWord.getSeverity();
            
            // Check if word should be masked based on context
            boolean shouldMask = shouldMaskWord(word, severity, contextAnalysis);
            
            if (shouldMask) {
                String maskedWord = getMaskedWord(word, strategy);
                if (maskedWord != null && !maskedWord.equals(word)) {
                    maskedContent = maskWordInContent(maskedContent, word, maskedWord);
                    maskedWords.add(new MaskedWord(word, maskedWord, severity, strategy));
                    
                    // Add warning for severe content
                    if (severity >= 3) {
                        warnings.add("Content contains strong language that has been masked");
                    }
                }
            }
        }
        
        boolean contentModified = !maskedContent.equals(content);
        
        return new MaskingResult(maskedContent, maskedWords, warnings, contentModified);
    }
    
    /**
     * Determine if a word should be masked based on context
     */
    private boolean shouldMaskWord(String word, int severity, ContextAwareModerationService.ContextAwareAnalysis contextAnalysis) {
        // Always mask severe content (hate speech, threats)
        if (severity >= 4) {
            return true;
        }
        
        // Check if word was reduced in severity due to context
        Optional<ContextAwareModerationService.ContextualWord> contextualWord = contextAnalysis.getContextualWords().stream()
            .filter(cw -> cw.getWord().equals(word))
            .findFirst();
        
        if (contextualWord.isPresent()) {
            ContextAwareModerationService.ContextualWord cw = contextualWord.get();
            
            // If severity was significantly reduced due to context, don't mask
            if (cw.wasSeverityReduced() && cw.getAdjustedSeverity() <= 1) {
                return false;
            }
            
            // If in therapeutic or mental health context, be more lenient
            if (cw.getContextTypes().contains("therapeutic_context") || 
                cw.getContextTypes().contains("mental_health_context")) {
                return cw.getAdjustedSeverity() >= 2;
            }
        }
        
        // Default masking rules
        return severity >= 2;
    }
    
    /**
     * Get masked version of a word using the specified strategy
     */
    private String getMaskedWord(String word, MaskingStrategy strategy) {
        Map<MaskingStrategy, String> masks = WORD_MASKING_MAP.get(word.toLowerCase());
        if (masks == null) {
            return word; // No masking available
        }
        
        return masks.get(strategy);
    }
    
    /**
     * Mask a specific word in content
     */
    private String maskWordInContent(String content, String originalWord, String maskedWord) {
        // Use case-insensitive replacement with word boundaries
        return content.replaceAll("(?i)\\b" + Pattern.quote(originalWord) + "\\b", maskedWord);
    }
    
    /**
     * Mask content with user consent flow
     * 
     * @param content The content to mask
     * @param userId The user ID for consent tracking
     * @return ConsentMaskingResult with masking options and consent request
     */
    public ConsentMaskingResult maskContentWithConsent(String content, UUID userId) {
        if (content == null || content.trim().isEmpty()) {
            return new ConsentMaskingResult(content, new ArrayList<>(), new ArrayList<>(), false, false);
        }
        
        logger.debug("Processing content for consent-based masking: {}", userId);
        
        // Analyze content
        LocalModerationService.LocalModerationAnalysis localAnalysis = localModerationService.analyzeContent(content);
        ContextAwareModerationService.ContextAwareAnalysis contextAnalysis = contextAwareModerationService.analyzeWithContext(content);
        
        if (!localAnalysis.hasDetectedWords()) {
            return new ConsentMaskingResult(content, new ArrayList<>(), new ArrayList<>(), false, false);
        }
        
        List<MaskingOption> maskingOptions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Create masking options for each detected word
        for (LocalModerationService.DetectedWord detectedWord : localAnalysis.getDetectedWords()) {
            String word = detectedWord.getWord();
            int severity = detectedWord.getSeverity();
            
            if (shouldMaskWord(word, severity, contextAnalysis)) {
                MaskingOption option = createMaskingOption(word, severity, contextAnalysis);
                maskingOptions.add(option);
                
                if (severity >= 3) {
                    warnings.add("Strong language detected: " + word);
                }
            }
        }
        
        boolean needsConsent = !maskingOptions.isEmpty();
        boolean hasSevereContent = maskingOptions.stream().anyMatch(option -> option.getSeverity() >= 4);
        
        return new ConsentMaskingResult(content, maskingOptions, warnings, needsConsent, hasSevereContent);
    }
    
    /**
     * Create masking option for a word
     */
    private MaskingOption createMaskingOption(String word, int severity, ContextAwareModerationService.ContextAwareAnalysis contextAnalysis) {
        Map<MaskingStrategy, String> masks = WORD_MASKING_MAP.get(word.toLowerCase());
        if (masks == null) {
            masks = Map.of(MaskingStrategy.FULL_MASKING, "****");
        }
        
        List<String> availableMasks = new ArrayList<>();
        availableMasks.add(masks.get(MaskingStrategy.ASTERISK_MASKING));
        availableMasks.add(masks.get(MaskingStrategy.REPLACEMENT));
        
        // Add context information
        String contextInfo = "";
        Optional<ContextAwareModerationService.ContextualWord> contextualWord = contextAnalysis.getContextualWords().stream()
            .filter(cw -> cw.getWord().equals(word))
            .findFirst();
        
        if (contextualWord.isPresent()) {
            ContextAwareModerationService.ContextualWord cw = contextualWord.get();
            if (!cw.getContextTypes().isEmpty()) {
                contextInfo = "Context: " + String.join(", ", cw.getContextTypes());
            }
        }
        
        return new MaskingOption(word, severity, availableMasks, contextInfo);
    }
    
    /**
     * Apply user's masking preferences
     * 
     * @param content The original content
     * @param maskingChoices User's choices for each word
     * @return MaskingResult with applied masking
     */
    public MaskingResult applyUserMaskingChoices(String content, List<UserMaskingChoice> maskingChoices) {
        if (content == null || content.trim().isEmpty() || maskingChoices.isEmpty()) {
            return new MaskingResult(content, new ArrayList<>(), new ArrayList<>(), false);
        }
        
        String maskedContent = content;
        List<MaskedWord> maskedWords = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        for (UserMaskingChoice choice : maskingChoices) {
            if (choice.isMask()) {
                String maskedWord = choice.getMaskedWord();
                maskedContent = maskWordInContent(maskedContent, choice.getOriginalWord(), maskedWord);
                maskedWords.add(new MaskedWord(choice.getOriginalWord(), maskedWord, choice.getSeverity(), choice.getStrategy()));
                
                if (choice.getSeverity() >= 3) {
                    warnings.add("User chose to mask: " + choice.getOriginalWord());
                }
            }
        }
        
        boolean contentModified = !maskedContent.equals(content);
        
        return new MaskingResult(maskedContent, maskedWords, warnings, contentModified);
    }
    
    /**
     * Get alternative words for replacement
     */
    public List<String> getAlternativeWords(String word) {
        List<String> alternatives = new ArrayList<>();
        
        // Get replacement from map
        String replacement = WORD_REPLACEMENTS.get(word.toLowerCase());
        if (replacement != null) {
            alternatives.add(replacement);
        }
        
        // Add common alternatives based on word type
        switch (word.toLowerCase()) {
            case "fuck", "fucking" -> {
                alternatives.add("freak");
                alternatives.add("freaking");
                alternatives.add("flip");
                alternatives.add("flipping");
            }
            case "shit" -> {
                alternatives.add("stuff");
                alternatives.add("crap");
                alternatives.add("poop");
            }
            case "damn" -> {
                alternatives.add("darn");
                alternatives.add("dang");
            }
            case "bitch" -> {
                alternatives.add("person");
                alternatives.add("individual");
            }
            case "ass" -> {
                alternatives.add("butt");
                alternatives.add("rear");
            }
        }
        
        return alternatives;
    }
    
    /**
     * Result of content masking
     */
    public static class MaskingResult {
        private final String maskedContent;
        private final List<MaskedWord> maskedWords;
        private final List<String> warnings;
        private final boolean contentModified;
        
        public MaskingResult(String maskedContent, List<MaskedWord> maskedWords, List<String> warnings, boolean contentModified) {
            this.maskedContent = maskedContent;
            this.maskedWords = maskedWords;
            this.warnings = warnings;
            this.contentModified = contentModified;
        }
        
        // Getters
        public String getMaskedContent() { return maskedContent; }
        public List<MaskedWord> getMaskedWords() { return maskedWords; }
        public List<String> getWarnings() { return warnings; }
        public boolean isContentModified() { return contentModified; }
    }
    
    /**
     * Result of consent-based masking
     */
    public static class ConsentMaskingResult {
        private final String originalContent;
        private final List<MaskingOption> maskingOptions;
        private final List<String> warnings;
        private final boolean needsConsent;
        private final boolean hasSevereContent;
        
        public ConsentMaskingResult(String originalContent, List<MaskingOption> maskingOptions, 
                                  List<String> warnings, boolean needsConsent, boolean hasSevereContent) {
            this.originalContent = originalContent;
            this.maskingOptions = maskingOptions;
            this.warnings = warnings;
            this.needsConsent = needsConsent;
            this.hasSevereContent = hasSevereContent;
        }
        
        // Getters
        public String getOriginalContent() { return originalContent; }
        public List<MaskingOption> getMaskingOptions() { return maskingOptions; }
        public List<String> getWarnings() { return warnings; }
        public boolean isNeedsConsent() { return needsConsent; }
        public boolean isHasSevereContent() { return hasSevereContent; }
    }
    
    /**
     * Represents a masked word
     */
    public static class MaskedWord {
        private final String originalWord;
        private final String maskedWord;
        private final int severity;
        private final MaskingStrategy strategy;
        
        public MaskedWord(String originalWord, String maskedWord, int severity, MaskingStrategy strategy) {
            this.originalWord = originalWord;
            this.maskedWord = maskedWord;
            this.severity = severity;
            this.strategy = strategy;
        }
        
        // Getters
        public String getOriginalWord() { return originalWord; }
        public String getMaskedWord() { return maskedWord; }
        public int getSeverity() { return severity; }
        public MaskingStrategy getStrategy() { return strategy; }
    }
    
    /**
     * Represents a masking option for user consent
     */
    public static class MaskingOption {
        private final String originalWord;
        private final int severity;
        private final List<String> availableMasks;
        private final String contextInfo;
        
        public MaskingOption(String originalWord, int severity, List<String> availableMasks, String contextInfo) {
            this.originalWord = originalWord;
            this.severity = severity;
            this.availableMasks = availableMasks;
            this.contextInfo = contextInfo;
        }
        
        // Getters
        public String getOriginalWord() { return originalWord; }
        public int getSeverity() { return severity; }
        public List<String> getAvailableMasks() { return availableMasks; }
        public String getContextInfo() { return contextInfo; }
    }
    
    /**
     * Represents user's masking choice
     */
    public static class UserMaskingChoice {
        private final String originalWord;
        private final String maskedWord;
        private final int severity;
        private final MaskingStrategy strategy;
        private final boolean mask;
        
        public UserMaskingChoice(String originalWord, String maskedWord, int severity, MaskingStrategy strategy, boolean mask) {
            this.originalWord = originalWord;
            this.maskedWord = maskedWord;
            this.severity = severity;
            this.strategy = strategy;
            this.mask = mask;
        }
        
        // Getters
        public String getOriginalWord() { return originalWord; }
        public String getMaskedWord() { return maskedWord; }
        public int getSeverity() { return severity; }
        public MaskingStrategy getStrategy() { return strategy; }
        public boolean isMask() { return mask; }
    }
}
