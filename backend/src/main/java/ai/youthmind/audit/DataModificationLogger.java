package ai.youthmind.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DataModificationLogger {
    private static final Logger logger = LoggerFactory.getLogger(DataModificationLogger.class);
    public void logChange(String entity, String change, String actor) {
        logger.info("AUDIT_CHANGE|{}|{}|{}|{}", LocalDateTime.now(), entity, change, actor);
    }
}


