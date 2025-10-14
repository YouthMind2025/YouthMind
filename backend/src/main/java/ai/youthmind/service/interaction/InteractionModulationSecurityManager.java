package ai.youthmind.service.interaction;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Simple access guard for interaction modulation endpoints/services.
 */
@Service
public class InteractionModulationSecurityManager {

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void assertUser() {
        // Method security enforced by Spring Security
    }
}


