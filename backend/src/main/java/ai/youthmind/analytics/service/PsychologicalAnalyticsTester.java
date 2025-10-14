package ai.youthmind.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PsychologicalAnalyticsTester {

    private static final Logger logger = LoggerFactory.getLogger(PsychologicalAnalyticsTester.class);

    public void sanity(List<Double> xs) {
        if (xs == null) {
            logger.warn("Analytics sanity: null series");
        }
    }
}


