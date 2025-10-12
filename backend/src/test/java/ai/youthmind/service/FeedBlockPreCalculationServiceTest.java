package ai.youthmind.service;

import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.UserFeedState;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.FeedBlockCacheRepository;
import ai.youthmind.repository.UserFeedStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedBlockPreCalculationService
 * Tests the "two rounds behind" logic and pre-calculation functionality
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class FeedBlockPreCalculationServiceTest {
    
    @Mock
    private RVectorService rVectorService;
    
    @Mock
    private FeedBlockGeneratorService feedBlockGeneratorService;
    
    @Mock
    private UserFeedStateRepository userFeedStateRepository;
    
    @Mock
    private FeedBlockCacheRepository feedBlockCacheRepository;
    
    @InjectMocks
    private FeedBlockPreCalculationService feedBlockPreCalculationService;
    
    private UUID testUserId;
    @Mock
    private UserFeedState testUserState;
    private UserRecommendationVector testRVector;
    private FeedBlockCache testFeedBlock;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Setup test UserFeedState mock
        when(testUserState.getUserId()).thenReturn(testUserId);
        when(testUserState.getCurrentBlockNumber()).thenReturn(2);
        when(testUserState.getTotalBlocksGenerated()).thenReturn(2);
        when(testUserState.getRVectorVersionForBlock(1)).thenReturn(5);
        when(testUserState.getRVectorVersionForBlock(2)).thenReturn(8);
        
        // Setup test R vector
        testRVector = new UserRecommendationVector();
        testRVector.setUserId(testUserId);
        testRVector.setVersion(12);
        testRVector.setEmotionValue("happy", 15);
        testRVector.setEmotionValue("sad", 10);
        
        // Setup test FeedBlockCache
        testFeedBlock = new FeedBlockCache();
        testFeedBlock.setId(UUID.randomUUID());
        testFeedBlock.setUserId(testUserId);
        testFeedBlock.setBlockNumber(3);
        testFeedBlock.setPostIds(java.util.List.of(UUID.randomUUID(), UUID.randomUUID()));
        testFeedBlock.setEmotionDistribution(Map.of("happy", 60, "sad", 40));
        testFeedBlock.setRVectorVersion(12);
        testFeedBlock.setGeneratedAt(LocalDateTime.now());
        testFeedBlock.setExpiresAt(LocalDateTime.now().plusHours(1));
    }
    
    @Test
    void testPreCalculateNextBlock_DefaultBlock_Blocks1And2() {
        // Arrange
        when(testUserState.getCurrentBlockNumber()).thenReturn(0); // Block 1
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(feedBlockGeneratorService.generateDefaultBlock(testUserId)).thenReturn(testFeedBlock);
        
        // Act
        FeedBlockCache result = feedBlockPreCalculationService.preCalculateNextBlock(testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testFeedBlock, result);
        verify(feedBlockGeneratorService).generateDefaultBlock(testUserId);
        verify(userFeedStateRepository).save(testUserState);
        verify(testUserState).setRVectorVersionForBlock(eq(1), eq(0)); // Version 0 for default, next block is 1
    }
    
    @Test
    void testPreCalculateNextBlock_TwoRoundsBehind_Block3() {
        // Arrange
        when(testUserState.getCurrentBlockNumber()).thenReturn(2); // Block 3
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(rVectorService.getVectorByVersion(testUserId, 5)).thenReturn(testRVector);
        when(feedBlockGeneratorService.generateBlockForUser(eq(testUserId), eq(3), eq(testRVector)))
            .thenReturn(testFeedBlock);
        
        // Act
        FeedBlockCache result = feedBlockPreCalculationService.preCalculateNextBlock(testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testFeedBlock, result);
        verify(rVectorService).getVectorByVersion(testUserId, 5); // Block 1's R vector version
        verify(feedBlockGeneratorService).generateBlockForUser(testUserId, 3, testRVector);
        verify(userFeedStateRepository).save(testUserState);
        verify(testUserState).setRVectorVersionForBlock(eq(3), eq(12)); // Current R vector version
    }
    
    @Test
    void testGetRVectorForBlock_ReturnsNull_ForBlocks1And2() {
        // Act & Assert
        assertNull(feedBlockPreCalculationService.getRVectorForBlock(testUserId, 1));
        assertNull(feedBlockPreCalculationService.getRVectorForBlock(testUserId, 2));
        
        // Verify no R vector service calls for blocks 1-2
        verify(rVectorService, never()).getVectorByVersion(any(), any());
    }
    
    @Test
    void testGetRVectorForBlock_ReturnsTwoRoundsBehind_ForBlock3Plus() {
        // Arrange
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        
        // Act
        Integer result = feedBlockPreCalculationService.getRVectorForBlock(testUserId, 3);
        
        // Assert
        assertEquals(5, result); // Block 1's R vector version (3-2=1)
        
        // Test block 4
        Integer result4 = feedBlockPreCalculationService.getRVectorForBlock(testUserId, 4);
        assertEquals(8, result4); // Block 2's R vector version (4-2=2)
    }
    
    @Test
    void testGetOrGenerateBlock_UsesCachedBlock() {
        // Arrange
        when(feedBlockCacheRepository.findByUserIdAndBlockNumber(testUserId, 3))
            .thenReturn(Optional.of(testFeedBlock));
        
        // Act
        FeedBlockCache result = feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 3);
        
        // Assert
        assertNotNull(result);
        assertEquals(testFeedBlock, result);
        verify(feedBlockCacheRepository).findByUserIdAndBlockNumber(testUserId, 3);
        verify(feedBlockGeneratorService, never()).generateDefaultBlock(any());
        verify(feedBlockGeneratorService, never()).generateBlockForUser(any(), any(), any());
    }
    
    @Test
    void testGetOrGenerateBlock_GeneratesNewBlock() {
        // Arrange
        when(feedBlockCacheRepository.findByUserIdAndBlockNumber(testUserId, 3))
            .thenReturn(Optional.empty());
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(rVectorService.getVectorByVersion(testUserId, 5)).thenReturn(testRVector);
        when(feedBlockGeneratorService.generateBlockForUser(eq(testUserId), eq(3), eq(testRVector)))
            .thenReturn(testFeedBlock);
        
        // Act
        FeedBlockCache result = feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 3);
        
        // Assert
        assertNotNull(result);
        assertEquals(testFeedBlock, result);
        verify(feedBlockCacheRepository).findByUserIdAndBlockNumber(testUserId, 3);
        verify(rVectorService).getVectorByVersion(testUserId, 5); // Two rounds behind
        verify(feedBlockGeneratorService).generateBlockForUser(testUserId, 3, testRVector);
    }
    
    @Test
    void testCleanupExpiredBlocks_Success() {
        // Arrange
        when(feedBlockCacheRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(5);
        
        // Act
        int result = feedBlockPreCalculationService.cleanupExpiredBlocks(testUserId);
        
        // Assert
        assertEquals(5, result);
        verify(feedBlockCacheRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
    
    @Test
    void testPreCalculateNextBlock_NullUserId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockPreCalculationService.preCalculateNextBlock(null);
        });
    }
    
    @Test
    void testGetRVectorForBlock_NullParameters_ReturnsNull() {
        // Act & Assert
        assertNull(feedBlockPreCalculationService.getRVectorForBlock(null, 3));
        assertNull(feedBlockPreCalculationService.getRVectorForBlock(testUserId, null));
    }
    
    @Test
    void testGetOrGenerateBlock_NullUserId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockPreCalculationService.getOrGenerateBlock(null, 3);
        });
    }
    
    @Test
    void testGetOrGenerateBlock_InvalidBlockNumber_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockPreCalculationService.getOrGenerateBlock(testUserId, null);
        });
    }
    
    @Test
    void testPreCalculateNextBlock_RVectorNotFound_FallbackToCurrent() {
        // Arrange
        testUserState.setCurrentBlockNumber(2); // Block 3
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(rVectorService.getVectorByVersion(testUserId, 5)).thenReturn(null); // R vector not found
        when(rVectorService.getCurrentVector(testUserId)).thenReturn(testRVector);
        when(feedBlockGeneratorService.generateBlockForUser(eq(testUserId), eq(3), eq(testRVector)))
            .thenReturn(testFeedBlock);
        
        // Act
        FeedBlockCache result = feedBlockPreCalculationService.preCalculateNextBlock(testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testFeedBlock, result);
        verify(rVectorService).getVectorByVersion(testUserId, 5);
        verify(rVectorService).getCurrentVector(testUserId); // Fallback
        verify(feedBlockGeneratorService).generateBlockForUser(testUserId, 3, testRVector);
    }
    
    @Test
    void testGetRVectorForBlock_NoUserState_ReturnsNull() {
        // Arrange
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        
        // Act
        Integer result = feedBlockPreCalculationService.getRVectorForBlock(testUserId, 3);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testCleanupExpiredBlocks_Exception_ReturnsZero() {
        // Arrange
        when(feedBlockCacheRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        int result = feedBlockPreCalculationService.cleanupExpiredBlocks(testUserId);
        
        // Assert
        assertEquals(0, result);
        verify(feedBlockCacheRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
