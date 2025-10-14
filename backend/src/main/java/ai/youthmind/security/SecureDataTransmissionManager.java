package ai.youthmind.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class SecureDataTransmissionManager {
    public void applySecurityHeaders(HttpHeaders headers) {
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("Referrer-Policy", "no-referrer");
    }
}


