package ai.youthmind.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DataAccessAuditor {

    private static final Logger logger = LoggerFactory.getLogger(DataAccessAuditor.class);

    public void logAccess(UUID userId, String resource, String action) {
        logger.info("AUDIT_ACCESS|{}|{}|{}|{}", LocalDateTime.now(), userId, resource, action);
    }
}


