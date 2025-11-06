package ai.youthmind.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

/**
 * Test JPA configuration for PostgreSQL Testcontainers.
 * This configuration is now compatible with TestContainersConfig.
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(basePackages = {
    "ai.youthmind.model", 
    "ai.youthmind.entity", 
    "ai.youthmind.analytics.entity", 
    "ai.youthmind.interests.entity", 
    "ai.youthmind.memory.entity",
    "ai.youthmind.recommendation.entity",
    "ai.youthmind.feed.entity",
    "ai.youthmind.moderation.entity",
    "ai.youthmind.vector.entity",
    "ai.youthmind"  // Root package to catch any other entities
})
@AutoConfigureDataJpa
@Profile("test")
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
    "spring.flyway.enabled=false",
    "spring.data.redis.enabled=false",
    "spring.data.redis.repositories.enabled=false"
})
public class TestJpaConfig {
    // No datasource bean needed - TestContainersConfig provides PostgreSQL datasource
}
