package ai.youthmind.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for security headers implementation
 * Verifies that security headers are properly set in responses
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("dev")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSecurityHeaders_DevelopmentProfile() throws Exception {
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
    void testSecurityHeaders_StagingProfile() throws Exception {
        mockMvc.perform(get("/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://staging.youthmind.ai"))
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
    void testSecurityHeaders_ProductionProfile() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://youthmind.ai"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Permissions-Policy"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload"));
    }

    @Test
    void testContentSecurityPolicy_ProductionProfile() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://youthmind.ai"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Content-Security-Policy", 
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self' https://api.deepseek.com; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'"));
    }

    @Test
    void testContentSecurityPolicy_DevelopmentProfile() throws Exception {
        mockMvc.perform(get("/v1/posts")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void testContentSecurityPolicy_StagingProfile() throws Exception {
        mockMvc.perform(get("/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://staging.youthmind.ai"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void testHstsHeader_OnlyInProduction() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://youthmind.ai"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload"));
    }

    @Test
    void testHstsHeader_NotInDevelopment() throws Exception {
        mockMvc.perform(get("/v1/posts")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void testHstsHeader_NotInStaging() throws Exception {
        mockMvc.perform(get("/v1/posts")
                .header(HttpHeaders.ORIGIN, "https://staging.youthmind.ai"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }
}
