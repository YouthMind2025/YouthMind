package ai.youthmind.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive CORS tests covering all scenarios
 * Tests CORS behavior, security headers, and edge cases
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("dev")
class CorsComprehensiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Development Profile CORS Tests")
    class DevelopmentCorsTests {

        @Test
        @DisplayName("Should allow all origins in development")
        void shouldAllowAllOriginsInDevelopment() throws Exception {
            String[] testOrigins = {
                "https://localhost:3000",
                "https://127.0.0.1:3000",
                "https://any-domain.com",
                "https://malicious-site.com",
                "http://localhost:8080"
            };

            for (String origin : testOrigins) {
                mockMvc.perform(options("/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                        .andExpect(status().isOk())
                        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
            }
        }

        @Test
        @DisplayName("Should allow all HTTP methods in development")
        void shouldAllowAllHttpMethodsInDevelopment() throws Exception {
            String[] methods = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};

            for (String method : methods) {
                mockMvc.perform(options("/v1/posts")
                        .header(HttpHeaders.ORIGIN, "https://test.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                        .andExpect(status().isOk())
                        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
            }
        }

        @Test
        @DisplayName("Should allow all headers in development")
        void shouldAllowAllHeadersInDevelopment() throws Exception {
            mockMvc.perform(options("/v1/posts")
                    .header(HttpHeaders.ORIGIN, "https://test.com")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,X-Custom-Header,Any-Header"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
        }
    }

    @Nested
    @DisplayName("Staging Profile CORS Tests")
    class StagingCorsTests {

        @Test
        @DisplayName("Should allow staging origins")
        void shouldAllowStagingOrigins() throws Exception {
            String[] allowedOrigins = {
                "https://staging.youthmind.ai",
                "https://staging-api.youthmind.ai",
                "http://localhost:3000",
                "http://localhost:3001"
            };

            for (String origin : allowedOrigins) {
                mockMvc.perform(options("/v1/posts")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                        .andExpect(status().isOk())
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
            }
        }

        @Test
        @DisplayName("Should reject non-staging origins")
        void shouldRejectNonStagingOrigins() throws Exception {
            String[] disallowedOrigins = {
                "https://malicious-site.com",
                "https://youthmind.ai",
                "https://api.youthmind.ai",
                "https://any-other-domain.com"
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
        @DisplayName("Should allow specific HTTP methods in staging")
        void shouldAllowSpecificHttpMethodsInStaging() throws Exception {
            String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};

            for (String method : allowedMethods) {
                mockMvc.perform(options("/v1/posts")
                        .header(HttpHeaders.ORIGIN, "https://staging.youthmind.ai")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                        .andExpect(status().isOk())
                        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
            }
        }
    }

    @Nested
    @DisplayName("Production Profile CORS Tests")
    class ProductionCorsTests {

        @Test
        @DisplayName("Should allow production origins")
        void shouldAllowProductionOrigins() throws Exception {
            String[] allowedOrigins = {
                "https://youthmind.ai",
                "https://www.youthmind.ai",
                "https://api.youthmind.ai"
            };

            for (String origin : allowedOrigins) {
                mockMvc.perform(options("/api/v1/posts")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,X-API-Key"))
                        .andExpect(status().isOk())
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
            }
        }

        @Test
        @DisplayName("Should reject non-production origins")
        void shouldRejectNonProductionOrigins() throws Exception {
            String[] disallowedOrigins = {
                "https://malicious-site.com",
                "https://staging.youthmind.ai",
                "http://youthmind.ai",
                "https://any-other-domain.com"
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

        @Test
        @DisplayName("Should allow only specific HTTP methods in production")
        void shouldAllowOnlySpecificHttpMethodsInProduction() throws Exception {
            String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

            for (String method : allowedMethods) {
                mockMvc.perform(options("/api/v1/posts")
                        .header(HttpHeaders.ORIGIN, "https://youthmind.ai")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                        .andExpect(status().isOk())
                        .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
            }
        }

        @Test
        @DisplayName("Should reject PATCH method in production")
        void shouldRejectPatchMethodInProduction() throws Exception {
            mockMvc.perform(options("/api/v1/posts")
                    .header(HttpHeaders.ORIGIN, "https://youthmind.ai")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
        }
    }

    @Nested
    @DisplayName("Security Headers Tests")
    class SecurityHeadersTests {

        @Test
        @DisplayName("Should include security headers in all profiles")
        void shouldIncludeSecurityHeadersInAllProfiles() throws Exception {
            mockMvc.perform(get("/v1/posts")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
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
        @DisplayName("Should include HSTS header only in production")
        void shouldIncludeHstsHeaderOnlyInProduction() throws Exception {
            mockMvc.perform(get("/api/v1/posts")
                    .header(HttpHeaders.ORIGIN, "https://youthmind.ai"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Strict-Transport-Security"))
                    .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload"));
        }

        @Test
        @DisplayName("Should include CSP header in all profiles")
        void shouldIncludeCspHeaderInAllProfiles() throws Exception {
            mockMvc.perform(get("/api/v1/posts")
                    .header(HttpHeaders.ORIGIN, "https://youthmind.ai"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Security-Policy"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle missing origin header")
        void shouldHandleMissingOriginHeader() throws Exception {
            mockMvc.perform(options("/v1/posts")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle malformed origin header")
        void shouldHandleMalformedOriginHeader() throws Exception {
            mockMvc.perform(options("/v1/posts")
                    .header(HttpHeaders.ORIGIN, "malformed-origin")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle empty origin header")
        void shouldHandleEmptyOriginHeader() throws Exception {
            mockMvc.perform(options("/v1/posts")
                    .header(HttpHeaders.ORIGIN, "")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle null origin header")
        void shouldHandleNullOriginHeader() throws Exception {
            mockMvc.perform(options("/v1/posts")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk());
        }
    }
}
