package ai.youthmind.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostUpdateRequest DTO
 */
class PostUpdateRequestTest {

    @Test
    void testDefaultConstructor() {
        PostUpdateRequest request = new PostUpdateRequest();
        assertNotNull(request);
        assertNull(request.getTitle());
        assertNull(request.getContent());
        assertNull(request.getEmotion());
        assertNull(request.getPreImg());
        assertNull(request.getTopics());
    }

    @Test
    void testAllArgsConstructor() {
        PostUpdateRequest request = new PostUpdateRequest(
            "Updated Title",
            "Updated content",
            "happy",
            "https://example.com/new-image.jpg",
            "mental health, anxiety"
        );
        
        assertEquals("Updated Title", request.getTitle());
        assertEquals("Updated content", request.getContent());
        assertEquals("happy", request.getEmotion());
        assertEquals("https://example.com/new-image.jpg", request.getPreImg());
        assertEquals("mental health, anxiety", request.getTopics());
    }

    @Test
    void testHasUpdates_WithUpdates() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("New Title");
        
        assertTrue(request.hasUpdates());
    }

    @Test
    void testHasUpdates_NoUpdates() {
        PostUpdateRequest request = new PostUpdateRequest();
        
        assertFalse(request.hasUpdates());
    }

    @Test
    void testHasUpdates_PartialUpdates() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setEmotion("sad");
        request.setTopics("depression");
        
        assertTrue(request.hasUpdates());
    }

    @Test
    void testIsValidEmotion_ValidEmotions() {
        PostUpdateRequest request = new PostUpdateRequest();
        
        String[] validEmotions = {"angry", "sad", "happy", "conflicted", "surprised", "fear"};
        
        for (String emotion : validEmotions) {
            request.setEmotion(emotion);
            assertTrue(request.isValidEmotion(), "Emotion " + emotion + " should be valid");
        }
    }

    @Test
    void testIsValidEmotion_InvalidEmotions() {
        PostUpdateRequest request = new PostUpdateRequest();
        
        String[] invalidEmotions = {"excited", "calm", "worried", "joyful", "angry_sad"};
        
        for (String emotion : invalidEmotions) {
            request.setEmotion(emotion);
            assertFalse(request.isValidEmotion(), "Emotion " + emotion + " should be invalid");
        }
    }

    @Test
    void testIsValidEmotion_CaseInsensitive() {
        PostUpdateRequest request = new PostUpdateRequest();
        
        request.setEmotion("HAPPY");
        assertTrue(request.isValidEmotion());
        
        request.setEmotion("Happy");
        assertTrue(request.isValidEmotion());
        
        request.setEmotion("hApPy");
        assertTrue(request.isValidEmotion());
    }

    @Test
    void testIsValidEmotion_NullEmotion() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setEmotion(null);
        
        assertTrue(request.isValidEmotion());
    }

    @Test
    void testIsValidEmotion_EmptyEmotion() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setEmotion("");
        
        assertFalse(request.isValidEmotion());
    }

    @Test
    void testGetUpdateSummary_NoUpdates() {
        PostUpdateRequest request = new PostUpdateRequest();
        
        String summary = request.getUpdateSummary();
        assertEquals("Updating post fields: no fields provided.", summary);
    }

    @Test
    void testGetUpdateSummary_SingleUpdate() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("New Title");
        
        String summary = request.getUpdateSummary();
        assertEquals("Updating post fields: title.", summary);
    }

    @Test
    void testGetUpdateSummary_MultipleUpdates() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("New Title");
        request.setContent("New content");
        request.setEmotion("happy");
        
        String summary = request.getUpdateSummary();
        assertEquals("Updating post fields: title, content, emotion.", summary);
    }

    @Test
    void testGetUpdateSummary_AllFields() {
        PostUpdateRequest request = new PostUpdateRequest(
            "New Title",
            "New content",
            "happy",
            "https://example.com/image.jpg",
            "mental health"
        );
        
        String summary = request.getUpdateSummary();
        assertEquals("Updating post fields: title, content, emotion, preImg, topics.", summary);
    }

    @Test
    void testSettersAndGetters() {
        PostUpdateRequest request = new PostUpdateRequest();
        
        request.setTitle("Test Title");
        request.setContent("Test content");
        request.setEmotion("sad");
        request.setPreImg("https://example.com/test.jpg");
        request.setTopics("test, example");
        
        assertEquals("Test Title", request.getTitle());
        assertEquals("Test content", request.getContent());
        assertEquals("sad", request.getEmotion());
        assertEquals("https://example.com/test.jpg", request.getPreImg());
        assertEquals("test, example", request.getTopics());
    }
} 