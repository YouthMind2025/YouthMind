package ai.youthmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "server.ssl")
@Data
public class SslConfig {
    String keyStore;
    String keyStorePassword;
    int warnBeforeDays;
}
