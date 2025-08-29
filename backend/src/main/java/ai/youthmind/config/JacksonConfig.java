package ai.youthmind.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for proper JSON serialization/deserialization.
 * 
 * This configuration ensures that Java 8 time types like LocalDateTime
 * are properly handled when serializing objects to JSON, especially
 * when caching to Redis or sending responses to clients.
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure ObjectMapper with JSR310 support for Java 8 time types.
     * This solves the serialization issue with LocalDateTime in Redis caching.
     * 
     * @return Configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule to handle Java 8 time types (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (use ISO-8601 format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configure other useful settings
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        return mapper;
    }
}
