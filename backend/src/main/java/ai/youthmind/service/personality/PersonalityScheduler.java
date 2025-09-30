package ai.youthmind.service.personality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Scheduler for periodic personality computation cycles.
 */
@Service
public class PersonalityScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PersonalityScheduler.class);

    // Run daily at 03:00 server time
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailyComputation() {
        logger.info("[PersonalityScheduler] Daily personality computation triggered");
        // Hook: iterate users and invoke PersonalityComputationService (injected when implementing)
    }

    // Run weekly on Sunday at 04:00 server time
    @Scheduled(cron = "0 0 4 * * SUN")
    public void runWeeklyReconciler() {
        logger.info("[PersonalityScheduler] Weekly reconciliation triggered");
    }
}


