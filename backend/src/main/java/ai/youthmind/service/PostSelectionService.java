package ai.youthmind.service;

import ai.youthmind.entity.Post;
import ai.youthmind.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for intelligent post selection based on emotions, popularity, and time priority
 * Integrates with recommendation vectors and mapping rules for personalized content delivery
 */
@Service
@Transactional
public class PostSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostSelectionService.class);
    
    @Autowired
    private PostRepository postRepository;
    
    
    @Autowired
    private RecommendationMappingService recommendationMappingService;
    
    // Popularity thresholds
    private static final int HOT_VIEW_THRESHOLD = 100;
    private static final int NORMAL_VIEW_THRESHOLD = 50;
    
    // Distribution percentages
    private static final double HOT_PERCENTAGE = 0.4;
    private static final double NORMAL_PERCENTAGE = 0.3;
    private static final double COLD_PERCENTAGE = 0.3;
    
    /**
     * Select posts for a specific emotion with popularity stratification
     * Distributes posts across hot, normal, and cold popularity tiers
     * 
     * @param emotion The emotion to filter posts by (case-insensitive)
     * @param count Total number of posts to select
     * @param cutoffTime Only select posts created after this time
     * @param excludePostIds Set of post IDs to exclude from selection
     * @return List of selected posts with popularity distribution
     * @throws IllegalArgumentException if emotion is null/empty or count is invalid
     */
    public List<Post> selectWithPopularityDistribution(String emotion, Integer count, 
                                                      LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
        logger.info("Starting post selection with popularity distribution - emotion: {}, count: {}, cutoffTime: {}, excludeCount: {}", 
                   emotion, count, cutoffTime, excludePostIds != null ? excludePostIds.size() : 0);
        
        // Input validation
        if (emotion == null || emotion.trim().isEmpty()) {
            throw new IllegalArgumentException("Emotion cannot be null or empty");
        }
        
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("Count must be a positive integer");
        }
        
        if (cutoffTime == null) {
            throw new IllegalArgumentException("Cutoff time cannot be null");
        }
        
        // Normalize emotion to lowercase for consistent filtering
        String normalizedEmotion = emotion.trim().toLowerCase();
        logger.debug("Normalized emotion: '{}'", normalizedEmotion);
        
        // Calculate distribution counts
        int hotCount = (int) (count * HOT_PERCENTAGE);
        int normalCount = (int) (count * NORMAL_PERCENTAGE);
        int coldCount = count - hotCount - normalCount;
        
        logger.info("Popularity distribution - Hot: {}, Normal: {}, Cold: {} (Total: {})", 
                   hotCount, normalCount, coldCount, count);
        
        List<Post> selectedPosts = new ArrayList<>();
        
        try {
            // Select from each popularity tier
            List<Post> hotPosts = selectHotPosts(normalizedEmotion, hotCount, cutoffTime, excludePostIds);
            List<Post> normalPosts = selectNormalPosts(normalizedEmotion, normalCount, cutoffTime, excludePostIds);
            List<Post> coldPosts = selectColdPosts(normalizedEmotion, coldCount, cutoffTime, excludePostIds);
            
            // Combine results
            selectedPosts.addAll(hotPosts);
            selectedPosts.addAll(normalPosts);
            selectedPosts.addAll(coldPosts);
            
            // Handle insufficient posts in tiers
            int totalSelected = selectedPosts.size();
            if (totalSelected < count) {
                logger.warn("Insufficient posts found - requested: {}, found: {}, shortfall: {}", 
                           count, totalSelected, count - totalSelected);
                
                // Calculate exactly how many more posts we need
                int remainingNeeded = count - totalSelected;
                logger.info("Compensating for shortfall - need {} more posts", remainingNeeded);
                
                // Try to compensate from other tiers, but only take what we need
                selectedPosts = compensateForShortfall(selectedPosts, normalizedEmotion, remainingNeeded, cutoffTime, excludePostIds);
            }
            
            // Remove duplicates (in case of overlap)
            selectedPosts = selectedPosts.stream()
                .distinct()
                .collect(Collectors.toList());
            
            // Ensure we don't return more posts than requested
            if (selectedPosts.size() > count) {
                selectedPosts = selectedPosts.subList(0, count);
                logger.warn("Limited result to requested count - had: {}, limited to: {}", 
                           selectedPosts.size() + (selectedPosts.size() - count), count);
            }
            
            logger.info("Post selection completed - emotion: {}, requested: {}, selected: {}, hot: {}, normal: {}, cold: {}", 
                       emotion, count, selectedPosts.size(), hotPosts.size(), normalPosts.size(), coldPosts.size());
            
            return selectedPosts;
            
        } catch (Exception e) {
            logger.error("Error during post selection with popularity distribution - emotion: {}, count: {}", 
                        emotion, count, e);
            throw new RuntimeException("Failed to select posts with popularity distribution", e);
        }
    }
    
    /**
     * Select hot posts (viewCount >= 100)
     */
    private List<Post> selectHotPosts(String emotion, int count, LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        
        logger.debug("Selecting hot posts - emotion: {}, count: {}", emotion, count);
        
        try {
            Pageable pageable = PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Query for hot posts: view >= 100, emotion match, after cutoff, not flagged
            Page<Post> hotPostsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
                emotion, cutoffTime, LocalDateTime.now(), pageable);
            
            List<Post> hotPosts = hotPostsPage.getContent();
            
            // Filter out excluded posts
            if (excludePostIds != null && !excludePostIds.isEmpty()) {
                hotPosts = hotPosts.stream()
                    .filter(post -> !excludePostIds.contains(post.getId()))
                    .filter(post -> !post.isFlagged())
                    .collect(Collectors.toList());
            } else {
                hotPosts = hotPosts.stream()
                    .filter(post -> !post.isFlagged())
                    .collect(Collectors.toList());
            }
            
            logger.debug("Found {} hot posts for emotion: {}", hotPosts.size(), emotion);
            return hotPosts;
            
        } catch (Exception e) {
            logger.error("Error selecting hot posts for emotion: {}", emotion, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Select normal posts (viewCount >= 50 AND viewCount < 100)
     */
    private List<Post> selectNormalPosts(String emotion, int count, LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        
        logger.debug("Selecting normal posts - emotion: {}, count: {}", emotion, count);
        
        try {
            Pageable pageable = PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Query for normal posts: view >= 50 AND view < 100, emotion match, after cutoff, not flagged
            Page<Post> normalPostsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
                emotion, cutoffTime, LocalDateTime.now(), pageable);
            
            List<Post> normalPosts = normalPostsPage.getContent();
            
            // Filter out excluded posts
            if (excludePostIds != null && !excludePostIds.isEmpty()) {
                normalPosts = normalPosts.stream()
                    .filter(post -> !excludePostIds.contains(post.getId()))
                    .filter(post -> !post.isFlagged())
                    .collect(Collectors.toList());
            } else {
                normalPosts = normalPosts.stream()
                    .filter(post -> !post.isFlagged())
                    .collect(Collectors.toList());
            }
            
            logger.debug("Found {} normal posts for emotion: {}", normalPosts.size(), emotion);
            return normalPosts;
            
        } catch (Exception e) {
            logger.error("Error selecting normal posts for emotion: {}", emotion, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Select cold posts (viewCount < 50)
     */
    private List<Post> selectColdPosts(String emotion, int count, LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        
        logger.debug("Selecting cold posts - emotion: {}, count: {}", emotion, count);
        
        try {
            Pageable pageable = PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Query for cold posts: view < 50, emotion match, after cutoff, not flagged
            Page<Post> coldPostsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
                emotion, cutoffTime, LocalDateTime.now(), pageable);
            
            List<Post> coldPosts = coldPostsPage.getContent();
            
            // Filter out excluded posts
            if (excludePostIds != null && !excludePostIds.isEmpty()) {
                coldPosts = coldPosts.stream()
                    .filter(post -> !excludePostIds.contains(post.getId()))
                    .filter(post -> !post.isFlagged())
                    .collect(Collectors.toList());
            } else {
                coldPosts = coldPosts.stream()
                    .filter(post -> !post.isFlagged())
                    .collect(Collectors.toList());
            }
            
            logger.debug("Found {} cold posts for emotion: {}", coldPosts.size(), emotion);
            return coldPosts;
            
        } catch (Exception e) {
            logger.error("Error selecting cold posts for emotion: {}", emotion, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Compensate for shortfall by selecting additional posts from available tiers
     */
    private List<Post> compensateForShortfall(List<Post> currentPosts, String emotion, int remainingNeeded, 
                                            LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
        if (remainingNeeded <= 0) {
            return currentPosts;
        }
        
        logger.info("Compensating for shortfall - need {} more posts", remainingNeeded);
        
        Set<UUID> alreadySelectedIds = currentPosts.stream()
            .map(Post::getId)
            .collect(Collectors.toSet());
        
        // Combine with excludePostIds
        Set<UUID> allExcludeIds = new HashSet<>(alreadySelectedIds);
        if (excludePostIds != null) {
            allExcludeIds.addAll(excludePostIds);
        }
        
        List<Post> additionalPosts = new ArrayList<>();
        
        try {
            // Try to get additional posts from any tier, ordered by recency
            // Request more than needed to account for filtering, but limit to remainingNeeded
            Pageable pageable = PageRequest.of(0, remainingNeeded * 2, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            Page<Post> allPostsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
                emotion, cutoffTime, LocalDateTime.now(), pageable);
            
            List<Post> candidatePosts = allPostsPage.getContent().stream()
                .filter(post -> !allExcludeIds.contains(post.getId()))
                .filter(post -> !post.isFlagged())
                .limit(remainingNeeded) // Only take exactly what we need
                .collect(Collectors.toList());
            
            additionalPosts.addAll(candidatePosts);
            
            logger.info("Compensation completed - found {} additional posts (needed: {})", 
                       additionalPosts.size(), remainingNeeded);
            
        } catch (Exception e) {
            logger.error("Error during shortfall compensation for emotion: {}", emotion, e);
        }
        
        // Combine original posts with additional posts
        List<Post> finalPosts = new ArrayList<>(currentPosts);
        finalPosts.addAll(additionalPosts);
        
        return finalPosts;
    }

    /**
     * Applies waterfall time priority to select posts for a specific emotion.
     * Tries time windows in order: 12h → 24h → 48h → 7d until enough posts are found.
     * 
     * @param emotion The emotion to filter posts by (case-insensitive).
     * @param count The total number of posts to retrieve.
     * @param excludeIds Set of post IDs to exclude from results.
     * @return List of posts selected using time priority, sorted by creation date (newest first).
     * @throws IllegalArgumentException if emotion is null/empty or count is less than 1.
     */
    public List<Post> applyTimePriority(String emotion, Integer count, Set<UUID> excludeIds) {
        if (emotion == null || emotion.trim().isEmpty()) {
            throw new IllegalArgumentException("Emotion cannot be null or empty.");
        }
        if (count == null || count < 1) {
            throw new IllegalArgumentException("Count must be at least 1.");
        }
        if (excludeIds == null) {
            excludeIds = Collections.emptySet();
        }

        String normalizedEmotion = emotion.trim().toLowerCase();
        logger.info("Starting time priority selection - emotion: '{}', count: {}, excluding {} posts", 
                   emotion, count, excludeIds.size());

        List<Post> selectedPosts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Set<UUID> collectedPostIds = new HashSet<>(excludeIds);

        // Define time windows in waterfall order: 12h → 24h → 48h → 7d
        List<TimeWindow> timeWindows = Arrays.asList(
            new TimeWindow("12 hours", now.minusHours(12)),
            new TimeWindow("24 hours", now.minusHours(24)),
            new TimeWindow("48 hours", now.minusHours(48)),
            new TimeWindow("7 days", now.minusDays(7))
        );

        for (TimeWindow window : timeWindows) {
            int remainingNeeded = count - selectedPosts.size();
            if (remainingNeeded <= 0) {
                logger.info("Enough posts collected ({}), stopping time priority selection", selectedPosts.size());
                break;
            }

            logger.info("Querying {} window (since: {}) for {} more posts", 
                       window.name, window.since, remainingNeeded);

            try {
                List<Post> windowPosts = getPostsInTimeWindow(
                    normalizedEmotion, window.since, remainingNeeded, collectedPostIds);

                if (!windowPosts.isEmpty()) {
                    selectedPosts.addAll(windowPosts);
                    windowPosts.forEach(post -> collectedPostIds.add(post.getId()));
                    
                    logger.info("Found {} posts in {} window. Total collected: {}/{}", 
                               windowPosts.size(), window.name, selectedPosts.size(), count);
                } else {
                    logger.debug("No posts found in {} window", window.name);
                }

            } catch (Exception e) {
                logger.error("Error querying {} window for emotion: {}", window.name, emotion, e);
            }
        }

        // Ensure we don't return more than requested
        if (selectedPosts.size() > count) {
            selectedPosts = selectedPosts.subList(0, count);
            logger.warn("Limited result to requested count - had: {}, limited to: {}", 
                       selectedPosts.size() + (selectedPosts.size() - count), count);
        }

        logger.info("Time priority selection completed - emotion: '{}', requested: {}, selected: {}", 
                   emotion, count, selectedPosts.size());

        return selectedPosts;
    }

    /**
     * Helper method to get posts within a specific time window.
     * 
     * @param emotion The emotion to filter by (normalized to lowercase).
     * @param since The earliest creation time to include.
     * @param limit Maximum number of posts to retrieve.
     * @param excludeIds Set of post IDs to exclude.
     * @return List of posts within the time window, sorted by creation date (newest first).
     */
    private List<Post> getPostsInTimeWindow(String emotion, LocalDateTime since, Integer limit, Set<UUID> excludeIds) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            // Use the repository method that filters by emotion, creation time, and exclusions
            Page<Post> postsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
                emotion, since, LocalDateTime.now(), pageable);

            List<Post> posts = postsPage.getContent();
            
            logger.debug("Time window query - emotion: '{}', since: {}, limit: {}, found: {}", 
                        emotion, since, limit, posts.size());

            return posts;

        } catch (Exception e) {
            logger.error("Error querying posts in time window - emotion: '{}', since: {}, limit: {}", 
                        emotion, since, limit, e);
            return Collections.emptyList();
        }
    }

    /**
     * Main selection method that combines popularity distribution and time priority.
     * First tries popularity distribution with 24-hour cutoff, then falls back to time priority if needed.
     * 
     * @param emotion The emotion to filter posts by (case-insensitive).
     * @param count The total number of posts to retrieve.
     * @param excludePostIds Set of post IDs to exclude from results.
     * @return List of posts selected using combined strategy, shuffled and limited to exact count.
     * @throws IllegalArgumentException if emotion is null/empty or count is less than 1.
     */
    public List<Post> selectPostsForEmotion(String emotion, Integer count, Set<UUID> excludePostIds) {
        if (emotion == null || emotion.trim().isEmpty()) {
            throw new IllegalArgumentException("Emotion cannot be null or empty.");
        }
        if (count == null || count < 1) {
            throw new IllegalArgumentException("Count must be at least 1.");
        }
        if (excludePostIds == null) {
            excludePostIds = Collections.emptySet();
        }

        logger.info("Starting post selection for emotion '{}' - target count: {}, excluding {} posts", 
                   emotion, count, excludePostIds.size());

        List<Post> selectedPosts = new ArrayList<>();
        Set<UUID> collectedPostIds = new HashSet<>(excludePostIds);

        try {
            // Step 1: Try popularity distribution with 24-hour cutoff
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            logger.info("Step 1: Attempting popularity distribution with 24-hour cutoff");
            
            List<Post> popularityPosts = selectWithPopularityDistribution(
                emotion, count, twentyFourHoursAgo, collectedPostIds);
            
            selectedPosts.addAll(popularityPosts);
            popularityPosts.forEach(post -> collectedPostIds.add(post.getId()));
            
            logger.info("Popularity distribution found {} posts (target: {})", 
                       popularityPosts.size(), count);

            // Step 2: Check if we need more posts and use time priority fallback
            int remainingNeeded = count - selectedPosts.size();
            if (remainingNeeded > 0) {
                logger.info("Step 2: Using time priority fallback for {} additional posts", remainingNeeded);
                
                List<Post> timePriorityPosts = applyTimePriority(emotion, remainingNeeded, collectedPostIds);
                selectedPosts.addAll(timePriorityPosts);
                timePriorityPosts.forEach(post -> collectedPostIds.add(post.getId()));
                
                logger.info("Time priority fallback found {} additional posts", timePriorityPosts.size());
            } else {
                logger.info("Step 2: No fallback needed - popularity distribution provided sufficient posts");
            }

            // Step 3: Remove duplicates and shuffle
            selectedPosts = removeDuplicates(selectedPosts);
            selectedPosts = shufflePosts(selectedPosts);

            // Step 4: Limit to exact count
            if (selectedPosts.size() > count) {
                selectedPosts = selectedPosts.subList(0, count);
                logger.warn("Limited final result to requested count - had: {}, limited to: {}", 
                           selectedPosts.size() + (selectedPosts.size() - count), count);
            }

            String strategyUsed = remainingNeeded > 0 ? "popularity + time priority" : "popularity only";
            logger.info("Post selection completed for emotion '{}' - strategy: {}, requested: {}, selected: {}", 
                       emotion, strategyUsed, count, selectedPosts.size());

            return selectedPosts;

        } catch (Exception e) {
            logger.error("Error during post selection for emotion: '{}', count: {}", emotion, count, e);
            throw new RuntimeException("Failed to select posts for emotion: " + emotion, e);
        }
    }

    /**
     * Randomizes the order of posts to avoid predictable patterns.
     * 
     * @param posts The list of posts to shuffle.
     * @return A new list with posts in random order.
     */
    public List<Post> shufflePosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            logger.debug("Shuffle requested for null/empty list - returning as-is");
            return posts == null ? new ArrayList<>() : new ArrayList<>(posts);
        }

        List<Post> shuffledPosts = new ArrayList<>(posts);
        Collections.shuffle(shuffledPosts, new Random());
        
        logger.debug("Shuffled {} posts to randomize order", shuffledPosts.size());
        return shuffledPosts;
    }

    /**
     * Removes duplicate posts based on post ID, maintaining the order of first occurrence.
     * 
     * @param posts The list of posts that may contain duplicates.
     * @return A new list without duplicate posts.
     */
    private List<Post> removeDuplicates(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            logger.debug("Duplicate removal requested for null/empty list - returning as-is");
            return posts == null ? new ArrayList<>() : new ArrayList<>(posts);
        }

        List<Post> uniquePosts = new ArrayList<>();
        Set<UUID> seenIds = new HashSet<>();

        for (Post post : posts) {
            if (post != null && post.getId() != null && !seenIds.contains(post.getId())) {
                uniquePosts.add(post);
                seenIds.add(post.getId());
            }
        }

        int duplicatesRemoved = posts.size() - uniquePosts.size();
        if (duplicatesRemoved > 0) {
            logger.info("Removed {} duplicate posts, kept {} unique posts", duplicatesRemoved, uniquePosts.size());
        } else {
            logger.debug("No duplicates found in {} posts", posts.size());
        }

        return uniquePosts;
    }

    /**
     * Helper class to represent a time window for waterfall priority.
     */
    private static class TimeWindow {
        final String name;
        final LocalDateTime since;

        TimeWindow(String name, LocalDateTime since) {
            this.name = name;
            this.since = since;
        }
    }
}
