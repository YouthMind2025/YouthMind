package ai.youthmind.analytics.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PsychologicalAnalyticsSecurityManager {

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void assertAccess() {}
}


