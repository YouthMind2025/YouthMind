package ai.youthmind.repository;

import ai.youthmind.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    public void testSaveAndFindUser() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        
        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        
        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(savedUser.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
    }
    
    @Test
    public void testFindByEmail() {
        // Given
        User user = new User();
        user.setEmail("unique@example.com");
        user.setPassword("password456");
        user.setFirstName("Unique");
        user.setLastName("User");
        userRepository.save(user);
        entityManager.flush();
        
        // When
        Optional<User> foundUser = userRepository.findByEmail("unique@example.com");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("unique@example.com");
        assertThat(foundUser.get().getFirstName()).isEqualTo("Unique");
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
    }
    
    @Test
    public void testExistsByEmail() {
        // Given
        User user = new User();
        user.setEmail("existing@example.com");
        user.setPassword("password789");
        user.setFirstName("Existing");
        user.setLastName("User");
        userRepository.save(user);
        entityManager.flush();
        
        // When & Then
        assertThat(userRepository.existsByEmail("existing@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexisting@example.com")).isFalse();
    }
    
    @Test
    public void testTimestampIsAutomaticallySet() {
        // Given
        LocalDateTime beforeSave = LocalDateTime.now();
        User user = new User();
        user.setEmail("timestamp@example.com");
        user.setPassword("passwordtime");
        user.setFirstName("Timestamp");
        user.setLastName("Test");
        
        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        LocalDateTime afterSave = LocalDateTime.now();
        
        // Then
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(savedUser.getCreatedAt()).isBeforeOrEqualTo(afterSave);
        assertThat(savedUser.getUpdatedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(savedUser.getUpdatedAt()).isBeforeOrEqualTo(afterSave);
    }
} 