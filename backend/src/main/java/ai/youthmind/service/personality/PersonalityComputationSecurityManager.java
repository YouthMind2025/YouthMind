package ai.youthmind.service.personality;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PersonalityComputationSecurityManager {

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void assertComputationAccess() {}
}


