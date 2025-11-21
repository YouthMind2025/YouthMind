package ai.youthmind.config;

import ai.youthmind.service.DeploymentConfigurationService.DeploymentStrategy;
import ai.youthmind.service.DeploymentConfigurationService.ServiceType;
import ai.youthmind.service.DeploymentManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Jackson configuration for proper JSON serialization/deserialization.
 * 
 * This configuration ensures that Java 8 time types like LocalDateTime
 * are properly handled when serializing objects to JSON, especially
 * when caching to Redis or sending responses to clients.
 * 
 * Also configures deserialization to fail on invalid data instead of
 * silently accepting null/invalid values, ensuring proper error handling.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JacksonConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(JacksonConfig.class);
    
    private ObjectMapper configuredObjectMapper;

    /**
     * Configure ObjectMapper with JSR310 support for Java 8 time types.
     * This solves the serialization issue with LocalDateTime in Redis caching.
     * 
     * Also configures deserialization to fail fast on invalid enum values,
     * invalid types, and null values for primitives, ensuring proper error handling
     * through GlobalExceptionHandler instead of silent failures.
     * 
     * @return Configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule to handle Java 8 time types (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        
        // Register custom strict enum deserializers
        SimpleModule enumModule = new SimpleModule("StrictEnumModule");
        enumModule.addDeserializer(ServiceType.class, new StrictEnumDeserializer<>(ServiceType.class));
        enumModule.addDeserializer(DeploymentStrategy.class, new StrictEnumDeserializer<>(DeploymentStrategy.class));
        enumModule.addDeserializer(DeploymentManager.DeploymentStrategy.class, new StrictEnumDeserializer<>(DeploymentManager.DeploymentStrategy.class));
        mapper.registerModule(enumModule);
        
        // Disable writing dates as timestamps (use ISO-8601 format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configure other useful settings
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Deserialization features: Fail fast on invalid data
        // Prevents silent failures and ensures HttpMessageNotReadableException is thrown
        // for invalid enum values, invalid types, and null primitives
        mapper.disable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        mapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        // This ensures Jackson throws JsonMappingException for invalid enum values
        // which Spring will wrap in HttpMessageNotReadableException
        
        // Store reference for use in extendMessageConverters
        this.configuredObjectMapper = mapper;
        
        logger.info("Configured ObjectMapper with strict deserialization features: " +
            "READ_UNKNOWN_ENUM_VALUES_AS_NULL=false, FAIL_ON_INVALID_SUBTYPE=true, " +
            "FAIL_ON_NULL_FOR_PRIMITIVES=true, FAIL_ON_NUMBERS_FOR_ENUMS=true");
        
        return mapper;
    }
    
    /**
     * Configure HTTP message converters to use our @Primary ObjectMapper.
     * This ensures that HTTP request/response bodies use our strict deserialization settings.
     * 
     * Replaces ALL MappingJackson2HttpMessageConverter instances with new ones
     * configured with our ObjectMapper to ensure invalid enum values trigger HttpMessageNotReadableException.
     * 
     * @param converters List of message converters (managed by Spring)
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        logger.info("=== EXTEND MESSAGE CONVERTERS CALLED ===");
        logger.info("Initial converter count: {}", converters.size());
        
        // Log all existing converters
        for (int i = 0; i < converters.size(); i++) {
            HttpMessageConverter<?> conv = converters.get(i);
            logger.info("Converter {}: {}", i, conv.getClass().getName());
            if (conv instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jacksonConv = (MappingJackson2HttpMessageConverter) conv;
                ObjectMapper om = jacksonConv.getObjectMapper();
                logger.info("  - ObjectMapper: {}", om != null ? om.getClass().getName() : "null");
                if (om != null) {
                    logger.info("  - READ_UNKNOWN_ENUM_VALUES_AS_NULL: {}", 
                        om.getDeserializationConfig().isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL));
                }
            }
        }
        
        // CRITICAL FIX: Don't remove existing converters to preserve Springdoc's OpenAPI serialization
        // Root cause: Removing ALL MappingJackson2HttpMessageConverter instances breaks Springdoc
        // Springdoc relies on its converter for proper OpenAPI JSON serialization
        // When removed, Springdoc falls back to incorrect serialization (base64-encoded string)
        // 
        // Solution: Add our converter at the beginning for priority, but preserve existing ones
        // This ensures our strict deserialization is used for API requests while Springdoc
        // can still use its converter for /api-docs endpoint
        
        // CRITICAL FIX: Preserve Springdoc's converter while replacing others
        // Root cause: Our converter at index 0 handles ALL requests including /api-docs
        // Springdoc needs its own converter for proper OpenAPI JSON serialization
        // 
        // Solution: Find and preserve Springdoc's converter, replace others with our configured one
        // This ensures Springdoc can use its converter for /api-docs while our strict
        // deserialization is used for API requests
        
        // Verify our configured ObjectMapper has strict settings
        if (configuredObjectMapper == null) {
            logger.error("WARNING: configuredObjectMapper is NULL! ObjectMapper bean may not have been created yet.");
            return;
        }
        
        // Create a custom converter that excludes Springdoc endpoints
        // This ensures Springdoc can use its own converter for /api-docs
        MappingJackson2HttpMessageConverter jacksonConverter = 
            new MappingJackson2HttpMessageConverter(configuredObjectMapper) {
                @Override
                public boolean canWrite(Class<?> clazz, MediaType mediaType) {
                    // Exclude Springdoc endpoints - let Springdoc's converter handle them
                    // This is checked via the current request context
                    try {
                        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                        if (requestAttributes != null) {
                            String path = (String) requestAttributes.getAttribute(
                                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
                                RequestAttributes.SCOPE_REQUEST);
                            if (path != null && (path.startsWith("/api-docs") || 
                                                 path.startsWith("/v3/api-docs") || 
                                                 path.startsWith("/swagger-ui"))) {
                                logger.debug("Skipping Springdoc endpoint: " + path);
                                return false; // Don't handle Springdoc endpoints
                            }
                        }
                    } catch (Exception e) {
                        // If we can't check, allow it (fallback behavior)
                        logger.debug("Could not check request path, allowing converter: " + e.getMessage());
                    }
                    return super.canWrite(clazz, mediaType);
                }
            };
        
        // Remove existing non-Springdoc Jackson converters and add ours at the beginning
        final int[] removedCount = {0};
        converters.removeIf(converter -> {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                String className = converter.getClass().getName();
                // Preserve Springdoc converters if they exist
                boolean isSpringdoc = className.contains("springdoc") || className.contains("org.springdoc");
                if (!isSpringdoc) {
                    removedCount[0]++;
                    logger.info("Removed converter: {}", className);
                    return true;
                } else {
                    logger.info("Preserved Springdoc converter: {}", className);
                    return false;
                }
            }
            return false;
        });
        
        // Add our converter at the beginning (highest priority)
        converters.add(0, jacksonConverter);
        logger.info("Added custom MappingJackson2HttpMessageConverter with Springdoc exclusion at index 0");
        logger.info("Removed {} non-Springdoc Jackson converters", removedCount[0]);
        
        // Verify the converter we're using has strict settings
        logger.info("Configured ObjectMapper - READ_UNKNOWN_ENUM_VALUES_AS_NULL: {}", 
            configuredObjectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL));
        
        logger.info("Final converter count: {}", converters.size());
        logger.info("=== EXTEND MESSAGE CONVERTERS COMPLETE ===");
    }
}
