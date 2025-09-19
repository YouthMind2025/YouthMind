package ai.youthmind.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for SecurityConfig CORS integration
 * Verifies that SecurityConfig properly integrates with CORS configuration
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("dev")
class SecurityConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    @DisplayName("SecurityConfig should integrate with CORS configuration in development")
    void securityConfigShouldIntegrateWithCorsInDevelopment() throws Exception {
        // Verify CORS configuration source is injected
        assertNotNull(corsConfigurationSource);

        // Test that CORS is properly configured in SecurityConfig
        mockMvc.perform(options("/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://test.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("SecurityConfig should integrate with CORS configuration in staging")
    void securityConfigShouldIntegrateWithCorsInStaging() throws Exception {
        // Verify CORS configuration source is injected
        assertNotNull(corsConfigurationSource);

        // Test that CORS is properly configured in SecurityConfig
        mockMvc.perform(options("/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://staging.youthmind.ai")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://staging.youthmind.ai"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("SecurityConfig should integrate with CORS configuration in production")
    void securityConfigShouldIntegrateWithCorsInProduction() throws Exception {
        // Verify CORS configuration source is injected
        assertNotNull(corsConfigurationSource);

        // Test that CORS is properly configured in SecurityConfig
        mockMvc.perform(options("/api/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://youthmind.ai")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,X-API-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://youthmind.ai"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("SecurityConfig should include security headers")
    void securityConfigShouldIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(options("/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://test.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    @DisplayName("SecurityConfig should include HSTS header in production")
    void securityConfigShouldIncludeHstsHeaderInProduction() throws Exception {
        mockMvc.perform(options("/api/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://youthmind.ai")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload"));
    }

    @Test
    @DisplayName("SecurityConfig should include CSP header")
    void securityConfigShouldIncludeCspHeader() throws Exception {
        mockMvc.perform(options("/api/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://youthmind.ai")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("SecurityConfig should handle CORS preflight requests")
    void securityConfigShouldHandleCorsPreflightRequests() throws Exception {
        // Test various preflight scenarios
        String[] origins = {"https://test.com", "https://localhost:3000", "https://any-domain.com"};
        String[] methods = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};

        for (String origin : origins) {
            for (String method : methods) {
                mockMvc.perform(options("/v1/posts")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                        .andExpect(status().isOk())
                        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
            }
        }
    }

    @Test
    @DisplayName("SecurityConfig should reject disallowed origins in staging")
    void securityConfigShouldRejectDisallowedOriginsInStaging() throws Exception {
        String[] disallowedOrigins = {
            "https://malicious-site.com",
            "https://youthmind.ai",
            "https://api.youthmind.ai"
        };

        for (String origin : disallowedOrigins) {
            mockMvc.perform(options("/v1/posts")
                    .header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Test
    @DisplayName("SecurityConfig should reject disallowed origins in production")
    void securityConfigShouldRejectDisallowedOriginsInProduction() throws Exception {
        String[] disallowedOrigins = {
            "https://malicious-site.com",
            "https://staging.youthmind.ai",
            "http://youthmind.ai"
        };

        for (String origin : disallowedOrigins) {
            mockMvc.perform(options("/api/v1/posts")
                    .header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
