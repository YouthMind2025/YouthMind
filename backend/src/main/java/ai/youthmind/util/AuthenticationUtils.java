package ai.youthmind.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class for common authentication operations across controllers.
 * 
 * This class provides centralized authentication logic to reduce code duplication
 * and ensure consistent authentication handling throughout the application.
 */
@Component
public class AuthenticationUtils {
    
    /**
     * Get the current user ID from the security context
     * 
     * @return Current user ID
     * @throws IllegalStateException if no user is authenticated
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UUID)) {
            throw new IllegalStateException("Principal is not a UUID. Found: " + principal.getClass().getName());
        }
        
        return (UUID) principal;
    }
    
    /**
     * Get the current user ID from the security context, with null fallback
     * 
     * @return Current user ID, or null if not authenticated
     */
    public static UUID getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (IllegalStateException e) {
            return null;
        }
    }
    
    /**
     * Check if the current user is authenticated
     * 
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UUID;
    }
    
    /**
     * Validate that the current user owns a resource
     * 
     * @param resourceUserId The user ID of the resource owner
     * @return true if current user owns the resource, false otherwise
     * @throws IllegalStateException if no user is authenticated
     */
    public static boolean isResourceOwner(UUID resourceUserId) {
        UUID currentUserId = getCurrentUserId();
        return currentUserId.equals(resourceUserId);
    }
    
    /**
     * Validate that the current user owns a resource, with null fallback
     * 
     * @param resourceUserId The user ID of the resource owner
     * @return true if current user owns the resource, false if not authenticated or not owner
     */
    public static boolean isResourceOwnerOrNull(UUID resourceUserId) {
        try {
            return isResourceOwner(resourceUserId);
        } catch (IllegalStateException e) {
            return false;
        }
    }
} 