package ai.youthmind.service.personality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonalityComputationTester {

    private static final Logger logger = LoggerFactory.getLogger(PersonalityComputationTester.class);

    public void sanityCheck(List<Double> vector) {
        if (vector == null || vector.size() != 43) {
            logger.warn("Personality vector shape invalid: {}", vector == null ? -1 : vector.size());
        }
    }
}


