package ai.youthmind.access;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PsychologicalDataAccessController {

    @PreAuthorize("hasAnyRole('USER','ADMIN') and #userId.toString() == authentication.principal")
    public void assertUserAccess(UUID userId) {}
}


