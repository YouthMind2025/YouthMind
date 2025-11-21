package ai.youthmind.security;

import org.springframework.stereotype.Service;

@Service
public class DataPrivacyComplianceManager {
    public boolean isProcessingAllowed(String purpose) {
        return purpose != null && !purpose.isBlank();
    }
}


