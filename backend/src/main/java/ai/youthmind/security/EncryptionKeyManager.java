package ai.youthmind.security;

import org.springframework.stereotype.Service;

@Service
public class EncryptionKeyManager {
    public String getKeyRotationPolicy() { return "Rotate every 90 days"; }
}


