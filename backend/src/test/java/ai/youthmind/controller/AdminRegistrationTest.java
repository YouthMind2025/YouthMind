package ai.youthmind.controller;

import ai.youthmind.config.AdminConfig;
import ai.youthmind.dto.AdminRegisterRequest;
import ai.youthmind.dto.AdminRegisterResponse;
import ai.youthmind.entity.User;
import ai.youthmind.service.JWTService;
import ai.youthmind.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for admin registration with environment variable configuration
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "admin.secret-key=TEST_ADMIN_SECRET_123"
})
public class AdminRegistrationTest {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public PasswordEncoder testPasswordEncoder() {
            return new PasswordEncoder() {
                @Override
                public String encode(CharSequence rawPassword) {
                    return "$2a$10$test." + rawPassword.hashCode(); // Mock BCrypt-like encoding
                }
                
                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return encodedPassword.equals(encode(rawPassword));
                }
            };
        }
        
        @Bean
        public AdminConfig adminConfig() {
            AdminConfig config = new AdminConfig();
            config.setSecretKey("TEST_ADMIN_SECRET_123");
            return config;
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AdminConfig adminConfig;

    @MockBean
    private UserService userService;

    @MockBean
    private JWTService jwtService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testAdminRegistrationWithCorrectSecret() throws Exception {
        // Arrange
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setUsername("admin");
        request.setAdminSecretKey("TEST_ADMIN_SECRET_123");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("admin@test.com");
        mockUser.setRole(User.Role.ADMIN);

        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(userService.saveUser(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn(
            org.springframework.data.util.Pair.of("jwt_token", new java.sql.Timestamp(System.currentTimeMillis()))
        );

        // Act & Assert
        mockMvc.perform(post("/v1/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(mockUser.getId().toString()))
                .andExpect(jsonPath("$.data.token").value("jwt_token"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void testAdminRegistrationWithIncorrectSecret() throws Exception {
        // Arrange
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setUsername("admin");
        request.setAdminSecretKey("WRONG_SECRET");

        // Act & Assert
        mockMvc.perform(post("/v1/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message").value("Invalid admin secret key"));
    }

    @Test
    void testAdminRegistrationWithoutSecret() throws Exception {
        // Arrange
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setUsername("admin");
        // adminSecretKey is null

        // Act & Assert
        mockMvc.perform(post("/v1/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message").value("Invalid admin secret key"));
    }

    @Test
    void testAdminRegistrationWithEmptySecret() throws Exception {
        // Arrange
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setUsername("admin");
        request.setAdminSecretKey("");

        // Act & Assert
        mockMvc.perform(post("/v1/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message").value("Invalid admin secret key"));
    }

    @Test
    void testAdminConfigInjection() {
        // Test that AdminConfig is properly injected and configured
        assertNotNull(adminConfig, "AdminConfig should be injected");
        assertNotNull(adminConfig.getSecretKey(), "Admin secret key should not be null");
        assertEquals(adminConfig.getSecretKey(), "TEST_ADMIN_SECRET_123", 
            "Admin secret key should match the test configuration");
    }
}
