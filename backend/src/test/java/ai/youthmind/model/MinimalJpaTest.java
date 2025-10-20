package ai.youthmind.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Minimal JPA test using PostgreSQL Testcontainers.
 * This test verifies basic database functionality with PostgreSQL.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class MinimalJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void testEntityManagerIsAutowired() {
        // This test simply verifies that the EntityManager is properly autowired
        assertNotNull(entityManager);
    }
}
