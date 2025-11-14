package ai.youthmind.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Custom enum deserializer that always throws InvalidFormatException for invalid enum values.
 * This ensures that invalid enum values are rejected with HTTP 400 instead of being silently deserialized as null.
 */
public class StrictEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(StrictEnumDeserializer.class);
    private final Class<T> enumClass;
    
    public StrictEnumDeserializer(Class<T> enumClass) {
        this.enumClass = enumClass;
    }
    
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        logger.debug("Deserializing enum {} from value: {}", enumClass.getSimpleName(), value);
        
        if (value == null || value.trim().isEmpty()) {
            logger.error("Null or empty enum value provided for {}", enumClass.getSimpleName());
            throw InvalidFormatException.from(p, 
                "Enum value cannot be null or empty for " + enumClass.getSimpleName(), 
                value, enumClass);
        }
        
        try {
            // Try to parse as enum (case-insensitive)
            // First try exact match
            try {
                T enumValue = Enum.valueOf(enumClass, value);
                logger.debug("Successfully deserialized enum {} as {} (exact match)", enumClass.getSimpleName(), enumValue);
                return enumValue;
            } catch (IllegalArgumentException e1) {
                // If exact match fails, try case-insensitive match
                for (T enumConstant : enumClass.getEnumConstants()) {
                    if (enumConstant.name().equalsIgnoreCase(value)) {
                        logger.debug("Successfully deserialized enum {} as {} (case-insensitive match)", enumClass.getSimpleName(), enumConstant);
                        return enumConstant;
                    }
                }
                // If no match found, throw exception
                throw e1;
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid enum value '{}' for {}. Valid values: {}", 
                value, enumClass.getSimpleName(), java.util.Arrays.toString(enumClass.getEnumConstants()));
            
            // This will be caught by InvalidFormatException handler
            throw InvalidFormatException.from(p, 
                String.format("Invalid enum value '%s' for %s. Valid values: %s", 
                    value, enumClass.getSimpleName(), 
                    java.util.Arrays.toString(enumClass.getEnumConstants())), 
                value, enumClass);
        }
    }
}

