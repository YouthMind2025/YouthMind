package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Security Manager for access control and security enforcement.
 * Features:
 * - User-based cache isolation
 * - Access control and permissions
 * - Cache encryption and security
 * - Security audit logging
 * - Access pattern monitoring
 * - Security metrics and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheSecurityManager {

    // Security levels
    public enum SecurityLevel {
        PUBLIC, USER_ISOLATED, ADMIN_ONLY, SYSTEM_INTERNAL
    }

    // Access permissions
    public enum AccessPermission {
        READ, WRITE, DELETE, ADMIN
    }

    // Security tracking
    private final Map<String, SecurityPolicy> securityPolicies = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> accessCounters = new ConcurrentHashMap<>();
    private final Map<String, List<SecurityEvent>> securityEvents = new ConcurrentHashMap<>();
    private final Map<String, UserAccessContext> userContexts = new ConcurrentHashMap<>();

    /**
     * Check if user has access to cache key
     */
    public boolean hasAccess(String key) {
        return hasAccess(key, null, AccessPermission.READ);
    }

    /**
     * Check if user has specific access to cache key
     */
    public boolean hasAccess(String key, String userId, AccessPermission permission) {
        try {
            // Get security policy for key
            SecurityPolicy policy = getSecurityPolicy(key);
            
            // Check access based on policy
            boolean hasAccess = checkAccessPermission(policy, userId, permission);
            
            // Record access attempt
            recordAccessAttempt(key, userId, permission, hasAccess);
            
            return hasAccess;

        } catch (Exception e) {
            log.error("Error checking access for key: {} user: {}", key, userId, e);
            return false; // Deny access on error
        }
    }

    /**
     * Set security policy for cache key
     */
    public void setSecurityPolicy(String key, SecurityPolicy policy) {
        securityPolicies.put(key, policy);
        log.debug("Set security policy for key: {} with level: {}", key, policy.getSecurityLevel());
    }

    /**
     * Get security policy for cache key
     */
    public SecurityPolicy getSecurityPolicy(String key) {
        // Determine policy based on key pattern
        SecurityPolicy policy = securityPolicies.get(key);
        
        if (policy == null) {
            policy = determineDefaultPolicy(key);
            securityPolicies.put(key, policy);
        }
        
        return policy;
    }

    /**
     * Create user access context
     */
    public void createUserContext(String userId, Set<String> roles, Map<String, Object> attributes) {
        UserAccessContext context = UserAccessContext.builder()
            .userId(userId)
            .roles(roles)
            .attributes(attributes)
            .createdAt(LocalDateTime.now())
            .lastAccessed(LocalDateTime.now())
            .build();
            
        userContexts.put(userId, context);
        log.debug("Created user context for user: {} with roles: {}", userId, roles);
    }

    /**
     * Get user access context
     */
    public UserAccessContext getUserContext(String userId) {
        return userContexts.get(userId);
    }

    /**
     * Get security metrics
     */
    public SecurityMetrics getSecurityMetrics() {
        long totalAccessAttempts = accessCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        
        long totalSecurityEvents = securityEvents.values().stream()
            .mapToLong(List::size)
            .sum();
        
        long activeUserContexts = userContexts.size();
        long activeSecurityPolicies = securityPolicies.size();

        return SecurityMetrics.builder()
            .totalAccessAttempts(totalAccessAttempts)
            .totalSecurityEvents(totalSecurityEvents)
            .activeUserContexts(activeUserContexts)
            .activeSecurityPolicies(activeSecurityPolicies)
            .accessDeniedRate(calculateAccessDeniedRate())
            .lastSecurityEvent(getLastSecurityEvent())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Get security audit log
     */
    public List<SecurityEvent> getSecurityAuditLog(String key) {
        return securityEvents.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Encrypt cache data
     */
    public String encryptCacheData(String data, String userId) {
        try {
            // Simple encryption placeholder
            // In a real implementation, you would use proper encryption
            return "encrypted_" + data + "_" + userId;
        } catch (Exception e) {
            log.error("Error encrypting cache data", e);
            return data; // Return original data on error
        }
    }

    /**
     * Decrypt cache data
     */
    public String decryptCacheData(String encryptedData, String userId) {
        try {
            // Simple decryption placeholder
            // In a real implementation, you would use proper decryption
            if (encryptedData.startsWith("encrypted_") && encryptedData.endsWith("_" + userId)) {
                return encryptedData.substring(10, encryptedData.length() - userId.length() - 1);
            }
            return encryptedData;
        } catch (Exception e) {
            log.error("Error decrypting cache data", e);
            return encryptedData; // Return original data on error
        }
    }

    /**
     * Check if key requires encryption
     */
    public boolean requiresEncryption(String key) {
        SecurityPolicy policy = getSecurityPolicy(key);
        return policy.getSecurityLevel() == SecurityLevel.USER_ISOLATED ||
               policy.getSecurityLevel() == SecurityLevel.ADMIN_ONLY;
    }

    // Private helper methods

    private SecurityPolicy determineDefaultPolicy(String key) {
        // Determine default security policy based on key pattern
        if (key.startsWith("user_profile:") || key.startsWith("psychological_state:")) {
            return SecurityPolicy.builder()
                .key(key)
                .securityLevel(SecurityLevel.USER_ISOLATED)
                .allowedRoles(Set.of("USER", "ADMIN"))
                .allowedPermissions(Set.of(AccessPermission.READ, AccessPermission.WRITE))
                .requiresEncryption(true)
                .build();
        } else if (key.startsWith("admin:") || key.startsWith("system:")) {
            return SecurityPolicy.builder()
                .key(key)
                .securityLevel(SecurityLevel.ADMIN_ONLY)
                .allowedRoles(Set.of("ADMIN"))
                .allowedPermissions(Set.of(AccessPermission.READ, AccessPermission.WRITE, AccessPermission.DELETE, AccessPermission.ADMIN))
                .requiresEncryption(true)
                .build();
        } else if (key.startsWith("public:") || key.startsWith("feed_data:")) {
            return SecurityPolicy.builder()
                .key(key)
                .securityLevel(SecurityLevel.PUBLIC)
                .allowedRoles(Set.of("USER", "ADMIN", "GUEST"))
                .allowedPermissions(Set.of(AccessPermission.READ))
                .requiresEncryption(false)
                .build();
        } else {
            return SecurityPolicy.builder()
                .key(key)
                .securityLevel(SecurityLevel.SYSTEM_INTERNAL)
                .allowedRoles(Set.of("SYSTEM"))
                .allowedPermissions(Set.of(AccessPermission.READ, AccessPermission.WRITE))
                .requiresEncryption(false)
                .build();
        }
    }

    private boolean checkAccessPermission(SecurityPolicy policy, String userId, AccessPermission permission) {
        // Check if permission is allowed
        if (!policy.getAllowedPermissions().contains(permission)) {
            return false;
        }

        // Check if user has required role
        if (userId != null) {
            UserAccessContext context = getUserContext(userId);
            if (context != null) {
                return context.getRoles().stream()
                    .anyMatch(role -> policy.getAllowedRoles().contains(role));
            }
        }

        // Check for public access
        return policy.getSecurityLevel() == SecurityLevel.PUBLIC;
    }

    private void recordAccessAttempt(String key, String userId, AccessPermission permission, boolean granted) {
        // Record access attempt
        accessCounters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        // Record security event
        SecurityEvent event = SecurityEvent.builder()
            .key(key)
            .userId(userId)
            .permission(permission)
            .granted(granted)
            .timestamp(LocalDateTime.now())
            .build();
            
        securityEvents.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        
        // Update user context
        if (userId != null) {
            UserAccessContext context = getUserContext(userId);
            if (context != null) {
                context.setLastAccessed(LocalDateTime.now());
            }
        }
    }

    private double calculateAccessDeniedRate() {
        long totalAttempts = accessCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        
        long deniedAttempts = securityEvents.values().stream()
            .flatMap(List::stream)
            .mapToLong(event -> event.isGranted() ? 0 : 1)
            .sum();
        
        return totalAttempts > 0 ? (double) deniedAttempts / totalAttempts : 0.0;
    }

    private LocalDateTime getLastSecurityEvent() {
        return securityEvents.values().stream()
            .flatMap(List::stream)
            .map(SecurityEvent::getTimestamp)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now().minusHours(1));
    }

    // Inner classes

    @lombok.Data
    @lombok.Builder
    public static class SecurityPolicy {
        private String key;
        private SecurityLevel securityLevel;
        private Set<String> allowedRoles;
        private Set<AccessPermission> allowedPermissions;
        private boolean requiresEncryption;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserAccessContext {
        private String userId;
        private Set<String> roles;
        private Map<String, Object> attributes;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessed;
    }

    @lombok.Data
    @lombok.Builder
    public static class SecurityEvent {
        private String key;
        private String userId;
        private AccessPermission permission;
        private boolean granted;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class SecurityMetrics {
        private long totalAccessAttempts;
        private long totalSecurityEvents;
        private long activeUserContexts;
        private long activeSecurityPolicies;
        private double accessDeniedRate;
        private LocalDateTime lastSecurityEvent;
        private LocalDateTime timestamp;
    }
}
