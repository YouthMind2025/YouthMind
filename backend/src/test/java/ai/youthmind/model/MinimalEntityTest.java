package ai.youthmind.model;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Minimal test class that doesn't require Spring context or database
 */
public class MinimalEntityTest {

    @Test
    public void testPostEntity() {
        // Create a post entity
        Post post = new Post();
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        post.setId(postId);
        post.setUserId(userId);
        post.setContent("Test content");
        post.setEmotion("happy");
        post.setCreatedAt(now);
        
        // Verify properties
        assertEquals(postId, post.getId());
        assertEquals(userId, post.getUserId());
        assertEquals("Test content", post.getContent());
        assertEquals("happy", post.getEmotion());
        assertEquals(now, post.getCreatedAt());
    }
    
    @Test
    public void testUserEntity() {
        // Create a user entity
        User user = new User();
        UUID userId = UUID.randomUUID();
        
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        
        // Verify properties
        assertEquals(userId, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test", user.getFirstName());
        assertEquals("User", user.getLastName());
    }
}

