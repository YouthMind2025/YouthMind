package ai.youthmind.service.personality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class PersonalityComputationBackupManager {

    private static final Logger logger = LoggerFactory.getLogger(PersonalityComputationBackupManager.class);

    public void backup(UUID userId, Map<String, Object> payload) {
        logger.debug("Backing up personality computation for user {} (lightweight placeholder)", userId);
    }
}


