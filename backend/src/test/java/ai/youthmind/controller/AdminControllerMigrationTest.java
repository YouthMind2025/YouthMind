package ai.youthmind.controller;

import ai.youthmind.service.DataMigrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminController migration endpoint
 * Tests the POST /admin/v1/users/migrate endpoint functionality
 */
@ExtendWith(MockitoExtension.class)
public class AdminControllerMigrationTest {

    private MockMvc mockMvc;

    @Mock
    private DataMigrationService dataMigrationService;

    @InjectMocks
    private AdminController adminController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();
    }

    /**
     * Test successful migration with default parameters
     */
    @Test
    public void testMigrateUsers_Success() throws Exception {
        // Arrange
        DataMigrationService.MigrationResult mockResult = new DataMigrationService.MigrationResult(100, 50, 0);
        when(dataMigrationService.runAllMigrations()).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dryRun").value(false))
                .andExpect(jsonPath("$.data.batchSize").value(100))
                .andExpect(jsonPath("$.data.totalProcessed").value(150))
                .andExpect(jsonPath("$.data.rVectorsCreated").value(100))
                .andExpect(jsonPath("$.data.feedStatesCreated").value(100))
                .andExpect(jsonPath("$.data.skipped").value(50))
                .andExpect(jsonPath("$.data.errors").value(0))
                .andExpect(jsonPath("$.data.successful").value(true))
                .andExpect(jsonPath("$.data.hasErrors").value(false))
                .andExpect(jsonPath("$.data.completedAt").exists());
    }

    /**
     * Test migration with dry run enabled
     */
    @Test
    public void testMigrateUsers_WithDryRun() throws Exception {
        // Arrange
        DataMigrationService.MigrationResult mockResult = new DataMigrationService.MigrationResult(75, 25, 0);
        when(dataMigrationService.runAllMigrations()).thenReturn(mockResult);

        String requestBody = """
            {
                "dryRun": true,
                "batchSize": 200
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dryRun").value(true))
                .andExpect(jsonPath("$.data.batchSize").value(200))
                .andExpect(jsonPath("$.data.totalProcessed").value(100))
                .andExpect(jsonPath("$.data.rVectorsCreated").value(75))
                .andExpect(jsonPath("$.data.feedStatesCreated").value(75))
                .andExpect(jsonPath("$.data.skipped").value(25))
                .andExpect(jsonPath("$.data.errors").value(0));
    }

    /**
     * Test migration with invalid batch size (too small)
     */
    @Test
    public void testMigrateUsers_InvalidBatchSize_TooSmall() throws Exception {
        String requestBody = """
            {
                "dryRun": false,
                "batchSize": 0
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * Test migration with invalid batch size (too large)
     */
    @Test
    public void testMigrateUsers_InvalidBatchSize_TooLarge() throws Exception {
        String requestBody = """
            {
                "dryRun": false,
                "batchSize": 1001
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * Test migration with errors during processing
     */
    @Test
    public void testMigrateUsers_WithErrors() throws Exception {
        // Arrange
        DataMigrationService.MigrationResult mockResult = new DataMigrationService.MigrationResult(80, 15, 5);
        when(dataMigrationService.runAllMigrations()).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalProcessed").value(100))
                .andExpect(jsonPath("$.data.rVectorsCreated").value(80))
                .andExpect(jsonPath("$.data.feedStatesCreated").value(80))
                .andExpect(jsonPath("$.data.skipped").value(15))
                .andExpect(jsonPath("$.data.errors").value(5))
                .andExpect(jsonPath("$.data.successful").value(false))
                .andExpect(jsonPath("$.data.hasErrors").value(true));
    }

    /**
     * Test migration service failure
     */
    @Test
    public void testMigrateUsers_ServiceFailure() throws Exception {
        // Arrange
        when(dataMigrationService.runAllMigrations())
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * Test migration with custom batch size
     */
    @Test
    public void testMigrateUsers_CustomBatchSize() throws Exception {
        // Arrange
        DataMigrationService.MigrationResult mockResult = new DataMigrationService.MigrationResult(50, 10, 0);
        when(dataMigrationService.runAllMigrations()).thenReturn(mockResult);

        String requestBody = """
            {
                "batchSize": 500
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchSize").value(500))
                .andExpect(jsonPath("$.data.totalProcessed").value(60))
                .andExpect(jsonPath("$.data.rVectorsCreated").value(50))
                .andExpect(jsonPath("$.data.feedStatesCreated").value(50))
                .andExpect(jsonPath("$.data.skipped").value(10))
                .andExpect(jsonPath("$.data.errors").value(0));
    }

    /**
     * Test migration with null request body (should use defaults)
     */
    @Test
    public void testMigrateUsers_NullRequestBody() throws Exception {
        // Arrange
        DataMigrationService.MigrationResult mockResult = new DataMigrationService.MigrationResult(30, 5, 0);
        when(dataMigrationService.runAllMigrations()).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/admin/v1/users/migrate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dryRun").value(false))
                .andExpect(jsonPath("$.data.batchSize").value(100))
                .andExpect(jsonPath("$.data.totalProcessed").value(35))
                .andExpect(jsonPath("$.data.rVectorsCreated").value(30))
                .andExpect(jsonPath("$.data.feedStatesCreated").value(30))
                .andExpect(jsonPath("$.data.skipped").value(5))
                .andExpect(jsonPath("$.data.errors").value(0));
    }
}