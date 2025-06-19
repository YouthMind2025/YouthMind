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
        user.setEmailHash("test_hash_123");
        
        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        
        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmailHash()).isEqualTo("test_hash_123");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(savedUser.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
    }
    
    @Test
    public void testFindByEmailHash() {
        // Given
        User user = new User();
        user.setEmailHash("unique_hash_456");
        userRepository.save(user);
        entityManager.flush();
        
        // When
        Optional<User> foundUser = userRepository.findByEmailHash("unique_hash_456");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmailHash()).isEqualTo("unique_hash_456");
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
    }
    
    @Test
    public void testExistsByEmailHash() {
        // Given
        User user = new User();
        user.setEmailHash("existing_hash_789");
        userRepository.save(user);
        entityManager.flush();
        
        // When & Then
        assertThat(userRepository.existsByEmailHash("existing_hash_789")).isTrue();
        assertThat(userRepository.existsByEmailHash("non_existing_hash")).isFalse();
    }
    
    @Test
    public void testTimestampIsAutomaticallySet() {
        // Given
        LocalDateTime beforeSave = LocalDateTime.now();
        User user = new User();
        user.setEmailHash("timestamp_test_hash");
        
        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        LocalDateTime afterSave = LocalDateTime.now();
        
        // Then
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(savedUser.getCreatedAt()).isBeforeOrEqualTo(afterSave);
    }
} 