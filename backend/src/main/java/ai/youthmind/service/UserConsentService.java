package ai.youthmind.service;

import ai.youthmind.entity.UserConsent;
import ai.youthmind.entity.UserConsent.ConsentType;
import ai.youthmind.entity.UserConsent.ConsentStatus;
import ai.youthmind.repository.UserConsentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user consent for content with masked words.
 * Handles consent requests, tracking, and enforcement.
 */
@Service
@Transactional
public class UserConsentService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserConsentService.class);
    
    @Autowired
    private UserConsentRepository userConsentRepository;
    
    @Autowired
    private WordMaskingService wordMaskingService;
    
    /**
     * Request user consent for content with masked words
     * 
     * @param userId The user ID
     * @param contentId The content ID (post, comment, etc.)
     * @param contentType The type of content
     * @param originalContent The original content with profanity
     * @param maskedContent The masked content
     * @param maskingOptions Available masking options
     * @return UserConsent entity
     */
    public UserConsent requestConsentForMaskedContent(UUID userId, UUID contentId, String contentType,
                                                    String originalContent, String maskedContent,
                                                    List<WordMaskingService.MaskingOption> maskingOptions) {
        logger.info("Requesting consent for masked content from user {} for content {}", userId, contentId);
        
        try {
            // Create consent request
            UserConsent consent = new UserConsent();
            consent.setUserId(userId);
            consent.setContentId(contentId);
            consent.setContentType(contentType);
            consent.setConsentType(ConsentType.MASKED_CONTENT);
            consent.setStatus(ConsentStatus.PENDING);
            consent.setOriginalContent(originalContent);
            consent.setMaskedContent(maskedContent);
            consent.setMaskingOptions(convertMaskingOptionsToString(maskingOptions));
            consent.setRequestedAt(LocalDateTime.now());
            consent.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24-hour expiration
            
            // Save consent request
            UserConsent savedConsent = userConsentRepository.save(consent);
            
            logger.info("Consent request created for user {} with ID {}", userId, savedConsent.getId());
            
            return savedConsent;
            
        } catch (Exception e) {
            logger.error("Error creating consent request for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create consent request", e);
        }
    }
    
    /**
     * Process user's consent decision
     * 
     * @param consentId The consent request ID
     * @param userId The user ID
     * @param decision The user's decision (ACCEPT, REJECT, MODIFY)
     * @param modifiedContent Optional modified content if user chose MODIFY
     * @param selectedMaskingStrategy Optional masking strategy if user chose MODIFY
     * @return Updated UserConsent entity
     */
    public UserConsent processConsentDecision(UUID consentId, UUID userId, ConsentStatus decision,
                                             String modifiedContent, WordMaskingService.MaskingStrategy selectedMaskingStrategy) {
        logger.info("Processing consent decision {} for user {} on consent {}", decision, userId, consentId);
        
        try {
            // Find consent request
            Optional<UserConsent> consentOpt = userConsentRepository.findById(consentId);
            if (consentOpt.isEmpty()) {
                throw new IllegalArgumentException("Consent request not found: " + consentId);
            }
            
            UserConsent consent = consentOpt.get();
            
            // Verify user owns this consent request
            if (!consent.getUserId().equals(userId)) {
                throw new IllegalArgumentException("User does not own this consent request");
            }
            
            // Check if consent has expired
            if (consent.getExpiresAt().isBefore(LocalDateTime.now())) {
                consent.setStatus(ConsentStatus.EXPIRED);
                userConsentRepository.save(consent);
                throw new IllegalArgumentException("Consent request has expired");
            }
            
            // Process decision
            consent.setStatus(decision);
            consent.setDecidedAt(LocalDateTime.now());
            
            switch (decision) {
                case ACCEPTED -> {
                    consent.setFinalContent(consent.getMaskedContent());
                    consent.setMaskingStrategy(WordMaskingService.MaskingStrategy.ASTERISK_MASKING.toString());
                    logger.info("User {} accepted masked content for consent {}", userId, consentId);
                }
                case REJECTED -> {
                    consent.setFinalContent(null);
                    logger.info("User {} rejected masked content for consent {}", userId, consentId);
                }
                case MODIFIED -> {
                    if (modifiedContent == null || modifiedContent.trim().isEmpty()) {
                        throw new IllegalArgumentException("Modified content is required for MODIFY decision");
                    }
                    consent.setFinalContent(modifiedContent);
                    consent.setMaskingStrategy(selectedMaskingStrategy.toString());
                    logger.info("User {} modified content for consent {}", userId, consentId);
                }
                default -> throw new IllegalArgumentException("Invalid consent decision: " + decision);
            }
            
            // Save updated consent
            UserConsent updatedConsent = userConsentRepository.save(consent);
            
            return updatedConsent;
            
        } catch (Exception e) {
            logger.error("Error processing consent decision for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to process consent decision", e);
        }
    }
    
    /**
     * Get pending consent requests for a user
     * 
     * @param userId The user ID
     * @return List of pending consent requests
     */
    public List<UserConsent> getPendingConsentRequests(UUID userId) {
        logger.debug("Getting pending consent requests for user {}", userId);
        
        try {
            return userConsentRepository.findByUserIdAndStatusAndExpiresAtAfter(
                userId, ConsentStatus.PENDING, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Error getting pending consent requests for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Get consent history for a user
     * 
     * @param userId The user ID
     * @param limit Maximum number of records to return
     * @return List of consent history
     */
    public List<UserConsent> getConsentHistory(UUID userId, int limit) {
        logger.debug("Getting consent history for user {} with limit {}", userId, limit);
        
        try {
            return userConsentRepository.findByUserIdOrderByRequestedAtDesc(userId)
                .stream()
                .limit(limit)
                .toList();
        } catch (Exception e) {
            logger.error("Error getting consent history for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Check if user has given consent for specific content
     * 
     * @param userId The user ID
     * @param contentId The content ID
     * @return Optional UserConsent if found
     */
    public Optional<UserConsent> getConsentForContent(UUID userId, UUID contentId) {
        logger.debug("Checking consent for user {} and content {}", userId, contentId);
        
        try {
            return userConsentRepository.findByUserIdAndContentId(userId, contentId);
        } catch (Exception e) {
            logger.error("Error checking consent for user {} and content {}: {}", userId, contentId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if user can proceed with content (has valid consent)
     * 
     * @param userId The user ID
     * @param contentId The content ID
     * @return true if user can proceed
     */
    public boolean canUserProceedWithContent(UUID userId, UUID contentId) {
        Optional<UserConsent> consentOpt = getConsentForContent(userId, contentId);
        
        if (consentOpt.isEmpty()) {
            return false; // No consent request found
        }
        
        UserConsent consent = consentOpt.get();
        
        // Check if consent is accepted and not expired
        return consent.getStatus() == ConsentStatus.ACCEPTED && 
               consent.getExpiresAt().isAfter(LocalDateTime.now());
    }
    
    /**
     * Get the final content based on user's consent decision
     * 
     * @param userId The user ID
     * @param contentId The content ID
     * @return Optional final content
     */
    public Optional<String> getFinalContent(UUID userId, UUID contentId) {
        Optional<UserConsent> consentOpt = getConsentForContent(userId, contentId);
        
        if (consentOpt.isEmpty()) {
            return Optional.empty();
        }
        
        UserConsent consent = consentOpt.get();
        
        if (consent.getStatus() == ConsentStatus.ACCEPTED || consent.getStatus() == ConsentStatus.MODIFIED) {
            return Optional.ofNullable(consent.getFinalContent());
        }
        
        return Optional.empty();
    }
    
    /**
     * Clean up expired consent requests
     * 
     * @return Number of expired requests cleaned up
     */
    public int cleanupExpiredConsentRequests() {
        logger.info("Cleaning up expired consent requests");
        
        try {
            List<UserConsent> expiredConsents = userConsentRepository.findByExpiresAtBeforeAndStatus(
                LocalDateTime.now(), ConsentStatus.PENDING);
            
            for (UserConsent consent : expiredConsents) {
                consent.setStatus(ConsentStatus.EXPIRED);
            }
            
            userConsentRepository.saveAll(expiredConsents);
            
            logger.info("Cleaned up {} expired consent requests", expiredConsents.size());
            return expiredConsents.size();
            
        } catch (Exception e) {
            logger.error("Error cleaning up expired consent requests: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Get consent statistics for a user
     * 
     * @param userId The user ID
     * @return ConsentStatistics
     */
    public ConsentStatistics getConsentStatistics(UUID userId) {
        logger.debug("Getting consent statistics for user {}", userId);
        
        try {
            List<UserConsent> allConsents = userConsentRepository.findByUserId(userId);
            
            long totalRequests = allConsents.size();
            long acceptedRequests = allConsents.stream()
                .filter(c -> c.getStatus() == ConsentStatus.ACCEPTED)
                .count();
            long rejectedRequests = allConsents.stream()
                .filter(c -> c.getStatus() == ConsentStatus.REJECTED)
                .count();
            long modifiedRequests = allConsents.stream()
                .filter(c -> c.getStatus() == ConsentStatus.MODIFIED)
                .count();
            long expiredRequests = allConsents.stream()
                .filter(c -> c.getStatus() == ConsentStatus.EXPIRED)
                .count();
            long pendingRequests = allConsents.stream()
                .filter(c -> c.getStatus() == ConsentStatus.PENDING)
                .count();
            
            return new ConsentStatistics(
                totalRequests, acceptedRequests, rejectedRequests, 
                modifiedRequests, expiredRequests, pendingRequests
            );
            
        } catch (Exception e) {
            logger.error("Error getting consent statistics for user {}: {}", userId, e.getMessage(), e);
            return new ConsentStatistics(0, 0, 0, 0, 0, 0);
        }
    }
    
    /**
     * Consent statistics for a user
     */
    public static class ConsentStatistics {
        private final long totalRequests;
        private final long acceptedRequests;
        private final long rejectedRequests;
        private final long modifiedRequests;
        private final long expiredRequests;
        private final long pendingRequests;
        
        public ConsentStatistics(long totalRequests, long acceptedRequests, long rejectedRequests,
                               long modifiedRequests, long expiredRequests, long pendingRequests) {
            this.totalRequests = totalRequests;
            this.acceptedRequests = acceptedRequests;
            this.rejectedRequests = rejectedRequests;
            this.modifiedRequests = modifiedRequests;
            this.expiredRequests = expiredRequests;
            this.pendingRequests = pendingRequests;
        }
        
        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getAcceptedRequests() { return acceptedRequests; }
        public long getRejectedRequests() { return rejectedRequests; }
        public long getModifiedRequests() { return modifiedRequests; }
        public long getExpiredRequests() { return expiredRequests; }
        public long getPendingRequests() { return pendingRequests; }
        
        public double getAcceptanceRate() {
            return totalRequests > 0 ? (double) acceptedRequests / totalRequests : 0.0;
        }
        
        public double getModificationRate() {
            return totalRequests > 0 ? (double) modifiedRequests / totalRequests : 0.0;
        }
    }
    
    /**
     * Convert masking options to JSON string for storage
     */
    private String convertMaskingOptionsToString(List<WordMaskingService.MaskingOption> maskingOptions) {
        if (maskingOptions == null || maskingOptions.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < maskingOptions.size(); i++) {
            WordMaskingService.MaskingOption option = maskingOptions.get(i);
            json.append("{")
                .append("\"originalWord\":\"").append(option.getOriginalWord()).append("\",")
                .append("\"severity\":").append(option.getSeverity()).append(",")
                .append("\"availableMasks\":").append(option.getAvailableMasks().toString()).append(",")
                .append("\"contextInfo\":\"").append(option.getContextInfo()).append("\"")
                .append("}");
            
            if (i < maskingOptions.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        
        return json.toString();
    }
}
