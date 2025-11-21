package ai.youthmind.audit;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditReportGenerator {
    public String generateSummary(Map<String, Object> stats) {
        return "Audit Report: " + String.valueOf(stats);
    }
}


