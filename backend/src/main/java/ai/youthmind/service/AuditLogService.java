package ai.youthmind.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logSecurityEvent(String eventType, String userId, String description, boolean success) {
        Map<String, Object> auditLog = new LinkedHashMap<>();
        auditLog.put("timestamp", Instant.now().toString());
        auditLog.put("eventType", eventType);
        auditLog.put("userId", userId);
        auditLog.put("description", description);
        auditLog.put("success", success);

        try {
            String logMessage = new ObjectMapper().writeValueAsString(auditLog);
            logger.info(logMessage);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit log: {}", e.getMessage());
        }
    }

}
