package ai.youthmind.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for admin secret configuration
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "admin.secret-key=TEST_ADMIN_SECRET_123"
})
public class AdminSecretConfigurationTest {

    @Autowired
    private AdminConfig adminConfig;

    @Test
    void testAdminSecretKeyConfiguration() {
        // Test that admin secret key is properly configured
        assertNotNull(adminConfig.getSecretKey(), "Admin secret key should not be null");
        assertEquals("TEST_ADMIN_SECRET_123", adminConfig.getSecretKey(), 
            "Admin secret key should match the configured value");
    }

    @Test
    void testAdminSecretKeyIsNotEmpty() {
        // Test that admin secret key is not empty
        assertFalse(adminConfig.getSecretKey().trim().isEmpty(), 
            "Admin secret key should not be empty");
    }

    @Test
    void testAdminSecretKeyLength() {
        // Test that admin secret key has reasonable length
        assertTrue(adminConfig.getSecretKey().length() >= 8, 
            "Admin secret key should be at least 8 characters long");
    }
}
