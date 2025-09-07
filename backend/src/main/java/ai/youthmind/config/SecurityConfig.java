package ai.youthmind.config;

import ai.youthmind.filter.JWTFilter;
import ai.youthmind.filter.RateFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JWTFilter jwtFilter;

    //@Autowired
    //private RateFilter rateFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (must be first)
                .requestMatchers("/").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/v1/auth/register").permitAll()
                .requestMatchers("/v1/auth/login").permitAll()
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
                // Protected endpoints
                .requestMatchers("/v1/auth/test").authenticated()
                .requestMatchers("/v1/posts/create").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            //.addFilterAfter(rateFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 