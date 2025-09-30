package ai.youthmind.service;

import ai.youthmind.entity.User;
import ai.youthmind.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserCreationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testCreateBasicUser() {
        // Create a basic user
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword123");
        user.setUsername("testuser");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setBio("This is a test user bio");
        user.setAvatar("https://example.com/avatar.jpg");
        user.setPostsCount(0);
        user.setLikesCount(0);
        user.setBookmarksCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setRole(User.Role.USER);

        // Save the user to database
        User savedUser = userRepository.save(user);

        // Verify the user was saved
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("John", savedUser.getFirstName());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(User.Role.USER, savedUser.getRole());

        // Verify we can find the user by email
        Optional<User> foundByEmail = userRepository.findByEmail("test@example.com");
        assertTrue(foundByEmail.isPresent());
        assertEquals(savedUser.getId(), foundByEmail.get().getId());

        // Verify we can find the user by username
        Optional<User> foundByUsername = userRepository.findByUsername("testuser");
        assertTrue(foundByUsername.isPresent());
        assertEquals(savedUser.getId(), foundByUsername.get().getId());

        // Verify email uniqueness constraint
        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertTrue(userRepository.existsByUsername("testuser"));
    }

    @Test
    public void testCreateUserWithMinimalData() {
        // Create a user with only required fields
        User user = new User();
        user.setEmail("minimal@example.com");
        user.setPassword("password123");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Save the user
        User savedUser = userRepository.save(user);

        // Verify the user was saved with defaults
        assertNotNull(savedUser.getId());
        assertEquals("minimal@example.com", savedUser.getEmail());
        assertEquals(0, savedUser.getPostsCount());
        assertEquals(0, savedUser.getLikesCount());
        assertEquals(0, savedUser.getBookmarksCount());
        assertEquals(User.Role.USER, savedUser.getRole()); // Default role
    }

    @Test
    public void testCreateAdminUser() {
        // Create an admin user
        User adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("adminPassword123");
        adminUser.setUsername("admin");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());

        // Save the admin user
        User savedAdmin = userRepository.save(adminUser);

        // Verify admin role
        assertNotNull(savedAdmin.getId());
        assertEquals(User.Role.ADMIN, savedAdmin.getRole());
        assertEquals("admin@example.com", savedAdmin.getEmail());
    }

    @Test
    public void testUserLifecycle() {
        // Create user
        User user = new User();
        user.setEmail("lifecycle@example.com");
        user.setPassword("password123");
        user.setUsername("lifecycleuser");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        UUID userId = savedUser.getId();

        // Update user
        savedUser.setFirstName("Updated");
        savedUser.setLastName("Name");
        savedUser.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(savedUser);

        assertEquals("Updated", updatedUser.getFirstName());
        assertEquals("Name", updatedUser.getLastName());

        // Delete user
        userRepository.deleteById(userId);

        // Verify user is deleted
        Optional<User> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isPresent());
    }
} 