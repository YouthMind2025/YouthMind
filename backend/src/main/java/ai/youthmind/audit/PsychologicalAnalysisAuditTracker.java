package ai.youthmind.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PsychologicalAnalysisAuditTracker {

    private static final Logger logger = LoggerFactory.getLogger(PsychologicalAnalysisAuditTracker.class);

    public void trackOperation(UUID userId, String operation, String status) {
        logger.info("AUDIT_PSY_ANALYTICS|{}|{}|{}|{}", LocalDateTime.now(), userId, operation, status);
    }
}


