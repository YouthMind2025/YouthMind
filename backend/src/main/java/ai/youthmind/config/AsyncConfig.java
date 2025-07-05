package ai.youthmind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot's default async configuration is sufficient for our needs
    // The default task executor will be used for @Async methods
}