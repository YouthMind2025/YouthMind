package ai.youthmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ai.youthmind")
@EnableTransactionManagement
@EnableScheduling
public class YouthmindBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YouthmindBackendApplication.class, args);
    }

} 