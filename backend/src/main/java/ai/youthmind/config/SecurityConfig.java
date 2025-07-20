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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JWTFilter jwtFilter;

    @Autowired
    private RateFilter rateFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Actuator endpoints for monitoring and DevOps
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics", "/actuator/metrics/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/env", "/actuator/env/**").hasRole("ADMIN")
                .requestMatchers("/actuator/beans").hasRole("ADMIN")
                .requestMatchers("/actuator/configprops").hasRole("ADMIN")
                .requestMatchers("/actuator/mappings").hasRole("ADMIN")
                .requestMatchers("/actuator/conditions").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Application endpoints
                .requestMatchers("/v1/auth/register").permitAll()
                .requestMatchers("/v1/posts/create").authenticated()
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