package ai.youthmind.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("YouthMind Backend API")
                        .description("""
                            Backend API for YouthMind - A mental health support platform for youth.
                            
                            ## Features
                            - User authentication and registration
                            - Post creation and management
                            - Feed generation and caching
                            - AI-powered content analysis
                            - Real-time notifications via Redis Pub/Sub
                            
                            ## Authentication
                            This API uses JWT (JSON Web Tokens) for authentication. 
                            Include the token in the Authorization header: `Bearer <your-token>`
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("YouthMind Team")
                                .email("team@youthmind.ai")
                                .url("https://youthmind.ai"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development server"),
                        new Server().url("https://api.youthmind.ai").description("Production server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
} 