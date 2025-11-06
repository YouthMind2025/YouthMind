package ai.youthmind.config;

import ai.youthmind.filter.JWTFilter;
import ai.youthmind.filter.RateFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JWTFilter jwtFilter;

    @Autowired
    @Qualifier("corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private RateFilter rateFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                .contentTypeOptions(contentTypeOptions -> contentTypeOptions.and())
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true))
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .addHeaderWriter((request, response) -> {
                    // Content Security Policy - Allow frames for H2 console
                    String requestUri = request.getRequestURI();
                    if (requestUri != null && requestUri.startsWith("/h2-console")) {
                        response.setHeader("Content-Security-Policy", 
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self' data:; " +
                            "connect-src 'self' https://api.deepseek.com; " +
                            "frame-ancestors 'self'; " +
                            "base-uri 'self'; " +
                            "form-action 'self'");
                    } else {
                        response.setHeader("Content-Security-Policy", 
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self' data:; " +
                            "connect-src 'self' https://api.deepseek.com; " +
                            "frame-ancestors 'none'; " +
                            "base-uri 'self'; " +
                            "form-action 'self'");
                    }
                    
                    // Additional security headers
                    response.setHeader("X-Content-Type-Options", "nosniff");
                    response.setHeader("X-XSS-Protection", "1; mode=block");
                    response.setHeader("Permissions-Policy", 
                        "camera=(), microphone=(), geolocation=(), payment=()");
                }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (must be first)
                .requestMatchers("/").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/v1/auth/register").permitAll()
                .requestMatchers("/v1/auth/login").permitAll()
                .requestMatchers("/v1/auth/send-otp").permitAll()
                .requestMatchers("/v1/auth/forgot-password").permitAll()
                .requestMatchers("/v1/auth/refresh").permitAll()
                .requestMatchers("/v1/auth/admin/register").permitAll()
                .requestMatchers("/v1/auth/admin/login").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                // Actuator endpoints for monitoring and DevOps
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics", "/actuator/metrics/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                // Public article endpoints for website
                .requestMatchers("/v1/articles").permitAll()
                .requestMatchers("/v1/articles/view/**").permitAll()
                .requestMatchers("/v1/articles/search").permitAll()
                // Admin-only article endpoints
                .requestMatchers("/v1/articles/admin/**").hasRole("ADMIN")
                .requestMatchers("/v1/articles/create").hasRole("ADMIN")
                .requestMatchers("/v1/articles/edit/**").hasRole("ADMIN")
                .requestMatchers("/v1/articles/delete/**").hasRole("ADMIN")
                .requestMatchers("/v1/articles/manage/**").hasRole("ADMIN")
                .requestMatchers("/v1/articles/upload-docx").hasRole("ADMIN")
                // Admin-only post endpoints
                .requestMatchers("/admin/v1/posts/**").hasRole("ADMIN")
                // Admin-only moderation endpoints
                .requestMatchers("/api/moderation/**").hasRole("ADMIN")
                // Admin actuator endpoints
                .requestMatchers("/actuator/env", "/actuator/env/**").hasRole("ADMIN")
                .requestMatchers("/actuator/beans").hasRole("ADMIN")
                .requestMatchers("/actuator/configprops").hasRole("ADMIN")
                .requestMatchers("/actuator/mappings").hasRole("ADMIN")
                .requestMatchers("/actuator/conditions").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Admin auth endpoints
                .requestMatchers("/v1/auth/admin/revoke-token").hasRole("ADMIN")
                // Protected endpoints
                .requestMatchers("/v1/auth/test").authenticated()
                .requestMatchers("/v1/posts/create").authenticated()
                .requestMatchers("/v1/posts").permitAll()  // Allow public access to post list
                .requestMatchers("/v1/posts/*/comments").permitAll()  // Allow public access to comments
                .requestMatchers("/v1/user/**").authenticated()  // User profile and related endpoints
                .requestMatchers("/v1/notifications/**").authenticated()  // Notification endpoints
                .requestMatchers("/v1/conversations/**").authenticated()
                .requestMatchers("/v1/ai-chat/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 