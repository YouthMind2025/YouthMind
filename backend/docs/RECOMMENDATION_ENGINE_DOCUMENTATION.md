# YouthMind Recommendation Engine Documentation

**Part 1: Executive Summary & System Architecture**

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Architecture](#2-system-architecture)
3. [Core Concepts](#3-core-concepts)
4. [How It Works - Technical Deep Dive](#4-how-it-works---technical-deep-dive)
5. [API Endpoints](#5-api-endpoints)
6. [Frontend Integration Guide](#6-frontend-integration-guide)
7. [Configuration](#7-configuration)
8. [Code Examples](#8-code-examples)
9. [Data Models](#9-data-models)
10. [Error Handling](#10-error-handling)
11. [Performance Considerations](#11-performance-considerations)
12. [Testing](#12-testing)
13. [Troubleshooting](#13-troubleshooting)
14. [Future Enhancements](#14-future-enhancements)

---

## 1. Executive Summary

### What is the Recommendation Engine?

The YouthMind Recommendation Engine is an emotion-based content personalization system that dynamically curates social media feed content based on user emotional preferences. The system tracks user interactions (posting, liking, viewing) and maintains a 6-dimensional emotion vector (R vector) that represents the user's emotional engagement patterns.

### Problem Statement

Traditional social media feeds often suffer from:
- **Generic content delivery**: One-size-fits-all approach that doesn't adapt to individual user preferences
- **Engagement optimization without emotional awareness**: Algorithms that maximize clicks but ignore emotional resonance
- **Echo chamber effects**: Systems that reinforce existing patterns without diversity
- **Latency issues**: Real-time computation during feed refresh causing noticeable delays

The YouthMind Recommendation Engine addresses these challenges by:
- **Personalized emotion-based recommendations**: Content matched to user's emotional state
- **Real-time learning**: Immediate updates to user preferences based on behavior
- **Balanced diversity**: 40% non-matching content to prevent echo chambers
- **Pre-computation strategy**: Blocks prepared in advance to eliminate refresh latency

### Key Features

1. **6-Dimensional Emotion Vector (R Vector)**
   - Tracks: `[sad, fear, angry, happy, conflicted, surprised]`
   - Integer-based weights: `[2, 40]` range
   - Real-time updates after each user interaction

2. **Behavioral Weighting System**
   - **Posting**: +6 weight (highest engagement indicator)
   - **Liking**: +4 weight (moderate engagement)
   - **Viewing (≥15s)**: +2 weight (passive engagement)

3. **Anti-Explosion Mechanism**
   - Automatic scaling when any dimension exceeds 40
   - Value-2 protection to maintain minimum thresholds
   - Standard mathematical rounding

4. **Top 3 Emotion Selection**
   - Extracts three highest-valued dimensions
   - Fixed tie-breaking priority: `conflicted > surprised > angry > sad > fear > happy`

5. **20 Predefined Mapping Rules**
   - Maps top 3 emotion combinations to content distribution percentages
   - Ensures balanced emotional representation (60% matching, 40% diverse)

6. **Intelligent Post Retrieval**
   - Time-based priority: 0-12h → 12-24h → 24-48h → 2-7d
   - Popularity stratification: Hot (≥100 views, 40%), Normal (50-99 views, 30%), Cold (<50 views, 30%)
   - Cascading fallback mechanisms

7. **Block Refresh Mechanism**
   - Block N uses R vector version (N-2) for two-round lag
   - Default configuration for blocks 1-2
   - Pre-computation of Block N+1 while viewing Block N

### Benefits

- **Personalized Experience**: Content tailored to individual emotional preferences
- **Real-time Adaptation**: System learns and adapts immediately from user behavior
- **Emotional Balance**: Prevents echo chambers with 40% diverse content
- **Zero-Latency Refresh**: Pre-computed blocks enable instant feed updates
- **Scalable Architecture**: Event-driven design handles high throughput
- **Robust Fallbacks**: Multiple fallback mechanisms ensure content availability

---

## 2. System Architecture

### 2.1 Overall System Design

```
┌─────────────────────────────────────────────────────────────────────┐
│                        User Interaction Layer                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │  Create  │  │   Like   │  │   View   │  │  Scroll  │            │
│  │   Post   │  │   Post   │  │   Post   │  │   Feed   │            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
│       │             │             │             │                   │
└───────┼─────────────┼─────────────┼─────────────┼───────────────────┘
        │             │             │             │
        ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PostService Integration Layer                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  • Extract post emotion                                       │  │
│  │  • Validate duration (≥15s for views)                       │  │
│  │  • Call RVectorUpdateService                                 │  │
│  │  • Publish events to Redis Streams                           │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    R Vector Update Service Layer                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  RVectorUpdateService                                         │  │
│  │  • updateOnPost(userId, postId, emotion)     → +6 weight     │  │
│  │  • updateOnLike(userId, postId, emotion)     → +4 weight     │  │
│  │  • updateOnView(userId, postId, emotion, d) → +2 weight    │  │
│  │  • Apply behavioral weights                                  │  │
│  │  • Check halving trigger (>40)                               │  │
│  │  • Apply value-2 protection                                  │  │
│  │  • Create new version                                        │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Event-Driven Processing Layer                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Redis Streams: "recommendation:rvector-updates"             │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │  RecommendationEventPublisher                          │  │  │
│  │  │  • Serialize events                                    │  │  │
│  │  │  • Publish to Redis Streams                            │  │  │
│  │  │  • Track metrics                                       │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │  RecommendationEventConsumer                           │  │  │
│  │  │  • Subscribe to stream                                │  │  │
│  │  │  • Deserialize events                                  │  │  │
│  │  │  • Route to RVectorUpdateService                       │  │  │
│  │  │  • Handle errors gracefully                            │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Data Persistence Layer                            │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL Database                                          │  │
│  │  • user_recommendation_vectors (R vector versions)           │  │
│  │  • recommendation_mapping_rules (20 predefined rules)        │  │
│  │  • feed_block_cache (pre-computed blocks)                    │  │
│  │  • user_feed_state (block tracking)                          │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Feed Generation Layer                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  FeedBlockGeneratorService                                    │  │
│  │  • Get R vector for block (R(N-2) logic)                      │  │
│  │  • Extract top 3 emotions                                    │  │
│  │  • Get emotion distribution from mapping rules                │  │
│  │  • Calculate post counts per emotion                         │  │
│  │  • Assemble block with PostSelectionService                  │  │
│  │  • Pre-compute Block N+1 asynchronously                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  PostSelectionService                                         │  │
│  │  • Time-based priority (0-12h, 12-24h, 24-48h, 2-7d)       │  │
│  │  • Popularity stratification (Hot/Normal/Cold)               │  │
│  │  • Cascading fallback mechanisms                             │  │
│  │  • Anti-clustering logic                                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  RecommendationMappingService                                │  │
│  │  • Map top 3 emotions to distribution percentages             │  │
│  │  • Retrieve from 20 predefined rules                        │  │
│  │  • Fallback to default distribution                          │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Content Delivery Layer                        │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  RecommendationController                                     │  │
│  │  • GET /v1/recommendation/feed/block/{blockNumber}          │  │
│  │  • Return pre-computed or generate on-demand                 │  │
│  │  • Trigger pre-computation of next block                     │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Component Relationships

#### Core Services

1. **RVectorUpdateService**
   - **Purpose**: Updates user R vectors based on behavioral interactions
   - **Dependencies**: `UserRecommendationVectorRepository`
   - **Key Methods**:
     - `updateOnPost(userId, postId, emotion)` - +6 weight
     - `updateOnLike(userId, postId, emotion)` - +4 weight
     - `updateOnView(userId, postId, emotion, duration)` - +2 weight (if ≥15s)
     - `updateEmotionWeight(userId, emotion, weight)` - Core update logic
     - `initializeForNewUser(userId)` - Initialize with all emotions = 10

2. **FeedBlockGeneratorService**
   - **Purpose**: Generates personalized feed blocks for users
   - **Dependencies**: 
     - `UserRecommendationVectorRepository`
     - `RecommendationMappingService`
     - `PostSelectionService`
     - `FeedBlockCacheRepository`
   - **Key Methods**:
     - `generateBlockForUser(userId, blockNumber, rVector)` - Main generation logic
     - `getOrGenerateBlock(userId, blockNumber)` - Get cached or generate
     - `precomputeNextBlock(userId, nextBlockNumber)` - Async pre-computation
     - `getRVectorForBlock(userId, blockNumber)` - R(N-2) logic

3. **PostSelectionService**
   - **Purpose**: Intelligent post selection with time and popularity prioritization
   - **Dependencies**: `PostRepository`, `RecommendationMappingService`
   - **Key Methods**:
     - `selectWithPopularityDistribution(emotion, count, cutoffTime, excludePostIds)` - Main selection
     - Time window cascading: 0-12h → 12-24h → 24-48h → 2-7d
     - Popularity tier distribution: Hot (40%), Normal (30%), Cold (30%)

4. **RecommendationMappingService**
   - **Purpose**: Maps top 3 emotions to content distribution percentages
   - **Dependencies**: `RecommendationMappingRuleRepository`
   - **Key Methods**:
     - `getDistributionForEmotions(topThree)` - Get percentages for emotion combination
     - `findRuleByEmotionCombination(combination)` - Lookup mapping rule
     - `getDefaultDistribution()` - Fallback distribution

5. **RecommendationEventPublisher**
   - **Purpose**: Publishes user interaction events to Redis Streams
   - **Dependencies**: `RedisTemplate`, `ObjectMapper`, `MeterRegistry`
   - **Key Methods**:
     - `publishPostCreatedEvent(userId, postId, emotion)`
     - `publishPostLikedEvent(userId, postId, emotion)`
     - `publishPostViewedEvent(userId, postId, emotion, duration)`

6. **RecommendationEventConsumer**
   - **Purpose**: Consumes events from Redis Streams and triggers R vector updates
   - **Dependencies**: 
     - `StreamMessageListenerContainer`
     - `RVectorUpdateService`
     - `RedisTemplate`, `ObjectMapper`
   - **Key Methods**:
     - `onMessage(MapRecord message)` - Process incoming events
     - `handlePostCreated(PostCreatedEvent)` - Route to update service
     - `handlePostLiked(PostLikedEvent)` - Route to update service
     - `handlePostViewed(PostViewedEvent)` - Route to update service

#### Supporting Services

- **PostService**: Main service handling post creation, liking, and viewing
- **UserRecommendationVectorRepository**: Data access for R vectors
- **FeedBlockCacheRepository**: Data access for cached feed blocks
- **UserFeedState**: Tracks user's current block number and shown posts

### 2.3 Data Flow

#### Flow 1: User Creates a Post

```
1. User creates post via PostService.createPost()
   ↓
2. Post is saved with emotion tag (from AI classification)
   ↓
3. PostService calls RVectorUpdateService.updateOnPost(userId, postId, emotion)
   ↓
4. RVectorUpdateService:
   - Gets current R vector (or creates new with all = 10)
   - Creates new version
   - Adds +6 to emotion dimension
   - Clamps to [2, 40]
   - Checks halving trigger (>40)
   - Applies halving if needed (with value-2 protection)
   - Saves new version
   ↓
5. PostService publishes PostCreatedEvent to Redis Streams (async)
   ↓
6. RecommendationEventConsumer processes event (async, optional)
   ↓
7. R vector updated in database
```

#### Flow 2: User Likes a Post

```
1. User likes post via PostService.toggleResonance()
   ↓
2. PostService calls RVectorUpdateService.updateOnLike(userId, postId, emotion)
   ↓
3. RVectorUpdateService:
   - Gets current R vector
   - Creates new version
   - Adds +4 to emotion dimension
   - Clamps, checks halving, applies if needed
   - Saves new version
   ↓
4. PostService publishes PostLikedEvent to Redis Streams (async)
   ↓
5. R vector updated in database
```

#### Flow 3: User Views Post (≥15 seconds)

```
1. User views post via PostService.trackPostViewWithDuration()
   ↓
2. PostService checks duration >= 15 seconds
   ↓
3. If true, calls RVectorUpdateService.updateOnView(userId, postId, emotion, duration)
   ↓
4. RVectorUpdateService:
   - Gets current R vector
   - Creates new version
   - Adds +2 to emotion dimension
   - Clamps, checks halving, applies if needed
   - Saves new version
   ↓
5. PostService publishes PostViewedEvent to Redis Streams (async)
   ↓
6. R vector updated in database
```

#### Flow 4: Feed Block Generation

```
1. User requests feed block via RecommendationController
   ↓
2. FeedBlockGeneratorService.getOrGenerateBlock(userId, blockNumber)
   ↓
3. Check for pre-computed block in cache
   - If found and valid: return immediately, trigger pre-computation of Block N+1
   - If not found: continue to generation
   ↓
4. Get R vector for block using R(N-2) logic:
   - Block 1-2: Use default distribution
   - Block N: Use R vector version (N-2)
   ↓
5. Extract top 3 emotions from R vector
   ↓
6. RecommendationMappingService.getDistributionForEmotions(topThree)
   - Lookup mapping rule for emotion combination
   - Return distribution percentages (or default)
   ↓
7. Calculate post counts per emotion from percentages
   ↓
8. PostSelectionService.selectWithPopularityDistribution() for each emotion:
   - Time window cascading: 0-12h → 12-24h → 24-48h → 2-7d
   - Popularity stratification: Hot (40%), Normal (30%), Cold (30%)
   - Cascading fallback: hot → normal → cold
   ↓
9. Assemble all posts, apply anti-clustering shuffle
   ↓
10. Create FeedBlockCache and save
    ↓
11. Return block to user
    ↓
12. Trigger async pre-computation of Block N+1
```

### 2.4 Technology Stack

#### Backend Framework
- **Spring Boot 3.x**: Main application framework
- **Spring Data JPA**: Database access layer
- **Spring Data Redis**: Redis integration for streams and caching
- **Spring Security**: Authentication and authorization

#### Database
- **PostgreSQL 14+**: Primary data store
  - `user_recommendation_vectors`: R vector versions
  - `recommendation_mapping_rules`: 20 predefined mapping rules
  - `feed_block_cache`: Pre-computed feed blocks
  - `user_feed_state`: User feed state tracking
  - `posts`: Post data with emotion tags

#### Caching & Event Streaming
- **Redis**: 
  - Streams for event-driven architecture
  - Caching for feed blocks and R vectors
  - Pub/Sub for real-time updates

#### Monitoring & Metrics
- **Micrometer**: Application metrics
- **SLF4J + Logback**: Structured logging
- **MDC (Mapped Diagnostic Context)**: Request correlation

#### Build & Dependency Management
- **Maven**: Build tool
- **Lombok**: Boilerplate code reduction
- **Jackson**: JSON serialization/deserialization

### 2.5 Key Architectural Patterns

1. **Event-Driven Architecture**
   - Redis Streams for asynchronous event processing
   - Decoupled services via event publishing
   - Scalable event consumption with consumer groups

2. **Versioned Data Model**
   - R vectors maintain version history
   - Block N uses R vector version (N-2)
   - Enables pre-computation and lag-based personalization

3. **Pre-computation Strategy**
   - Blocks computed in advance during user viewing
   - Eliminates refresh latency
   - Async processing with `@Async` and thread pools

4. **Cascading Fallback Mechanisms**
   - Time windows: 12h → 24h → 48h → 7d
   - Popularity tiers: Hot → Normal → Cold
   - Default distribution when R vector unavailable

5. **Service Layer Separation**
   - Clear separation of concerns
   - Single responsibility principle
   - Dependency injection for testability

6. **Transactional Safety**
   - `@Transactional` annotations for data consistency
   - Version management prevents race conditions
   - Error handling with graceful degradation

---

## 3. Core Concepts

### 3.1 Emotion Vector (R Vector)

#### What is an R Vector?

The **R Vector** (Recommendation Vector) is a 6-dimensional integer-based vector that tracks a user's emotional engagement patterns. Each dimension represents one of six basic emotions, and the weight values indicate the user's preference strength for content matching that emotion.

#### 6 Emotion Dimensions

The R vector tracks six emotions:
- **sad**: Melancholy, sorrow, grief
- **fear**: Anxiety, worry, apprehension
- **angry**: Frustration, irritation, rage
- **happy**: Joy, contentment, elation
- **conflicted**: Mixed feelings, uncertainty, ambivalence
- **surprised**: Shock, astonishment, unexpectedness

#### Value Range: [2, 40]

All emotion weights operate within a constrained integer range:
- **Minimum**: 2 (baseline presence of emotion)
- **Maximum**: 40 (prevents numerical explosion)
- **Default**: 10 (neutral baseline for new users)

#### Initialization

When a new user is created, all six emotion dimensions are initialized to **10**, representing a neutral baseline state. This allows the system to learn user preferences through interactions without bias.

#### Versioning System

R vectors maintain a version history to support the R(N-2) lag mechanism:
- Each update creates a **new version** (incremented version number)
- Previous versions are preserved in the database
- Block N uses R vector version (N-2) for personalization
- This enables pre-computation and lag-based recommendations

**Code Extract: UserRecommendationVector Entity**

```java
// backend/src/main/java/ai/youthmind/entity/UserRecommendationVector.java

@Entity
@Table(name = "user_recommendation_vectors")
public class UserRecommendationVector {
    
    @Column(name = "version", nullable = false)
    @NotNull
    @Min(value = 1, message = "Version must be at least 1")
    private Integer version;
    
    // Emotion weights (2 to 40) - integer scale matching specification
    @Column(name = "sad_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Sad weight must be at least 2")
    @Max(value = 40, message = "Sad weight must be at most 40")
    private Integer sadWeight = 10;
    
    @Column(name = "fear_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Fear weight must be at least 2")
    @Max(value = 40, message = "Fear weight must be at most 40")
    private Integer fearWeight = 10;
    
    // ... similar for angryWeight, happyWeight, conflictedWeight, surprisedWeight
    
    /**
     * Get emotion weight by emotion name
     */
    public Integer getEmotionWeight(String emotion) {
        if (emotion == null) return 0;
        
        return switch (emotion.toLowerCase()) {
            case "sad" -> sadWeight;
            case "fear" -> fearWeight;
            case "angry" -> angryWeight;
            case "happy" -> happyWeight;
            case "conflicted" -> conflictedWeight;
            case "surprised" -> surprisedWeight;
            default -> 0;
        };
    }
    
    /**
     * Set emotion weight by emotion name
     */
    public void setEmotionWeight(String emotion, Integer weight) {
        if (emotion == null || weight == null) return;
        
        // Clamp value to [2, 40] range
        int clampedWeight = Math.max(2, Math.min(40, weight));
        
        switch (emotion.toLowerCase()) {
            case "sad" -> sadWeight = clampedWeight;
            case "fear" -> fearWeight = clampedWeight;
            case "angry" -> angryWeight = clampedWeight;
            case "happy" -> happyWeight = clampedWeight;
            case "conflicted" -> conflictedWeight = clampedWeight;
            case "surprised" -> surprisedWeight = clampedWeight;
            default -> {
                // Do nothing for unknown emotions
            }
        }
    }
}
```

---

### 3.2 Behavioral Weights

#### Weight Assignment

The system assigns different weights to three distinct user behaviors based on engagement level:

| Behavior | Weight | Rationale |
|----------|--------|-----------|
| **Posting** | +6 | Highest engagement; indicates strong emotional connection |
| **Liking** | +4 | Moderate engagement; shows approval/resonance |
| **Viewing (≥15 seconds)** | +2 | Passive engagement; indicates interest |

#### How Weights Are Applied

1. **Post Creation**: When a user creates a post, the post's emotion tag receives +6 weight
2. **Post Like**: When a user likes a post, the post's emotion tag receives +4 weight
3. **Post View**: When a user views a post for ≥15 seconds, the post's emotion tag receives +2 weight

The weight is **added** to the current emotion dimension value, then clamped to the [2, 40] range. If any dimension exceeds 40 after the update, the anti-explosion mechanism (halving) is triggered.

**Code Extract: RVectorUpdateService - Behavioral Weight Constants**

```java
// backend/src/main/java/ai/youthmind/service/RVectorUpdateService.java

@Service
@Transactional
public class RVectorUpdateService {
    
    // Behavioral weight constants matching specification
    public static final int WEIGHT_POSTING = 6;
    public static final int WEIGHT_LIKING = 4;
    public static final int WEIGHT_VIEWING = 2;
    
    // R vector constraints
    private static final int MIN_WEIGHT = 2;
    private static final int MAX_WEIGHT = 40;
    private static final int DEFAULT_WEIGHT = 10;
    
    /**
     * Update R vector when user creates a post
     * Applies weight +6 to the post's emotion
     */
    public UserRecommendationVector updateOnPost(UUID userId, UUID postId, Emotion emotion) {
        log.info("Updating R vector on post creation - user: {}, post: {}, emotion: {}, weight: +{}", 
                 userId, postId, emotion, WEIGHT_POSTING);
        return updateEmotionWeight(userId, emotion, WEIGHT_POSTING);
    }
    
    /**
     * Update R vector when user likes a post
     * Applies weight +4 to the post's emotion
     */
    public UserRecommendationVector updateOnLike(UUID userId, UUID postId, Emotion emotion) {
        log.info("Updating R vector on post like - user: {}, post: {}, emotion: {}, weight: +{}", 
                 userId, postId, emotion, WEIGHT_LIKING);
        return updateEmotionWeight(userId, emotion, WEIGHT_LIKING);
    }
    
    /**
     * Update R vector when user views a post for ≥15 seconds
     * Applies weight +2 to the post's emotion
     */
    public UserRecommendationVector updateOnView(UUID userId, UUID postId, Emotion emotion, Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds < 15) {
            log.debug("Skipping R vector update - duration {}s < 15 seconds threshold", durationSeconds);
            return null;
        }
        
        log.info("Updating R vector on post view - user: {}, post: {}, emotion: {}, duration: {}s, weight: +{}", 
                 userId, postId, emotion, durationSeconds, WEIGHT_VIEWING);
        return updateEmotionWeight(userId, emotion, WEIGHT_VIEWING);
    }
    
    /**
     * Core method to update emotion weight in R vector
     */
    public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
        // Step 1: Get current R vector or create if doesn't exist
        UserRecommendationVector currentVector = getOrCreateCurrentVector(userId);
        
        // Step 2: Create new version
        Integer newVersion = currentVector.getVersion() + 1;
        UserRecommendationVector newVector = createNewVersion(currentVector, newVersion);
        
        // Step 3: Add weight to the emotion dimension
        Integer currentEmotionWeight = newVector.getEmotionWeight(emotion.toLowerCase());
        Integer newEmotionWeight = currentEmotionWeight + weight;
        
        // Step 4: Clamp to [2, 40] range
        newEmotionWeight = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, newEmotionWeight));
        newVector.setEmotionWeight(emotion.toLowerCase(), newEmotionWeight);
        
        // Step 5: Check if halving needed (any dimension > 40)
        if (newVector.needsHalving()) {
            // Step 6: Apply halving with value-2 protection
            newVector.halveWeights();
        }
        
        // Step 7: Save new version
        return vectorRepository.save(newVector);
    }
}
```

---

### 3.3 Block Generation

#### What is a Feed Block?

A **feed block** is a complete set of posts displayed from the initial screen to the bottom, before requiring a refresh. A typical block contains approximately 10-20+ posts, personalized based on the user's R vector.

#### R(N-2) Lag Mechanism

The system implements a **two-round lag** between R vector updates and content recommendations:

- **Block N uses R vector version (N-2)**
- This means Block 3 uses R vector version 1, Block 4 uses R vector version 2, etc.
- **Blocks 1-2** use a default distribution (not based on R vector)

**Rationale**: This lag allows the system to:
- Pre-compute blocks in advance
- Use stable R vector versions that won't change during block viewing
- Eliminate refresh latency through pre-computation

#### Pre-computation Strategy

**Block N+1 is computed while the user is viewing Block N**:
- When Block N is delivered, the system triggers async pre-computation of Block N+1
- Pre-computation uses R vector version ((N+1)-2) = (N-1)
- The pre-computed block is cached and marked as `isConsumed = false`
- When the user requests Block N+1, it's returned instantly from cache

#### Default Distribution for Blocks 1-2

For new users or blocks 1-2, the system uses a fixed default distribution:
- **sad**: 20%
- **fear**: 5%
- **angry**: 20%
- **happy**: 20%
- **conflicted**: 20%
- **surprised**: 15%

This provides balanced initial exposure across emotions with slightly reduced fear content.

**Code Extract: FeedBlockGeneratorService - R(N-2) Logic**

```java
// backend/src/main/java/ai/youthmind/service/FeedBlockGeneratorService.java

/**
 * Get the R vector for a specific block using R(N-2) lag mechanism
 * Block N uses R vector version (N-2)
 * Blocks 1-2 use default distribution (returns null)
 * 
 * @param userId The user ID
 * @param blockNumber The block number
 * @return UserRecommendationVector for block (N-2), or null if blockNumber <= 2 or R vector not found
 */
private UserRecommendationVector getRVectorForBlock(UUID userId, Integer blockNumber) {
    // Blocks 1-2 use default distribution
    if (blockNumber <= 2) {
        logger.debug("Block {} <= 2, using default distribution (no R vector)", blockNumber);
        return null;
    }
    
    // Calculate target R version: blockNumber - 2
    Integer targetRVersion = blockNumber - 2;
    logger.debug("Block {} requires R vector version {} (N-2)", blockNumber, targetRVersion);
    
    try {
        // Look up R vector version (blockNumber - 2) from repository
        Optional<UserRecommendationVector> rVectorOpt = userRecommendationVectorRepository
            .findByUserIdAndVersion(userId, targetRVersion);
        
        if (rVectorOpt.isPresent()) {
            UserRecommendationVector rVector = rVectorOpt.get();
            logger.info("Found R vector version {} for user {} block {} (N-2)", 
                       targetRVersion, userId, blockNumber);
            return rVector;
        } else {
            logger.info("R vector version {} not found for user {} block {} (N-2), using default", 
                       targetRVersion, userId, blockNumber);
            return null;
        }
    } catch (Exception e) {
        logger.error("Error retrieving R vector for user {} block {} (N-2)", userId, blockNumber, e);
        return null;
    }
}

/**
 * Pre-compute Block N+1 in the background while user is viewing Block N
 * This method runs asynchronously and does not block the current request
 */
@Async("blockPrecomputationExecutor")
public CompletableFuture<Void> precomputeNextBlock(UUID userId, Integer nextBlockNumber) {
    logger.info("Starting pre-computation of block {} for user {}", nextBlockNumber, userId);
    
    try {
        // Step 1: Check if Block N+1 already exists (don't recompute)
        Optional<FeedBlockCache> existingBlock = feedBlockCacheRepository
            .findByUserIdAndBlockNumber(userId, nextBlockNumber);
        
        if (existingBlock.isPresent() && !existingBlock.get().isExpired()) {
            logger.info("Block {} for user {} already exists and is valid, skipping pre-computation", 
                       nextBlockNumber, userId);
            return CompletableFuture.completedFuture(null);
        }
        
        // Step 2: Get R vector for Block N+1 using R(N-1) logic
        // Block N+1 uses R vector version ((N+1)-2) = (N-1)
        UserRecommendationVector rVector = getRVectorForBlock(userId, nextBlockNumber);
        
        if (rVector == null) {
            logger.info("Pre-computing block {} for user {} with default distribution (R(N-1) not available)", 
                       nextBlockNumber, userId);
        } else {
            logger.info("Pre-computing block {} for user {} with R vector version {} (N-1)", 
                       nextBlockNumber, userId, rVector.getVersion());
        }
        
        // Step 3: Generate Block N+1 asynchronously
        FeedBlockCache preComputedBlock = generateBlockForUser(userId, nextBlockNumber, rVector);
        
        // Step 4: Save to cache with isConsumed = false (pre-computed, not yet consumed)
        if (preComputedBlock.wasConsumed()) {
            preComputedBlock.setIsConsumed(false);
            preComputedBlock.setConsumedAt(null);
            feedBlockCacheRepository.save(preComputedBlock);
        }
        
        logger.info("Successfully pre-computed block {} for user {} with {} posts (isConsumed=false)", 
                   nextBlockNumber, userId, preComputedBlock.getPostCount());
        
        return CompletableFuture.completedFuture(null);
        
    } catch (Exception e) {
        logger.error("Failed to pre-compute block {} for user {}", nextBlockNumber, userId, e);
        return CompletableFuture.completedFuture(null); // Don't fail the request
    }
}
```

---

### 3.4 Post Selection

#### Time-Based Priority

The system uses a **hierarchical time window** approach to prioritize fresh content:

1. **0-12 hours** (Primary): Highest priority, most recent content
2. **12-24 hours** (Secondary): If primary window insufficient
3. **24-48 hours** (Tertiary): If secondary window insufficient
4. **2-7 days** (Final fallback): If tertiary window insufficient

The system cascades through these windows until enough posts are found, ensuring content freshness while maintaining availability.

#### Popularity Tiers

Posts are stratified into three popularity tiers based on view count:

| Tier | View Count | Target Proportion |
|------|------------|-------------------|
| **Hot** | ≥100 views | 40% |
| **Normal** | 50-99 views | 30% |
| **Cold** | <50 views | 30% |

**Cascading Fallback**: If insufficient posts in a tier, the system replaces them with posts from other tiers:
- Insufficient hot posts → replace with normal posts
- Insufficient normal posts → replace with cold posts

#### Emotion-Based Filtering

Posts are filtered by emotion tag to match the distribution percentages from the recommendation mapping rules. Each emotion category is filled independently using the time-based priority and popularity stratification.

**Code Extract: PostSelectionService - Time Priority and Popularity Tiers**

```java
// backend/src/main/java/ai/youthmind/service/PostSelectionService.java

@Service
@Transactional
public class PostSelectionService {
    
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
     */
    public List<Post> selectWithPopularityDistribution(String emotion, Integer count, 
                                                      LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
        // Calculate distribution counts
        int hotCount = (int) (count * HOT_PERCENTAGE);
        int normalCount = (int) (count * NORMAL_PERCENTAGE);
        int coldCount = count - hotCount - normalCount;
        
        logger.info("Popularity distribution - Hot: {}, Normal: {}, Cold: {} (Total: {})", 
                   hotCount, normalCount, coldCount, count);
        
        List<Post> selectedPosts = new ArrayList<>();
        
        // Select from each popularity tier
        List<Post> hotPosts = selectHotPosts(normalizedEmotion, hotCount, cutoffTime, excludePostIds);
        List<Post> normalPosts = selectNormalPosts(normalizedEmotion, normalCount, cutoffTime, excludePostIds);
        List<Post> coldPosts = selectColdPosts(normalizedEmotion, coldCount, cutoffTime, excludePostIds);
        
        // Combine results
        selectedPosts.addAll(hotPosts);
        selectedPosts.addAll(normalPosts);
        selectedPosts.addAll(coldPosts);
        
        // Handle insufficient posts in tiers with cascading fallback
        if (selectedPosts.size() < count) {
            int remainingNeeded = count - selectedPosts.size();
            selectedPosts = compensateForShortfall(selectedPosts, normalizedEmotion, 
                                                  remainingNeeded, cutoffTime, excludePostIds);
        }
        
        return selectedPosts;
    }
    
    /**
     * Select hot posts (viewCount >= 100)
     */
    private List<Post> selectHotPosts(String emotion, int count, LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
        // Query for hot posts: view >= 100, emotion match, after cutoff, not flagged
        Page<Post> hotPostsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
            emotion, cutoffTime, LocalDateTime.now(), pageable);
        
        return hotPostsPage.getContent().stream()
            .filter(post -> !excludePostIds.contains(post.getId()))
            .filter(post -> !post.isFlagged())
            .filter(post -> post.getViewCount() >= HOT_VIEW_THRESHOLD)
            .collect(Collectors.toList());
    }
    
    /**
     * Applies waterfall time priority to select posts for a specific emotion.
     * Tries time windows in order: 0-12h (primary) → 12-24h → 24-48h → 2-7d until enough posts are found.
     */
    public List<Post> applyTimePriority(String emotion, Integer count, Set<UUID> excludeIds) {
        List<Post> selectedPosts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Define time windows in waterfall order: 0-12h (primary) → 12-24h → 24-48h → 2-7d
        List<TimeWindow> timeWindows = Arrays.asList(
            new TimeWindow("0-12 hours", now.minusHours(12)),
            new TimeWindow("12-24 hours", now.minusHours(24)),
            new TimeWindow("24-48 hours", now.minusHours(48)),
            new TimeWindow("2-7 days", now.minusDays(7))
        );
        
        for (TimeWindow window : timeWindows) {
            int remainingNeeded = count - selectedPosts.size();
            if (remainingNeeded <= 0) {
                break; // We have enough posts
            }
            
            // Query posts in this time window
            List<Post> windowPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
                emotion, window.getStartTime(), now, pageable)
                .getContent()
                .stream()
                .filter(post -> !excludeIds.contains(post.getId()))
                .filter(post -> !post.isFlagged())
                .limit(remainingNeeded)
                .collect(Collectors.toList());
            
            selectedPosts.addAll(windowPosts);
            
            if (selectedPosts.size() >= count) {
                break; // We have enough posts
            }
        }
        
        return selectedPosts;
    }
}
```

---

## 4. How It Works - Technical Deep Dive

### 4.1 User Interaction Flow

#### Step-by-Step: User Creates a Post

When a user creates a post, the following sequence occurs:

1. **Post Creation Request**
   - User submits post content via `PostService.createPost()`
   - Post is saved to database with initial metadata

2. **AI Emotion Classification**
   - System calls AI service to classify post emotion
   - Emotion tag is assigned (sad, fear, angry, happy, conflicted, surprised)
   - Post is updated with emotion classification data

3. **R Vector Update**
   - System extracts emotion from saved post
   - Converts emotion string to `Emotion` enum
   - Calls `rVectorUpdateService.updateOnPost(userId, postId, emotion)`
   - R vector receives +6 weight on the emotion dimension

4. **Event Publishing (Async)**
   - `PostCreatedEvent` is published to Redis Streams
   - Event contains: userId, postId, emotion, timestamp
   - Publishing happens asynchronously (doesn't block post creation)

5. **Error Handling**
   - If R vector update fails, error is logged but post creation succeeds
   - If emotion classification fails, fallback emotion ("neutral") is used

**Code Extract: PostService - Post Creation Integration**

```java
// backend/src/main/java/ai/youthmind/service/PostService.java

@Transactional
public Post createPost(UUID userId, PostCreateRequest request) {
    // ... (post creation and saving)
    
    // AI emotion classification
    try {
        // Perform AI emotion classification
        savedPost = aiPostAnalysisService.classifyPostEmotion(savedPost);
    } catch (Exception e) {
        logger.error("Failed to perform AI emotion classification", e);
        // Set fallback emotion
        savedPost.setEmotion("neutral");
    }
    
    // Update R vector based on post emotion
    try {
        String emotionStr = savedPost.getEmotion();
        if (emotionStr != null && !emotionStr.trim().isEmpty()) {
            // Convert string emotion to Emotion enum
            ai.youthmind.enums.Emotion emotion = ai.youthmind.enums.Emotion.fromString(emotionStr);
            
            if (emotion != null) {
                logger.info("→ Updating R vector for user: {} with emotion: {} (post: {})", 
                           userId, emotion, savedPost.getId());
                rVectorUpdateService.updateOnPost(userId, savedPost.getId(), emotion);
                logger.info("✓ R vector updated successfully for user: {} with emotion: {}", userId, emotion);
            } else {
                logger.warn("→ Skipping R vector update - invalid emotion string: {}", emotionStr);
            }
        }
    } catch (Exception e) {
        // Log error but don't fail post creation
        logger.error("✗ Failed to update R vector for user: {} - {}", userId, e.getMessage(), e);
    }
    
    return savedPost;
}
```

#### Step-by-Step: User Likes a Post

When a user likes a post, the following sequence occurs:

1. **Like Request**
   - User toggles like via `PostService.toggleResonance()`
   - System checks if user has already liked the post

2. **Like Processing**
   - If not liked: Create `PostResonance` record, increment resonance count
   - If already liked: Remove `PostResonance` record, decrement resonance count

3. **R Vector Update (Only on Like)**
   - **Only updates if it's a like action (not unlike)**
   - System extracts emotion from the post
   - Converts emotion string to `Emotion` enum
   - Calls `rVectorUpdateService.updateOnLike(userId, postId, emotion)`
   - R vector receives +4 weight on the emotion dimension

4. **Event Publishing (Async)**
   - `PostLikedEvent` is published to Redis Streams (only if isLike = true)
   - Event contains: userId, postId, emotion, isLike flag, timestamp

5. **Error Handling**
   - If R vector update fails, error is logged but like operation succeeds

**Code Extract: PostService - Post Like Integration**

```java
// backend/src/main/java/ai/youthmind/service/PostService.java

@Transactional
public PostDto toggleResonance(UUID postId, UUID userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
    
    boolean hasResonanced = postResonanceRepository.existsByPostIdAndUserId(postId, userId);
    
    if (hasResonanced) {
        // Unlike: Remove resonance
        postResonanceRepository.deleteByPostIdAndUserId(postId, userId);
        post.setResonance(post.getResonance() - 1);
    } else {
        // Like: Add resonance
        PostResonance resonance = new PostResonance(userId, postId);
        postResonanceRepository.save(resonance);
        post.setResonance(post.getResonance() + 1);
        
        // Update R vector based on post emotion (only on like, not unlike)
        try {
            String emotionStr = post.getEmotion();
            if (emotionStr != null && !emotionStr.trim().isEmpty()) {
                ai.youthmind.enums.Emotion emotion = ai.youthmind.enums.Emotion.fromString(emotionStr);
                
                if (emotion != null) {
                    logger.info("→ Updating R vector for user: {} with emotion: {} (like, post: {})", 
                               userId, emotion, post.getId());
                    rVectorUpdateService.updateOnLike(userId, post.getId(), emotion);
                    logger.info("✓ R vector updated successfully for user: {} with emotion: {} (like)", 
                               userId, emotion);
                }
            }
        } catch (Exception e) {
            logger.error("✗ Failed to update R vector for user: {} (like) - {}", userId, e.getMessage(), e);
        }
    }
    
    postRepository.save(post);
    return convertToPostDto(post, userId);
}
```

#### Step-by-Step: User Views Post (≥15 seconds)

When a user views a post, the following sequence occurs:

1. **View Tracking Request**
   - User views post via `PostService.trackPostViewWithDuration()`
   - System tracks view with duration in seconds

2. **Duration Validation**
   - System checks if `durationSeconds >= 15`
   - **If duration < 15 seconds**: Skip R vector update, return early
   - **If duration >= 15 seconds**: Proceed with R vector update

3. **R Vector Update**
   - System extracts emotion from the post
   - Converts emotion string to `Emotion` enum
   - Calls `rVectorUpdateService.updateOnView(userId, postId, emotion, durationSeconds)`
   - R vector receives +2 weight on the emotion dimension

4. **Event Publishing (Async)**
   - `PostViewedEvent` is published to Redis Streams (only if duration >= 15s)
   - Event contains: userId, postId, emotion, duration, timestamp

5. **Error Handling**
   - If R vector update fails, error is logged but view tracking succeeds

**Code Extract: PostService - Post View Integration**

```java
// backend/src/main/java/ai/youthmind/service/PostService.java

@Transactional
public PostDto trackPostViewWithDuration(UUID postId, UUID userId, Integer durationSeconds) {
    // Use existing trackPostView logic
    PostDto postDto = trackPostView(postId, userId);
    
    // Check if duration is sufficient for R vector update (>= 15 seconds)
    if (durationSeconds != null && durationSeconds >= 15) {
        try {
            // Get the post to extract emotion
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            
            String emotionStr = post.getEmotion();
            if (emotionStr != null && !emotionStr.trim().isEmpty()) {
                ai.youthmind.enums.Emotion emotion = ai.youthmind.enums.Emotion.fromString(emotionStr);
                
                if (emotion != null) {
                    logger.info("→ Updating R vector for user: {} with emotion: {} (view, {}s, post: {})", 
                               userId, emotion, durationSeconds, postId);
                    rVectorUpdateService.updateOnView(userId, postId, emotion, durationSeconds);
                    logger.info("✓ R vector updated successfully for user: {} with emotion: {}, duration: {}s (view)", 
                               userId, emotion, durationSeconds);
                }
            }
        } catch (Exception e) {
            logger.error("✗ Failed to update R vector for user: {} with duration: {}s (view) - {}", 
                        userId, durationSeconds, e.getMessage(), e);
        }
    } else {
        logger.debug("→ Skipping R vector update - duration {}s < 15 seconds threshold", durationSeconds);
    }
    
    return postDto;
}
```

---

### 4.2 R Vector Update Process

#### How R Vectors Are Updated

The R vector update process follows these steps:

1. **Get Current R Vector**
   - Retrieve current R vector for user from database
   - If no R vector exists, create new one with all emotions = 10, version = 1

2. **Create New Version**
   - Increment version number: `newVersion = currentVersion + 1`
   - Create new `UserRecommendationVector` entity
   - Copy all emotion weights from current vector
   - Set new version number

3. **Add Behavioral Weight**
   - Get current weight for the emotion dimension
   - Add behavioral weight (+6, +4, or +2) to current weight
   - Example: If current `happyWeight = 15` and user posts happy content, new weight = 15 + 6 = 21

4. **Clamp to [2, 40] Range**
   - Ensure new weight is within valid range
   - If weight < 2, set to 2
   - If weight > 40, set to 40

5. **Check Halving Trigger**
   - Check if **any dimension > 40** (not total, but individual dimension)
   - If triggered, proceed to halving mechanism

6. **Apply Halving (If Needed)**
   - Divide all dimensions by 2
   - Apply value-2 protection (dimensions at 2 remain unchanged)
   - Use standard rounding (round half up)

7. **Save New Version**
   - Save new R vector version to database
   - Previous versions are preserved for R(N-2) lookup

**Code Extract: RVectorUpdateService - Core Update Logic**

```java
// backend/src/main/java/ai/youthmind/service/RVectorUpdateService.java

public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
    log.debug("Updating R vector for user {} - emotion: {}, weight: +{}", userId, emotion, weight);
    
    // Step 1: Get current R vector or create if doesn't exist
    UserRecommendationVector currentVector = getOrCreateCurrentVector(userId);
    log.debug("Current R vector for user {} - version: {}, weights: {}", 
             userId, currentVector.getVersion(), currentVector.getAllEmotionWeights());
    
    // Step 2: Create new version
    Integer newVersion = currentVector.getVersion() + 1;
    UserRecommendationVector newVector = createNewVersion(currentVector, newVersion);
    log.debug("Created new R vector version {} for user {}", newVersion, userId);
    
    // Step 3: Add weight to the emotion dimension
    Integer currentEmotionWeight = newVector.getEmotionWeight(emotion.toLowerCase());
    Integer newEmotionWeight = currentEmotionWeight + weight;
    log.debug("Emotion weight update - current: {}, added: {}, new: {}", 
             currentEmotionWeight, weight, newEmotionWeight);
    
    // Step 4: Clamp to [2, 40] range
    newEmotionWeight = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, newEmotionWeight));
    newVector.setEmotionWeight(emotion.toLowerCase(), newEmotionWeight);
    log.debug("Clamped emotion weight to: {}", newEmotionWeight);
    
    // Step 5: Check if halving needed (any dimension > 40)
    if (newVector.needsHalving()) {
        log.info("R vector needs halving for user {} - applying halving with value-2 protection", userId);
        Map<String, Integer> beforeHalving = new HashMap<>(newVector.getAllEmotionWeights());
        
        // Step 6: Apply halving with value-2 protection
        newVector.halveWeights();
        
        Map<String, Integer> afterHalving = newVector.getAllEmotionWeights();
        log.info("R vector halved for user {} - before: {}, after: {}", 
                userId, beforeHalving, afterHalving);
    }
    
    // Step 7: Save new version
    UserRecommendationVector savedVector = vectorRepository.save(newVector);
    log.info("R vector updated successfully for user {} - version: {}, emotion: {}, final weight: {}", 
            userId, savedVector.getVersion(), emotion, savedVector.getEmotionWeight(emotion.toLowerCase()));
    
    return savedVector;
}
```

#### Version Management

Each R vector update creates a new version:

```java
// backend/src/main/java/ai/youthmind/service/RVectorUpdateService.java

private UserRecommendationVector createNewVersion(UserRecommendationVector currentVector, Integer newVersion) {
    UserRecommendationVector newVector = new UserRecommendationVector();
    
    // Copy user ID
    newVector.setUserId(currentVector.getUserId());
    
    // Set new version
    newVector.setVersion(newVersion);
    
    // Copy all emotion weights
    newVector.setSad(currentVector.getSadWeight());
    newVector.setFear(currentVector.getFearWeight());
    newVector.setAngry(currentVector.getAngryWeight());
    newVector.setHappy(currentVector.getHappyWeight());
    newVector.setConflicted(currentVector.getConflictedWeight());
    newVector.setSurprised(currentVector.getSurprisedWeight());
    
    // Timestamps will be set by @PrePersist
    return newVector;
}
```

#### Halving Mechanism

The halving mechanism prevents numerical explosion:

**Trigger Condition**: When **any dimension > 40** (not total)

**Halving Process**:
1. Check each dimension
2. If dimension > 2: Divide by 2, round half up
3. If dimension = 2: Leave unchanged (value-2 protection)

**Code Extract: UserRecommendationVector - Halving Logic**

```java
// backend/src/main/java/ai/youthmind/entity/UserRecommendationVector.java

/**
 * Check if this R vector needs halving
 * Returns true if ANY dimension > 40 (specification requirement)
 */
public boolean needsHalving() {
    return sadWeight > 40 ||
           fearWeight > 40 ||
           angryWeight > 40 ||
           happyWeight > 40 ||
           conflictedWeight > 40 ||
           surprisedWeight > 40;
}

/**
 * Halve all emotion weights with value-2 protection
 * Used when any dimension exceeds 40 to prevent numerical explosion
 * Dimensions at value 2 are protected and remain unchanged
 * Uses standard rounding (round half up)
 */
public void halveWeights() {
    // Halve each weight, but protect values at 2 (minimum threshold)
    // Round half up: 7/2 = 3.5 → 4, 5/2 = 2.5 → 3
    if (sadWeight > 2) {
        sadWeight = (int) Math.round(sadWeight / 2.0);
    }
    if (fearWeight > 2) {
        fearWeight = (int) Math.round(fearWeight / 2.0);
    }
    if (angryWeight > 2) {
        angryWeight = (int) Math.round(angryWeight / 2.0);
    }
    if (happyWeight > 2) {
        happyWeight = (int) Math.round(happyWeight / 2.0);
    }
    if (conflictedWeight > 2) {
        conflictedWeight = (int) Math.round(conflictedWeight / 2.0);
    }
    if (surprisedWeight > 2) {
        surprisedWeight = (int) Math.round(surprisedWeight / 2.0);
    }
}
```

**Example Halving Scenario**:
```
Before halving: R = [sad=40, fear=2, angry=12, happy=14, conflicted=28, surprised=26]
After halving:  R = [sad=20, fear=2, angry=6, happy=7, conflicted=14, surprised=13]
Note: Fear remains at 2 (value-2 protection)
```

---

### 4.3 Event Processing

#### Redis Streams Architecture

The recommendation engine uses Redis Streams for asynchronous event processing:

- **Stream Name**: `"recommendation:rvector-updates"`
- **Consumer Group**: `"recommendation-engine-workers"`
- **Event Types**: `PostCreatedEvent`, `PostLikedEvent`, `PostViewedEvent`

#### Event Flow

1. **Event Publishing**
   - `PostService` publishes events to Redis Streams after R vector updates
   - Events are serialized to JSON and published asynchronously
   - Publishing doesn't block the main operation

2. **Event Consumption**
   - `RecommendationEventConsumer` subscribes to the stream
   - Consumer group ensures load balancing across multiple instances
   - Each message is processed independently

3. **Event Processing**
   - Event is deserialized from JSON
   - Event type is determined
   - Event is routed to appropriate handler
   - Handler calls `RVectorUpdateService` method

4. **Error Handling**
   - Failed events are logged but don't crash the consumer
   - Metrics track processed/failed events
   - Consumer continues processing other events

**Code Extract: RecommendationEventConsumer - Event Processing**

```java
// backend/src/main/java/ai/youthmind/events/consumer/RecommendationEventConsumer.java

/**
 * Handle stream messages from Redis Streams
 */
@Override
public void onMessage(MapRecord<String, String, String> message) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String recordId = message.getId().getValue();
    
    try {
        log.debug("Received stream message - recordId: {}, stream: {}", recordId, message.getStream());
        
        // Extract event data from stream record
        Map<String, String> recordValue = message.getValue();
        String eventDataJson = recordValue.get("eventData");
        String eventType = recordValue.get("eventType");
        
        if (eventDataJson == null || eventDataJson.isEmpty()) {
            log.warn("Received message with empty eventData - recordId: {}", recordId);
            eventsDeserializationErrors.increment();
            return;
        }
        
        // Deserialize event
        UserInteractionEvent event = deserializeEvent(eventDataJson, eventType);
        if (event == null) {
            log.warn("Failed to deserialize event - recordId: {}", recordId);
            eventsDeserializationErrors.increment();
            return;
        }
        
        // Validate event
        if (!event.isValid()) {
            log.warn("Received invalid event - recordId: {}, userId: {}", recordId, event.getUserId());
            eventsFailedTotal.increment();
            return;
        }
        
        // Route event to appropriate handler
        processEvent(event, recordId);
        
        eventsProcessedTotal.increment();
        log.debug("Successfully processed event - recordId: {}, eventType: {}", recordId, eventType);
        
    } catch (Exception e) {
        log.error("Error processing stream message - recordId: {}", recordId, e);
        eventsFailedTotal.increment();
        // Don't rethrow - continue processing other messages
    } finally {
        sample.stop(eventProcessingDuration);
    }
}

/**
 * Route event to appropriate R vector update method
 */
private void processEvent(UserInteractionEvent event, String recordId) {
    try {
        String eventType = event.getEventType();
        UUID userId = event.getUserId();
        
        log.debug("Processing event - type: {}, userId: {}, recordId: {}", eventType, userId, recordId);
        
        switch (eventType) {
            case "POST_CREATED" -> handlePostCreated((PostCreatedEvent) event);
            case "POST_LIKED" -> handlePostLiked((PostLikedEvent) event);
            case "POST_VIEWED" -> handlePostViewed((PostViewedEvent) event);
            default -> {
                log.warn("Unknown event type: {} - recordId: {}", eventType, recordId);
                eventsFailedTotal.increment();
            }
        }
    } catch (Exception e) {
        log.error("Error processing event - recordId: {}", recordId, e);
        eventsFailedTotal.increment();
    }
}

/**
 * Handle PostCreatedEvent - update R vector with +6 weight
 */
private void handlePostCreated(PostCreatedEvent event) {
    UUID userId = event.getUserId();
    UUID postId = event.getPostId();
    ai.youthmind.events.model.Emotion eventEmotion = event.getEmotion();
    
    if (eventEmotion == null) {
        log.warn("PostCreatedEvent has null emotion - userId: {}, postId: {}", userId, postId);
        return;
    }
    
    // Convert event model Emotion to enum Emotion
    Emotion emotionEnum = convertEmotion(eventEmotion);
    
    log.info("Processing PostCreatedEvent - userId: {}, postId: {}, emotion: {}", 
            userId, postId, emotionEnum);
    
    rVectorUpdateService.updateOnPost(userId, postId, emotionEnum);
}

/**
 * Handle PostLikedEvent - update R vector with +4 weight (only if isLike = true)
 */
private void handlePostLiked(PostLikedEvent event) {
    UUID userId = event.getUserId();
    UUID postId = event.getPostId();
    Boolean isLike = event.getIsLike();
    
    // Only process likes, not unlikes
    if (isLike == null || !isLike) {
        log.debug("Skipping PostLikedEvent - isLike is false or null");
        return;
    }
    
    ai.youthmind.events.model.Emotion eventEmotion = event.getEmotion();
    if (eventEmotion == null) {
        log.warn("PostLikedEvent has null emotion - userId: {}, postId: {}", userId, postId);
        return;
    }
    
    Emotion emotionEnum = convertEmotion(eventEmotion);
    rVectorUpdateService.updateOnLike(userId, postId, emotionEnum);
}

/**
 * Handle PostViewedEvent - update R vector with +2 weight (only if duration >= 15s)
 */
private void handlePostViewed(PostViewedEvent event) {
    UUID userId = event.getUserId();
    UUID postId = event.getPostId();
    Integer duration = event.getDuration();
    
    // Check if duration is sufficient
    if (!event.shouldUpdateRVector()) {
        log.debug("Skipping PostViewedEvent - duration {}s < 15s", duration);
        return;
    }
    
    ai.youthmind.events.model.Emotion eventEmotion = event.getEmotion();
    if (eventEmotion == null) {
        log.warn("PostViewedEvent has null emotion - userId: {}, postId: {}", userId, postId);
        return;
    }
    
    Emotion emotionEnum = convertEmotion(eventEmotion);
    rVectorUpdateService.updateOnView(userId, postId, emotionEnum, duration);
}
```

#### Consumer Lifecycle

The consumer starts automatically on application startup:

```java
@PostConstruct
public void start() {
    String streamName = properties.getStreams().getRvectorUpdatesStream();
    String consumerGroup = properties.getStreams().getConsumerGroup();
    String consumerName = "consumer-" + UUID.randomUUID().toString().substring(0, 8);
    
    log.info("Starting RecommendationEventConsumer - stream: {}, group: {}, consumer: {}", 
            streamName, consumerGroup, consumerName);
    
    // Create subscription to the stream
    StreamOffset<String> streamOffset = StreamOffset.create(streamName, ReadOffset.lastConsumed());
    Consumer consumer = Consumer.from(consumerGroup, consumerName);
    
    subscription = container.receive(consumer, streamOffset, this);
    
    isRunning.set(true);
    log.info("✅ RecommendationEventConsumer started successfully");
}
```

---

### 4.4 Feed Block Generation

#### How Blocks Are Generated

The feed block generation process follows these steps:

1. **Check Pre-computed Block**
   - First, check if block is already pre-computed in cache
   - If found and valid: Return immediately, mark as consumed, trigger pre-computation of Block N+1
   - If not found: Continue to generation

2. **Get R Vector for Block (R(N-2) Logic)**
   - Calculate target R version: `targetRVersion = blockNumber - 2`
   - Blocks 1-2: Return null (use default distribution)
   - Block N: Lookup R vector version (N-2) from database
   - If R vector not found: Use default distribution

3. **Extract Top 3 Emotions**
   - If R vector exists: Call `rVector.getTopThreeEmotions()`
   - Returns three emotions sorted by weight (with tie-breaking)
   - If R vector is null: Skip to default distribution

4. **Get Emotion Distribution**
   - Lookup mapping rule for top 3 emotion combination
   - Get distribution percentages for all 6 emotions
   - If no mapping rule found: Use default distribution

5. **Calculate Post Counts**
   - Convert percentages to post counts based on `DEFAULT_POSTS_PER_BLOCK`
   - Example: If happy = 20% and block size = 20, then happy posts = 4

6. **Assemble Block**
   - For each emotion, call `PostSelectionService.selectWithPopularityDistribution()`
   - Combine all selected posts
   - Apply anti-clustering shuffle

7. **Create FeedBlockCache**
   - Create cache entity with post IDs, emotion distribution, R vector version
   - Set expiration time
   - Mark as consumed if returned immediately

8. **Save and Return**
   - Save `FeedBlockCache` to database
   - Update `UserFeedState` (current block number, total blocks)
   - Return block to user

9. **Trigger Pre-computation**
   - Asynchronously pre-compute Block N+1
   - Uses R vector version (N-1) for Block N+1

**Code Extract: FeedBlockGeneratorService - Block Generation**

```java
// backend/src/main/java/ai/youthmind/service/FeedBlockGeneratorService.java

public FeedBlockCache generateBlockForUser(UUID userId, Integer blockNumber, UserRecommendationVector rVector) {
    // Handle null R vector (default distribution for blocks 1-2 or when R vector not found)
    if (rVector == null) {
        logger.info("R vector is null for user {} block {}, using default distribution", userId, blockNumber);
        return generateDefaultBlockForBlockNumber(userId, blockNumber);
    }
    
    try {
        // Step 1: Get top 3 emotions from recommendation vector
        logger.debug("Step 1: Extracting top 3 emotions from R vector");
        List<String> topThreeEmotions = rVector.getTopThreeEmotions();
        logger.info("Top 3 emotions for user {}: {}", userId, topThreeEmotions);
        
        // Step 2: Get emotion distribution percentages
        logger.debug("Step 2: Getting emotion distribution percentages");
        Map<String, Integer> emotionDistribution = recommendationMappingService.getDistributionForEmotions(topThreeEmotions);
        logger.info("Emotion distribution for user {}: {}", userId, emotionDistribution);
        
        // Step 3: Calculate post counts from percentages
        logger.debug("Step 3: Calculating post counts from percentages");
        Map<String, Integer> postCounts = calculatePostCounts(emotionDistribution, DEFAULT_POSTS_PER_BLOCK);
        logger.info("Post counts for user {}: {}", userId, postCounts);
        
        // Step 4: Get or create UserFeedState
        logger.debug("Step 4: Getting or creating UserFeedState");
        UserFeedState userState = getUserFeedState(userId);
        
        // Step 5: Assemble the feed block
        logger.debug("Step 5: Assembling feed block with posts");
        List<Post> assembledPosts = assembleBlock(userId, postCounts, userState);
        logger.info("Assembled {} posts for user {} block {}", assembledPosts.size(), userId, blockNumber);
        
        // Step 6: Create FeedBlockCache
        logger.debug("Step 6: Creating FeedBlockCache");
        FeedBlockCache feedBlockCache = createFeedBlockCache(userId, blockNumber, assembledPosts, 
                                                           emotionDistribution, rVector);
        
        // Step 7: Save to repository
        logger.debug("Step 7: Saving FeedBlockCache to repository");
        FeedBlockCache savedCache = feedBlockCacheRepository.save(feedBlockCache);
        
        // Step 8: Update UserFeedState
        logger.debug("Step 8: Updating UserFeedState");
        updateUserFeedState(userState, assembledPosts, blockNumber, rVector.getVersion());
        userFeedStateRepository.save(userState);
        
        return savedCache;
        
    } catch (Exception e) {
        logger.error("Feed block generation failed", e);
        throw new RuntimeException("Failed to generate feed block for user: " + userId, e);
    }
}
```

#### R(N-2) Lookup Logic

```java
// backend/src/main/java/ai/youthmind/service/FeedBlockGeneratorService.java

/**
 * Get the R vector for a specific block using R(N-2) lag mechanism
 * Block N uses R vector version (N-2)
 * Blocks 1-2 use default distribution (returns null)
 */
private UserRecommendationVector getRVectorForBlock(UUID userId, Integer blockNumber) {
    // Blocks 1-2 use default distribution
    if (blockNumber <= 2) {
        logger.debug("Block {} <= 2, using default distribution (no R vector)", blockNumber);
        return null;
    }
    
    // Calculate target R version: blockNumber - 2
    Integer targetRVersion = blockNumber - 2;
    logger.debug("Block {} requires R vector version {} (N-2)", blockNumber, targetRVersion);
    
    try {
        // Look up R vector version (blockNumber - 2) from repository
        Optional<UserRecommendationVector> rVectorOpt = userRecommendationVectorRepository
            .findByUserIdAndVersion(userId, targetRVersion);
        
        if (rVectorOpt.isPresent()) {
            UserRecommendationVector rVector = rVectorOpt.get();
            logger.info("Found R vector version {} for user {} block {} (N-2)", 
                       targetRVersion, userId, blockNumber);
            return rVector;
        } else {
            logger.info("R vector version {} not found for user {} block {} (N-2), using default", 
                       targetRVersion, userId, blockNumber);
            return null;
        }
    } catch (Exception e) {
        logger.error("Error retrieving R vector for user {} block {} (N-2)", userId, blockNumber, e);
        return null;
    }
}
```

#### Pre-computation Process

```java
// backend/src/main/java/ai/youthmind/service/FeedBlockGeneratorService.java

/**
 * Pre-compute Block N+1 in the background while user is viewing Block N
 * This method runs asynchronously and does not block the current request
 */
@Async("blockPrecomputationExecutor")
public CompletableFuture<Void> precomputeNextBlock(UUID userId, Integer nextBlockNumber) {
    logger.info("Starting pre-computation of block {} for user {}", nextBlockNumber, userId);
    
    try {
        // Step 1: Check if Block N+1 already exists (don't recompute)
        Optional<FeedBlockCache> existingBlock = feedBlockCacheRepository
            .findByUserIdAndBlockNumber(userId, nextBlockNumber);
        
        if (existingBlock.isPresent() && !existingBlock.get().isExpired()) {
            logger.info("Block {} for user {} already exists and is valid, skipping pre-computation", 
                       nextBlockNumber, userId);
            return CompletableFuture.completedFuture(null);
        }
        
        // Step 2: Get R vector for Block N+1 using R(N-1) logic
        // Block N+1 uses R vector version ((N+1)-2) = (N-1)
        UserRecommendationVector rVector = getRVectorForBlock(userId, nextBlockNumber);
        
        if (rVector == null) {
            logger.info("Pre-computing block {} for user {} with default distribution (R(N-1) not available)", 
                       nextBlockNumber, userId);
        } else {
            logger.info("Pre-computing block {} for user {} with R vector version {} (N-1)", 
                       nextBlockNumber, userId, rVector.getVersion());
        }
        
        // Step 3: Generate Block N+1 asynchronously
        FeedBlockCache preComputedBlock = generateBlockForUser(userId, nextBlockNumber, rVector);
        
        // Step 4: Save to cache with isConsumed = false (pre-computed, not yet consumed)
        if (preComputedBlock.wasConsumed()) {
            preComputedBlock.setIsConsumed(false);
            preComputedBlock.setConsumedAt(null);
            feedBlockCacheRepository.save(preComputedBlock);
        }
        
        logger.info("Successfully pre-computed block {} for user {} with {} posts (isConsumed=false)", 
                   nextBlockNumber, userId, preComputedBlock.getPostCount());
        
        return CompletableFuture.completedFuture(null);
        
    } catch (Exception e) {
        logger.error("Failed to pre-compute block {} for user {}", nextBlockNumber, userId, e);
        return CompletableFuture.completedFuture(null); // Don't fail the request
    }
}
```

#### Caching Strategy

- **Pre-computed blocks**: Cached with `isConsumed = false` until user requests them
- **Consumed blocks**: Marked as `isConsumed = true` when returned to user
- **Expiration**: Blocks expire after `CACHE_EXPIRATION_HOURS` (typically 24 hours)
- **Cache lookup**: First check for pre-computed non-consumed blocks, then consumed blocks, then generate new

---

### 4.5 Post Selection Algorithm

#### Time Window Waterfall Logic

The post selection algorithm uses a cascading time window approach:

1. **Primary Window (0-12 hours)**
   - Query posts created in the last 12 hours
   - Highest priority for freshness
   - If enough posts found, stop here

2. **Secondary Window (12-24 hours)**
   - If primary window insufficient, query posts from 12-24 hours ago
   - Continue until enough posts collected

3. **Tertiary Window (24-48 hours)**
   - If secondary window insufficient, query posts from 24-48 hours ago
   - Continue cascading

4. **Final Fallback (2-7 days)**
   - If tertiary window insufficient, query posts from 2-7 days ago
   - Ensures content availability even with limited recent content

**Code Extract: PostSelectionService - Time Priority**

```java
// backend/src/main/java/ai/youthmind/service/PostSelectionService.java

/**
 * Applies waterfall time priority to select posts for a specific emotion.
 * Tries time windows in order: 0-12h (primary) → 12-24h → 24-48h → 2-7d until enough posts are found.
 */
public List<Post> applyTimePriority(String emotion, Integer count, Set<UUID> excludeIds) {
    List<Post> selectedPosts = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    Set<UUID> collectedPostIds = new HashSet<>(excludeIds);
    
    // Define time windows in waterfall order: 0-12h (primary) → 12-24h → 24-48h → 2-7d
    List<TimeWindow> timeWindows = Arrays.asList(
        new TimeWindow("0-12 hours", now.minusHours(12)),
        new TimeWindow("12-24 hours", now.minusHours(24)),
        new TimeWindow("24-48 hours", now.minusHours(48)),
        new TimeWindow("2-7 days", now.minusDays(7))
    );
    
    for (TimeWindow window : timeWindows) {
        int remainingNeeded = count - selectedPosts.size();
        if (remainingNeeded <= 0) {
            logger.info("Enough posts collected ({}), stopping time priority selection", selectedPosts.size());
            break; // We have enough posts
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
    
    return selectedPosts;
}
```

#### Popularity Distribution

Posts are stratified into three popularity tiers:

1. **Hot Posts (≥100 views, 40%)**
   - Query posts with viewCount >= 100
   - Target: 40% of total posts needed
   - Example: For 20 posts needed, get 8 hot posts

2. **Normal Posts (50-99 views, 30%)**
   - Query posts with viewCount >= 50 AND viewCount < 100
   - Target: 30% of total posts needed
   - Example: For 20 posts needed, get 6 normal posts

3. **Cold Posts (<50 views, 30%)**
   - Query posts with viewCount < 50
   - Target: 30% of total posts needed
   - Example: For 20 posts needed, get 6 cold posts

**Code Extract: PostSelectionService - Popularity Distribution**

```java
// backend/src/main/java/ai/youthmind/service/PostSelectionService.java

// Popularity thresholds
private static final int HOT_VIEW_THRESHOLD = 100;
private static final int NORMAL_VIEW_THRESHOLD = 50;

// Distribution percentages
private static final double HOT_PERCENTAGE = 0.4;
private static final double NORMAL_PERCENTAGE = 0.3;
private static final double COLD_PERCENTAGE = 0.3;

public List<Post> selectWithPopularityDistribution(String emotion, Integer count, 
                                                  LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
    // Calculate distribution counts
    int hotCount = (int) (count * HOT_PERCENTAGE);
    int normalCount = (int) (count * NORMAL_PERCENTAGE);
    int coldCount = count - hotCount - normalCount;
    
    logger.info("Popularity distribution - Hot: {}, Normal: {}, Cold: {} (Total: {})", 
               hotCount, normalCount, coldCount, count);
    
    List<Post> selectedPosts = new ArrayList<>();
    
    // Select from each popularity tier
    List<Post> hotPosts = selectHotPosts(normalizedEmotion, hotCount, cutoffTime, excludePostIds);
    List<Post> normalPosts = selectNormalPosts(normalizedEmotion, normalCount, cutoffTime, excludePostIds);
    List<Post> coldPosts = selectColdPosts(normalizedEmotion, coldCount, cutoffTime, excludePostIds);
    
    // Combine results
    selectedPosts.addAll(hotPosts);
    selectedPosts.addAll(normalPosts);
    selectedPosts.addAll(coldPosts);
    
    // Handle insufficient posts in tiers with cascading fallback
    if (selectedPosts.size() < count) {
        int remainingNeeded = count - selectedPosts.size();
        selectedPosts = compensateForShortfall(selectedPosts, normalizedEmotion, 
                                              remainingNeeded, cutoffTime, excludePostIds);
    }
    
    return selectedPosts;
}

/**
 * Select hot posts (viewCount >= 100)
 */
private List<Post> selectHotPosts(String emotion, int count, LocalDateTime cutoffTime, Set<UUID> excludePostIds) {
    Pageable pageable = PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "createdAt"));
    
    // Query for hot posts: view >= 100, emotion match, after cutoff, not flagged
    Page<Post> hotPostsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(
        emotion, cutoffTime, LocalDateTime.now(), pageable);
    
    return hotPostsPage.getContent().stream()
        .filter(post -> !excludePostIds.contains(post.getId()))
        .filter(post -> !post.isFlagged())
        .filter(post -> post.getViewCount() >= HOT_VIEW_THRESHOLD)
        .collect(Collectors.toList());
}
```

#### Fallback Mechanisms

The system implements cascading fallback to ensure content availability:

1. **Time Window Fallback**
   - If 0-12h window insufficient → try 12-24h
   - If 12-24h insufficient → try 24-48h
   - If 24-48h insufficient → try 2-7d

2. **Popularity Tier Fallback**
   - If hot posts insufficient → replace with normal posts
   - If normal posts insufficient → replace with cold posts
   - If cold posts insufficient → get any available posts

3. **Compensation Logic**
   - Calculate shortfall: `remainingNeeded = requestedCount - selectedCount`
   - Query additional posts from any tier
   - Filter out already-selected posts
   - Limit to exactly `remainingNeeded` posts

**Code Extract: PostSelectionService - Fallback Compensation**

```java
// backend/src/main/java/ai/youthmind/service/PostSelectionService.java

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
```

---

## 5. API Endpoints

### 5.1 Feed Endpoints

#### GET /v1/posts/feed/blocks

Retrieve a personalized feed block with recommendation engine integration and block metadata.

**HTTP Method**: `GET`

**Path**: `/v1/posts/feed/blocks`

**Authentication**: Required (JWT Bearer Token)

**Request Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Query Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `blockNumber` | Integer | No | `1` | Block number (1-based) |
| `pageSize` | Integer | No | `20` | Number of posts per block (1-50) |
| `prefetch` | Boolean | No | `false` | Whether to trigger prefetch of next block |

**Request Example**:
```bash
curl -X GET "https://api.youthmind.ai/v1/posts/feed/blocks?blockNumber=1&pageSize=20&prefetch=true" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

**Response Structure**:

```json
{
  "success": true,
  "message": "Feed block retrieved successfully",
  "data": {
    "posts": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "content": "Post content here...",
        "emotion": "happy",
        "author": {
          "id": "123e4567-e89b-12d3-a456-426614174001",
          "username": "user123"
        },
        "resonance": 5,
        "viewCount": 42,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "blockNumber": 1,
    "blockMetadata": {
      "emotionDistribution": {
        "happy": 8,
        "sad": 5,
        "angry": 3,
        "conflicted": 2,
        "surprised": 1,
        "fear": 1
      },
      "rVectorVersion": 15,
      "generatedAt": "2024-01-15T10:30:00",
      "expiresAt": "2024-01-16T10:30:00",
      "isPersonalized": true
    },
    "nextBlockAvailable": true,
    "nextBlockNumber": 2,
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 20,
    "pageSize": 20,
    "hasNext": true,
    "isFirst": true,
    "isLast": false
  }
}
```

**Response Fields**:

- `posts`: Array of `PostDto` objects in this block
- `blockNumber`: Current block number (1-based)
- `blockMetadata`: Block metadata object containing:
  - `emotionDistribution`: Map of emotion names to post counts
  - `rVectorVersion`: R vector version used to generate this block (null for blocks 1-2)
  - `generatedAt`: Timestamp when block was generated
  - `expiresAt`: Timestamp when block expires
  - `isPersonalized`: Whether block uses personalized recommendations
- `nextBlockAvailable`: Whether next block is available for prefetching
- `nextBlockNumber`: Next block number for prefetching
- Pagination fields: `currentPage`, `totalPages`, `totalElements`, `pageSize`, `hasNext`, `isFirst`, `isLast`

**Status Codes**:

| Code | Description |
|------|-------------|
| `200` | Feed block retrieved successfully |
| `400` | Invalid request parameters (blockNumber < 1 or pageSize out of range) |
| `401` | Unauthorized - invalid or missing token |
| `404` | Resource not found |
| `500` | Internal server error |

**Error Response Example**:
```json
{
  "success": false,
  "message": "Block number must be at least 1",
  "data": null
}
```

---

### 5.2 R Vector Endpoints

**Note**: R vector endpoints are currently not fully implemented in the codebase. The following endpoints are planned but may not be available:

#### GET /v1/recommendation/rvector

Get current R vector for authenticated user.

**HTTP Method**: `GET`

**Path**: `/v1/recommendation/rvector`

**Authentication**: Required (JWT Bearer Token)

**Request Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Example**:
```bash
curl -X GET "https://api.youthmind.ai/v1/recommendation/rvector" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

**Response Structure** (Planned):

```json
{
  "success": true,
  "data": {
    "version": 5,
    "weights": {
      "sad": 12,
      "fear": 8,
      "angry": 15,
      "happy": 18,
      "conflicted": 10,
      "surprised": 9
    },
    "topEmotions": ["happy", "angry", "sad"],
    "updatedAt": "2024-01-15T10:30:00",
    "totalWeight": 72,
    "userId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

**Status Codes**:

| Code | Description |
|------|-------------|
| `200` | R vector retrieved successfully |
| `401` | Unauthorized - invalid or missing token |
| `404` | R vector not found for user |
| `500` | Internal server error |

#### GET /v1/recommendation/rvector/history

Get R vector history for authenticated user.

**HTTP Method**: `GET`

**Path**: `/v1/recommendation/rvector/history`

**Authentication**: Required (JWT Bearer Token)

**Query Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `limit` | Integer | No | `50` | Maximum number of history items to return |

**Request Example**:
```bash
curl -X GET "https://api.youthmind.ai/v1/recommendation/rvector/history?limit=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

**Response Structure** (Planned):

```json
{
  "success": true,
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "history": [
      {
        "rVector": [12, 8, 15, 18, 10, 9],
        "topEmotions": ["happy", "angry", "sad"],
        "timestamp": "2024-01-15T10:30:00",
        "interactionCount": 25,
        "triggerEvent": "POST_CREATED",
        "confidenceScore": 0.85
      }
    ]
  }
}
```

#### POST /v1/recommendation/rvector/reset

Reset R vector for authenticated user to default values.

**HTTP Method**: `POST`

**Path**: `/v1/recommendation/rvector/reset`

**Authentication**: Required (JWT Bearer Token)

**Request Body**:

```json
{
  "reason": "User requested reset"
}
```

**Request Example**:
```bash
curl -X POST "https://api.youthmind.ai/v1/recommendation/rvector/reset" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "User requested reset"
  }'
```

**Response Structure** (Planned):

```json
{
  "success": true,
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "success": true,
    "resetAt": "2024-01-15T10:30:00",
    "reason": "User requested reset",
    "newRVector": [10, 10, 10, 10, 10, 10]
  }
}
```

---

### 5.3 User Preference Endpoints

#### GET /admin/v1/users/{userId}/recommendation-status

Get user's recommendation engine status and related information.

**HTTP Method**: `GET`

**Path**: `/admin/v1/users/{userId}/recommendation-status`

**Authentication**: Required (JWT Bearer Token, Admin Role)

**Request Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | UUID | Yes | User ID to get status for |

**Request Example**:
```bash
curl -X GET "https://api.youthmind.ai/admin/v1/users/123e4567-e89b-12d3-a456-426614174000/recommendation-status" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

**Response Structure**:

```json
{
  "success": true,
  "message": "Recommendation status retrieved successfully",
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "username": "user123",
    "email": "user@example.com",
    "recommendationEngineEnabled": true,
    "rVector": {
      "version": 5,
      "createdAt": "2024-01-10T08:00:00",
      "updatedAt": "2024-01-15T10:30:00",
      "topEmotions": ["happy", "angry", "sad"]
    },
    "feedStatistics": {
      "postsCount": 42,
      "likesCount": 128,
      "bookmarksCount": 15
    }
  }
}
```

**Response Fields**:

- `userId`: User ID
- `username`: User's username
- `email`: User's email
- `recommendationEngineEnabled`: Whether recommendation engine is enabled for this user
- `rVector`: R vector information (or `null` if not found)
  - `version`: Current R vector version
  - `createdAt`: When R vector was created
  - `updatedAt`: When R vector was last updated
  - `topEmotions`: Top 3 emotions
- `feedStatistics`: Basic feed statistics
  - `postsCount`: Number of posts created
  - `likesCount`: Number of likes given
  - `bookmarksCount`: Number of bookmarks

**Status Codes**:

| Code | Description |
|------|-------------|
| `200` | Recommendation status retrieved successfully |
| `401` | Unauthorized - invalid or missing token |
| `403` | Forbidden - admin access required |
| `404` | User not found |
| `500` | Internal server error |

#### PUT /admin/v1/users/{userId}/recommendation-toggle

Enable or disable personalized recommendations for a specific user.

**HTTP Method**: `PUT`

**Path**: `/admin/v1/users/{userId}/recommendation-toggle`

**Authentication**: Required (JWT Bearer Token, Admin Role)

**Request Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | UUID | Yes | User ID to update |

**Request Body**:

```json
{
  "enabled": true
}
```

**Request Body Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `enabled` | Boolean | Yes | Whether to enable recommendation engine |

**Request Example**:
```bash
curl -X PUT "https://api.youthmind.ai/admin/v1/users/123e4567-e89b-12d3-a456-426614174000/recommendation-toggle" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true
  }'
```

**Response Structure**:

```json
{
  "success": true,
  "message": "Recommendation engine toggle updated successfully",
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "username": "user123",
    "email": "user@example.com",
    "previousValue": false,
    "newValue": true,
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Response Fields**:

- `userId`: User ID
- `username`: User's username
- `email`: User's email
- `previousValue`: Previous recommendation engine enabled state
- `newValue`: New recommendation engine enabled state
- `updatedAt`: Timestamp when update occurred

**Status Codes**:

| Code | Description |
|------|-------------|
| `200` | Recommendation toggle updated successfully |
| `400` | Invalid request body |
| `401` | Unauthorized - invalid or missing token |
| `403` | Forbidden - admin access required |
| `404` | User not found |
| `500` | Internal server error |

---

### 5.4 Recommendation Mapping Rules Endpoints

#### GET /admin/v1/recommendationmapping/rules

Get all recommendation mapping rules.

**HTTP Method**: `GET`

**Path**: `/admin/v1/recommendationmapping/rules`

**Authentication**: Required (JWT Bearer Token, Admin Role)

**Request Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Example**:
```bash
curl -X GET "https://api.youthmind.ai/admin/v1/recommendationmapping/rules" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

**Response Structure**:

```json
{
  "success": true,
  "message": "Mapping rules retrieved successfully",
  "data": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "emotionCombination": "angry,fear,sad",
      "sadPercentage": 20,
      "fearPercentage": 20,
      "angryPercentage": 20,
      "happyPercentage": 20,
      "conflictedPercentage": 10,
      "surprisedPercentage": 10,
      "createdAt": "2024-01-10T08:00:00",
      "updatedAt": "2024-01-10T08:00:00"
    }
  ]
}
```

**Response Fields**:

- Array of `RecommendationMappingRule` objects, each containing:
  - `id`: Rule ID
  - `emotionCombination`: Sorted comma-separated emotion combination (e.g., "angry,fear,sad")
  - `sadPercentage`: Percentage for sad emotion (0-100)
  - `fearPercentage`: Percentage for fear emotion (0-100)
  - `angryPercentage`: Percentage for angry emotion (0-100)
  - `happyPercentage`: Percentage for happy emotion (0-100)
  - `conflictedPercentage`: Percentage for conflicted emotion (0-100)
  - `surprisedPercentage`: Percentage for surprised emotion (0-100)
  - `createdAt`: Timestamp when rule was created
  - `updatedAt`: Timestamp when rule was last updated

**Status Codes**:

| Code | Description |
|------|-------------|
| `200` | Mapping rules retrieved successfully |
| `401` | Unauthorized - invalid or missing token |
| `403` | Forbidden - admin access required |
| `500` | Internal server error |

#### GET /admin/v1/recommendationmapping/rules/{id}

Get a specific recommendation mapping rule by ID.

**HTTP Method**: `GET`

**Path**: `/admin/v1/recommendationmapping/rules/{id}`

**Authentication**: Required (JWT Bearer Token, Admin Role)

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Mapping rule ID |

**Request Example**:
```bash
curl -X GET "https://api.youthmind.ai/admin/v1/recommendationmapping/rules/123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

**Response Structure**:

```json
{
  "success": true,
  "message": "Mapping rule retrieved successfully",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "emotionCombination": "angry,fear,sad",
    "sadPercentage": 20,
    "fearPercentage": 20,
    "angryPercentage": 20,
    "happyPercentage": 20,
    "conflictedPercentage": 10,
    "surprisedPercentage": 10,
    "createdAt": "2024-01-10T08:00:00",
    "updatedAt": "2024-01-10T08:00:00"
  }
}
```

**Status Codes**:

| Code | Description |
|------|-------------|
| `200` | Mapping rule retrieved successfully |
| `401` | Unauthorized - invalid or missing token |
| `403` | Forbidden - admin access required |
| `404` | Mapping rule not found |
| `500` | Internal server error |

#### PUT /admin/v1/recommendationmapping/rules/{id}

Update the emotion percentage distribution for a specific mapping rule.

**HTTP Method**: `PUT`

**Path**: `/admin/v1/recommendationmapping/rules/{id}`

**Authentication**: Required (JWT Bearer Token, Admin Role)

**Path Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | UUID | Yes | Mapping rule ID |

**Request Body**:

```json
{
  "sadPercentage": 20,
  "fearPercentage": 20,
  "angryPercentage": 20,
  "happyPercentage": 20,
  "conflictedPercentage": 10,
  "surprisedPercentage": 10
}
```

**Request Body Fields**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sadPercentage` | Integer | Yes | Percentage for sad emotion (0-100) |
| `fearPercentage` | Integer | Yes | Percentage for fear emotion (0-100) |
| `angryPercentage` | Integer | Yes | Percentage for angry emotion (0-100) |
| `happyPercentage` | Integer | Yes | Percentage for happy emotion (0-100) |
| `conflictedPercentage` | Integer | Yes | Percentage for conflicted emotion (0-100) |
| `surprisedPercentage` | Integer | Yes | Percentage for surprised emotion (0-100) |

**Validation**: All percentages must sum to exactly 100.

**Request Example**:
```bash
curl -X PUT "https://api.youthmind.ai/admin/v1/recommendationmapping/rules/123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "sadPercentage": 20,
    "fearPercentage": 20,
    "angryPercentage": 20,
    "happyPercentage": 20,
    "conflictedPercentage": 10,
    "surprisedPercentage": 10
  }'
```

**Response Structure**:

```json
{
  "success": true,
  "message": "Mapping rule updated successfully",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "emotionCombination": "angry,fear,sad",
    "sadPercentage": 20,
    "fearPercentage": 20,
    "angryPercentage": 20,
    "happyPercentage": 20,
    "conflictedPercentage": 10,
    "surprisedPercentage": 10,
    "createdAt": "2024-01-10T08:00:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

**Status Codes**:

| Code | Description |
|------|-------------|
| `200` | Mapping rule updated successfully |
| `400` | Invalid request - percentages must sum to 100 |
| `401` | Unauthorized - invalid or missing token |
| `403` | Forbidden - admin access required |
| `404` | Mapping rule not found |
| `500` | Internal server error |

**Error Response Example** (Percentages don't sum to 100):
```json
{
  "success": false,
  "message": "Percentages must sum to exactly 100. Current sum: 95",
  "data": null
}
```

---

### 5.5 Common Response Patterns

#### Success Response

All successful API responses follow this structure:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

#### Error Response

All error responses follow this structure:

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

#### Authentication

All endpoints require JWT authentication via Bearer token in the `Authorization` header:

```
Authorization: Bearer <jwt_token>
```

#### Admin Endpoints

Admin endpoints require both authentication and the `ROLE_ADMIN` role. Users without admin role will receive a `403 Forbidden` response.

---

## 6. Frontend Integration Guide

This section provides practical guidance for frontend developers integrating with the YouthMind Recommendation Engine. All examples use TypeScript/JavaScript and follow modern React patterns.

### 6.1 Getting Started

#### Prerequisites

Before integrating the recommendation engine, ensure you have:

- **Authentication System**: JWT-based authentication implemented
- **HTTP Client**: Axios, Fetch API, or similar HTTP client library
- **State Management**: React Query, Redux, or similar (recommended)
- **TypeScript**: Type definitions for type safety (recommended)

#### Authentication Requirements

All recommendation engine endpoints require JWT authentication. The token must be included in the `Authorization` header:

```typescript
// Token management example
class TokenManager {
  static getToken(): string | null {
    return localStorage.getItem('access_token');
  }
  
  static setToken(token: string): void {
    localStorage.setItem('access_token', token);
  }
}
```

#### Base URL Configuration

Configure your API base URL based on environment:

```typescript
// config/api.ts
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const API_ENDPOINTS = {
  FEED_BLOCKS: '/v1/posts/feed/blocks',
  POST_VIEW: '/v1/posts',
  POST_LIKE: '/v1/posts',
  RECOMMENDATION_STATUS: '/admin/v1/users',
} as const;
```

#### Environment Setup

Create environment-specific configuration:

```typescript
// .env.development
REACT_APP_API_URL=http://localhost:8080

// .env.production
REACT_APP_API_URL=https://api.youthmind.ai
```

#### HTTP Client Setup

Configure your HTTP client with authentication interceptor:

```typescript
// api/client.ts
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = TokenManager.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Handle token refresh or redirect to login
      TokenManager.clearToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export { apiClient };
```

---

### 6.2 Fetching Personalized Feed

#### API Endpoint

**GET** `/v1/posts/feed/blocks`

#### Request Headers

```typescript
{
  'Authorization': 'Bearer <jwt_token>',
  'Content-Type': 'application/json'
}
```

#### Request Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `blockNumber` | number | No | `1` | Block number (1-based) |
| `pageSize` | number | No | `20` | Posts per block (1-50) |
| `prefetch` | boolean | No | `false` | Trigger prefetch of next block |

#### TypeScript Types

```typescript
// types/recommendation.ts
export interface FeedBlockResponse {
  success: boolean;
  message: string;
  data: {
    posts: Post[];
    blockNumber: number;
    blockMetadata: BlockMetadata;
    nextBlockAvailable: boolean;
    nextBlockNumber: number;
    currentPage: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    hasNext: boolean;
    isFirst: boolean;
    isLast: boolean;
  };
}

export interface BlockMetadata {
  emotionDistribution: {
    sad: number;
    fear: number;
    angry: number;
    happy: number;
    conflicted: number;
    surprised: number;
  };
  rVectorVersion: number | null;
  generatedAt: string;
  expiresAt: string;
  isPersonalized: boolean;
}

export interface Post {
  id: string;
  content: string;
  emotion: string;
  author: {
    id: string;
    username: string;
  };
  resonance: number;
  viewCount: number;
  createdAt: string;
}
```

#### API Service Implementation

```typescript
// services/feedService.ts
import { apiClient } from '../api/client';
import type { FeedBlockResponse } from '../types/recommendation';

export class FeedService {
  /**
   * Fetch a personalized feed block
   */
  static async getFeedBlock(
    blockNumber: number = 1,
    pageSize: number = 20,
    prefetch: boolean = false
  ): Promise<FeedBlockResponse> {
    try {
      const response = await apiClient.get<FeedBlockResponse>(
        '/v1/posts/feed/blocks',
        {
          params: {
            blockNumber,
            pageSize,
            prefetch,
          },
        }
      );
      
      return response.data;
    } catch (error) {
      console.error('Failed to fetch feed block:', error);
      throw error;
    }
  }
}
```

#### React Hook Example

```typescript
// hooks/useFeedBlocks.ts
import { useState, useEffect, useCallback } from 'react';
import { FeedService } from '../services/feedService';
import type { FeedBlockResponse, Post } from '../types/recommendation';

export function useFeedBlocks() {
  const [blocks, setBlocks] = useState<Post[][]>([]);
  const [currentBlockNumber, setCurrentBlockNumber] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [hasNextBlock, setHasNextBlock] = useState(true);

  const loadBlock = useCallback(async (blockNumber: number) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await FeedService.getFeedBlock(blockNumber, 20, true);
      
      setBlocks((prev) => {
        const newBlocks = [...prev];
        newBlocks[blockNumber - 1] = response.data.posts;
        return newBlocks;
      });
      
      setCurrentBlockNumber(blockNumber);
      setHasNextBlock(response.data.nextBlockAvailable);
      
      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to load feed block');
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  const loadNextBlock = useCallback(() => {
    if (!loading && hasNextBlock) {
      return loadBlock(currentBlockNumber + 1);
    }
  }, [loading, hasNextBlock, currentBlockNumber, loadBlock]);

  useEffect(() => {
    // Load first block on mount
    loadBlock(1);
  }, [loadBlock]);

  return {
    blocks: blocks.flat(),
    currentBlockNumber,
    loading,
    error,
    hasNextBlock,
    loadNextBlock,
    reloadBlock: () => loadBlock(currentBlockNumber),
  };
}
```

#### Error Handling

```typescript
// Error handling example
try {
  const response = await FeedService.getFeedBlock(blockNumber);
  // Handle success
} catch (error) {
  if (axios.isAxiosError(error)) {
    switch (error.response?.status) {
      case 400:
        console.error('Invalid request parameters:', error.response.data.message);
        break;
      case 401:
        console.error('Unauthorized - token expired or invalid');
        // Redirect to login
        break;
      case 404:
        console.error('Feed block not found');
        break;
      case 500:
        console.error('Server error - please try again later');
        break;
      default:
        console.error('Unexpected error:', error.message);
    }
  } else {
    console.error('Network error:', error);
  }
}
```

---

### 6.3 Tracking User Interactions

#### Post Creation Tracking

**Automatic**: Post creation automatically updates the R vector. No additional API call needed.

When a user creates a post:
1. The backend automatically classifies the post's emotion
2. The R vector is updated with +6 weight for that emotion
3. No frontend action required

```typescript
// Post creation (R vector update happens automatically)
const createPost = async (content: string, emotion?: string) => {
  const response = await apiClient.post('/v1/posts/create', {
    content,
    emotion, // Optional - backend will classify if not provided
  });
  
  // R vector is automatically updated on backend
  // No additional API call needed
  return response.data;
};
```

#### Like Tracking

**API Call Required**: Call the like endpoint when user likes a post.

**Endpoint**: `PUT /v1/posts/{postId}/resonance`

```typescript
// services/postService.ts
export class PostService {
  /**
   * Toggle like (resonance) on a post
   * Automatically updates R vector with +4 weight if it's a like action
   */
  static async toggleLike(postId: string): Promise<Post> {
    try {
      const response = await apiClient.put<ApiResponse<Post>>(
        `/v1/posts/${postId}/resonance`
      );
      
      // R vector is automatically updated on backend if it's a like
      return response.data.data;
    } catch (error) {
      console.error('Failed to toggle like:', error);
      throw error;
    }
  }
}
```

**React Component Example**:

```typescript
// components/PostCard.tsx
import { useState } from 'react';
import { PostService } from '../services/postService';

interface PostCardProps {
  post: Post;
  onLikeChange?: (postId: string, liked: boolean) => void;
}

export function PostCard({ post, onLikeChange }: PostCardProps) {
  const [isLiked, setIsLiked] = useState(post.userHasLiked || false);
  const [liking, setLiking] = useState(false);

  const handleLike = async () => {
    if (liking) return;
    
    setLiking(true);
    try {
      const updatedPost = await PostService.toggleLike(post.id);
      
      // Update local state
      const newLikedState = !isLiked;
      setIsLiked(newLikedState);
      
      // Notify parent component
      onLikeChange?.(post.id, newLikedState);
      
      // R vector is automatically updated on backend
    } catch (error) {
      console.error('Failed to like post:', error);
      // Revert optimistic update
      setIsLiked(isLiked);
    } finally {
      setLiking(false);
    }
  };

  return (
    <div className="post-card">
      <p>{post.content}</p>
      <button 
        onClick={handleLike} 
        disabled={liking}
        className={isLiked ? 'liked' : ''}
      >
        {isLiked ? '❤️' : '🤍'} {post.resonance}
      </button>
    </div>
  );
}
```

#### View Tracking

**API Call Required**: Track post views with duration to update R vector.

**Important**: R vector is only updated if view duration ≥ 15 seconds.

**Endpoints**:
- `POST /v1/posts/{postId}/view/duration` - Track view with duration
- `POST /v1/posts/{postId}/view/start` - Start view session (optional)
- `POST /v1/posts/{postId}/view/end` - End view session (optional)

#### Duration Tracking Best Practices

**Option 1: Simple Duration Tracking** (Recommended)

Track view duration when user leaves the post:

```typescript
// services/postService.ts
export class PostService {
  /**
   * Track post view with duration
   * Updates R vector if duration >= 15 seconds
   */
  static async trackPostView(
    postId: string,
    durationSeconds: number
  ): Promise<Post> {
    try {
      const response = await apiClient.post<ApiResponse<Post>>(
        `/v1/posts/${postId}/view/duration`,
        {
          durationSeconds,
        }
      );
      
      // R vector is automatically updated if duration >= 15s
      return response.data.data;
    } catch (error) {
      console.error('Failed to track post view:', error);
      throw error;
    }
  }
}
```

**React Hook for View Tracking**:

```typescript
// hooks/usePostViewTracking.ts
import { useEffect, useRef } from 'react';
import { PostService } from '../services/postService';

export function usePostViewTracking(postId: string) {
  const startTimeRef = useRef<number | null>(null);
  const isVisibleRef = useRef<boolean>(false);

  useEffect(() => {
    // Start tracking when component mounts
    startTimeRef.current = Date.now();
    isVisibleRef.current = true;

    // Track view when component unmounts or becomes invisible
    return () => {
      if (startTimeRef.current && isVisibleRef.current) {
        const durationSeconds = Math.floor(
          (Date.now() - startTimeRef.current) / 1000
        );
        
        // Only track if duration >= 15 seconds
        if (durationSeconds >= 15) {
          PostService.trackPostView(postId, durationSeconds).catch((error) => {
            console.error('Failed to track view:', error);
          });
        }
      }
    };
  }, [postId]);

  // Handle visibility changes (tab switch, minimize, etc.)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        // User switched tabs or minimized window
        isVisibleRef.current = false;
      } else {
        // User returned to tab
        if (!startTimeRef.current) {
          startTimeRef.current = Date.now();
        }
        isVisibleRef.current = true;
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);
}
```

**React Component Example**:

```typescript
// components/PostView.tsx
import { useEffect, useRef } from 'react';
import { PostService } from '../services/postService';

interface PostViewProps {
  post: Post;
}

export function PostView({ post }: PostViewProps) {
  const startTimeRef = useRef<number | null>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    // Start tracking when post becomes visible
    startTimeRef.current = Date.now();

    // Track view every 15 seconds (in case user stays longer)
    intervalRef.current = setInterval(async () => {
      if (startTimeRef.current) {
        const durationSeconds = Math.floor(
          (Date.now() - startTimeRef.current) / 1000
        );
        
        if (durationSeconds >= 15) {
          try {
            await PostService.trackPostView(post.id, durationSeconds);
            // Reset start time to avoid duplicate tracking
            startTimeRef.current = Date.now();
          } catch (error) {
            console.error('Failed to track view:', error);
          }
        }
      }
    }, 15000); // Check every 15 seconds

    // Track final duration on unmount
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
      
      if (startTimeRef.current) {
        const durationSeconds = Math.floor(
          (Date.now() - startTimeRef.current) / 1000
        );
        
        if (durationSeconds >= 15) {
          PostService.trackPostView(post.id, durationSeconds).catch((error) => {
            console.error('Failed to track final view:', error);
          });
        }
      }
    };
  }, [post.id]);

  return (
    <div className="post-view">
      <h2>{post.content}</h2>
      {/* Post content */}
    </div>
  );
}
```

**Option 2: Start/End Session Tracking** (Advanced)

For more precise tracking, use start/end session endpoints:

```typescript
// services/postService.ts
export class PostService {
  /**
   * Start tracking a post view session
   */
  static async startPostView(postId: string): Promise<void> {
    try {
      await apiClient.post(`/v1/posts/${postId}/view/start`);
    } catch (error) {
      console.error('Failed to start view session:', error);
      throw error;
    }
  }

  /**
   * End a post view session
   * Automatically calculates duration and updates R vector if >= 15s
   */
  static async endPostView(postId: string): Promise<void> {
    try {
      await apiClient.post(`/v1/posts/${postId}/view/end`);
    } catch (error) {
      console.error('Failed to end view session:', error);
      throw error;
    }
  }
}
```

**Usage Example**:

```typescript
// In your component
useEffect(() => {
  PostService.startPostView(postId);
  
  return () => {
    PostService.endPostView(postId);
  };
}, [postId]);
```

---

### 6.4 Handling Feed Blocks

#### Understanding Block Numbers

- **Block 1**: First feed view (uses default distribution)
- **Block 2**: First refresh (uses default distribution)
- **Block 3+**: Personalized blocks (uses R vector version N-2)

#### Infinite Scroll Implementation

**React Component with Infinite Scroll**:

```typescript
// components/FeedView.tsx
import { useEffect, useRef, useCallback } from 'react';
import { useFeedBlocks } from '../hooks/useFeedBlocks';
import { PostCard } from './PostCard';

export function FeedView() {
  const {
    blocks,
    currentBlockNumber,
    loading,
    error,
    hasNextBlock,
    loadNextBlock,
  } = useFeedBlocks();

  const observerRef = useRef<IntersectionObserver | null>(null);
  const loadMoreRef = useCallback(
    (node: HTMLDivElement | null) => {
      if (loading) return;
      if (observerRef.current) observerRef.current.disconnect();

      observerRef.current = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && hasNextBlock) {
          loadNextBlock();
        }
      });

      if (node) observerRef.current.observe(node);
    },
    [loading, hasNextBlock, loadNextBlock]
  );

  if (error) {
    return (
      <div className="error">
        <p>Failed to load feed: {error.message}</p>
        <button onClick={() => loadNextBlock()}>Retry</button>
      </div>
    );
  }

  return (
    <div className="feed-view">
      {blocks.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}
      
      {loading && <div className="loading">Loading...</div>}
      
      {hasNextBlock && (
        <div ref={loadMoreRef} className="load-more-trigger">
          Scroll for more
        </div>
      )}
      
      {!hasNextBlock && blocks.length > 0 && (
        <div className="end-of-feed">You're all caught up!</div>
      )}
    </div>
  );
}
```

#### Pre-loading Next Block

The recommendation engine supports pre-loading the next block:

```typescript
// Enhanced feed hook with pre-loading
export function useFeedBlocksWithPreload() {
  const [blocks, setBlocks] = useState<Post[][]>([]);
  const [currentBlockNumber, setCurrentBlockNumber] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [hasNextBlock, setHasNextBlock] = useState(true);
  const [preloadedBlock, setPreloadedBlock] = useState<Post[] | null>(null);

  const loadBlock = useCallback(async (blockNumber: number, prefetch = false) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await FeedService.getFeedBlock(blockNumber, 20, prefetch);
      
      if (prefetch) {
        // Store preloaded block
        setPreloadedBlock(response.data.posts);
      } else {
        // Use preloaded block if available
        if (preloadedBlock && blockNumber === currentBlockNumber + 1) {
          setBlocks((prev) => [...prev, preloadedBlock]);
          setPreloadedBlock(null);
        } else {
          setBlocks((prev) => {
            const newBlocks = [...prev];
            newBlocks[blockNumber - 1] = response.data.posts;
            return newBlocks;
          });
        }
        
        setCurrentBlockNumber(blockNumber);
        setHasNextBlock(response.data.nextBlockAvailable);
        
        // Preload next block if available
        if (response.data.nextBlockAvailable) {
          FeedService.getFeedBlock(blockNumber + 1, 20, true).catch((err) => {
            console.error('Failed to preload next block:', err);
          });
        }
      }
      
      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to load feed block');
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [currentBlockNumber, preloadedBlock]);

  const loadNextBlock = useCallback(() => {
    if (!loading && hasNextBlock) {
      return loadBlock(currentBlockNumber + 1, false);
    }
  }, [loading, hasNextBlock, currentBlockNumber, loadBlock]);

  useEffect(() => {
    // Load first block and preload second block
    loadBlock(1, false);
    FeedService.getFeedBlock(2, 20, true).then((response) => {
      setPreloadedBlock(response.data.posts);
    }).catch((err) => {
      console.error('Failed to preload block 2:', err);
    });
  }, []);

  return {
    blocks: blocks.flat(),
    currentBlockNumber,
    loading,
    error,
    hasNextBlock,
    loadNextBlock,
  };
}
```

#### Block Metadata Usage

Use block metadata to display emotion distribution and personalization status:

```typescript
// components/FeedBlockInfo.tsx
interface FeedBlockInfoProps {
  blockMetadata: BlockMetadata;
}

export function FeedBlockInfo({ blockMetadata }: FeedBlockInfoProps) {
  const emotionDistribution = blockMetadata.emotionDistribution;
  const isPersonalized = blockMetadata.isPersonalized;
  const rVectorVersion = blockMetadata.rVectorVersion;

  return (
    <div className="block-info">
      {isPersonalized ? (
        <div className="personalized-badge">
          ✨ Personalized (R vector v{rVectorVersion})
        </div>
      ) : (
        <div className="default-badge">Default feed</div>
      )}
      
      <div className="emotion-distribution">
        <h4>Emotion Distribution:</h4>
        <div className="emotion-bars">
          {Object.entries(emotionDistribution).map(([emotion, count]) => (
            <div key={emotion} className="emotion-bar">
              <span className="emotion-label">{emotion}:</span>
              <div className="bar" style={{ width: `${(count / 20) * 100}%` }}>
                {count}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
```

---

### 6.5 User Preferences

#### Checking Recommendation Engine Status

**Endpoint**: `GET /admin/v1/users/{userId}/recommendation-status`

**Note**: This endpoint requires admin role. For regular users, check the user profile endpoint.

```typescript
// services/userService.ts
export class UserService {
  /**
   * Get user's recommendation engine status (Admin only)
   */
  static async getRecommendationStatus(userId: string): Promise<RecommendationStatus> {
    try {
      const response = await apiClient.get<ApiResponse<RecommendationStatus>>(
        `/admin/v1/users/${userId}/recommendation-status`
      );
      
      return response.data.data;
    } catch (error) {
      console.error('Failed to get recommendation status:', error);
      throw error;
    }
  }
}

interface RecommendationStatus {
  userId: string;
  username: string;
  email: string;
  recommendationEngineEnabled: boolean;
  rVector: {
    version: number;
    createdAt: string;
    updatedAt: string;
    topEmotions: string[];
  } | null;
  feedStatistics: {
    postsCount: number;
    likesCount: number;
    bookmarksCount: number;
  };
}
```

#### Toggling Recommendation Engine

**Endpoint**: `PUT /admin/v1/users/{userId}/recommendation-toggle`

**Note**: This endpoint requires admin role.

```typescript
// services/userService.ts
export class UserService {
  /**
   * Toggle recommendation engine for a user (Admin only)
   */
  static async toggleRecommendationEngine(
    userId: string,
    enabled: boolean
  ): Promise<ToggleResponse> {
    try {
      const response = await apiClient.put<ApiResponse<ToggleResponse>>(
        `/admin/v1/users/${userId}/recommendation-toggle`,
        {
          enabled,
        }
      );
      
      return response.data.data;
    } catch (error) {
      console.error('Failed to toggle recommendation engine:', error);
      throw error;
    }
  }
}

interface ToggleResponse {
  userId: string;
  username: string;
  email: string;
  previousValue: boolean;
  newValue: boolean;
  updatedAt: string;
}
```

**React Component Example**:

```typescript
// components/RecommendationSettings.tsx
import { useState, useEffect } from 'react';
import { UserService } from '../services/userService';

interface RecommendationSettingsProps {
  userId: string;
}

export function RecommendationSettings({ userId }: RecommendationSettingsProps) {
  const [enabled, setEnabled] = useState<boolean>(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Load current status
    UserService.getRecommendationStatus(userId)
      .then((status) => {
        setEnabled(status.recommendationEngineEnabled);
      })
      .catch((err) => {
        setError('Failed to load recommendation status');
        console.error(err);
      });
  }, [userId]);

  const handleToggle = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await UserService.toggleRecommendationEngine(
        userId,
        !enabled
      );
      
      setEnabled(response.newValue);
      
      // Show success message
      alert(
        `Recommendation engine ${response.newValue ? 'enabled' : 'disabled'} successfully`
      );
    } catch (err) {
      setError('Failed to update recommendation settings');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="recommendation-settings">
      <h3>Recommendation Engine</h3>
      
      <div className="toggle-section">
        <label>
          <input
            type="checkbox"
            checked={enabled}
            onChange={handleToggle}
            disabled={loading}
          />
          Enable personalized recommendations
        </label>
        
        {error && <div className="error">{error}</div>}
        
        <p className="description">
          When enabled, your feed will be personalized based on your emotional
          engagement patterns. The system learns from your posts, likes, and
          viewing behavior.
        </p>
      </div>
    </div>
  );
}
```

---

### 6.6 Best Practices

#### 1. Error Handling

Always implement comprehensive error handling:

```typescript
// utils/errorHandler.ts
export function handleApiError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;
    
    switch (status) {
      case 400:
        return `Invalid request: ${message}`;
      case 401:
        return 'Authentication required. Please log in.';
      case 403:
        return 'Access denied. Admin privileges required.';
      case 404:
        return 'Resource not found.';
      case 500:
        return 'Server error. Please try again later.';
      default:
        return `Unexpected error: ${message}`;
    }
  }
  
  return 'An unexpected error occurred';
}
```

#### 2. Loading States

Always show loading indicators:

```typescript
// components/LoadingSpinner.tsx
export function LoadingSpinner() {
  return (
    <div className="loading-spinner">
      <div className="spinner" />
      <p>Loading feed...</p>
    </div>
  );
}
```

#### 3. Retry Logic

Implement retry logic for failed requests:

```typescript
// utils/retry.ts
export async function retryRequest<T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: Error;
  
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error instanceof Error ? error : new Error('Unknown error');
      
      if (i < maxRetries - 1) {
        await new Promise((resolve) => setTimeout(resolve, delay * (i + 1)));
      }
    }
  }
  
  throw lastError!;
}

// Usage
const response = await retryRequest(() => 
  FeedService.getFeedBlock(blockNumber)
);
```

#### 4. Caching

Cache feed blocks for offline viewing:

```typescript
// utils/feedCache.ts
class FeedBlockCache {
  private static readonly CACHE_KEY = 'feed_blocks';
  private static readonly CACHE_EXPIRY = 24 * 60 * 60 * 1000; // 24 hours

  static setBlock(blockNumber: number, data: FeedBlockResponse): void {
    const cache = this.getCache();
    cache[blockNumber] = {
      data,
      timestamp: Date.now(),
    };
    localStorage.setItem(this.CACHE_KEY, JSON.stringify(cache));
  }

  static getBlock(blockNumber: number): FeedBlockResponse | null {
    const cache = this.getCache();
    const cached = cache[blockNumber];
    
    if (!cached) return null;
    
    // Check if cache is expired
    if (Date.now() - cached.timestamp > this.CACHE_EXPIRY) {
      delete cache[blockNumber];
      localStorage.setItem(this.CACHE_KEY, JSON.stringify(cache));
      return null;
    }
    
    return cached.data;
  }

  private static getCache(): Record<number, { data: FeedBlockResponse; timestamp: number }> {
    const cached = localStorage.getItem(this.CACHE_KEY);
    return cached ? JSON.parse(cached) : {};
  }
}
```

#### 5. Type Safety

Use TypeScript for type safety:

```typescript
// types/api.ts
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

// Always type your API responses
const response = await apiClient.get<ApiResponse<FeedBlockResponse>>(
  '/v1/posts/feed/blocks'
);
```

---

### 6.7 Common Patterns

#### Pattern 1: Optimistic Updates

Update UI immediately, then sync with backend:

```typescript
const handleLike = async () => {
  // Optimistic update
  const previousLiked = isLiked;
  setIsLiked(!isLiked);
  setResonanceCount((prev) => (prev + (isLiked ? -1 : 1)));
  
  try {
    await PostService.toggleLike(postId);
  } catch (error) {
    // Revert on error
    setIsLiked(previousLiked);
    setResonanceCount((prev) => (prev + (isLiked ? 1 : -1)));
    alert('Failed to update like. Please try again.');
  }
};
```

#### Pattern 2: Debounced View Tracking

Debounce view tracking to avoid excessive API calls:

```typescript
// utils/debounce.ts
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout;
  
  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

// Usage
const debouncedTrackView = debounce((postId: string, duration: number) => {
  PostService.trackPostView(postId, duration);
}, 1000);
```

#### Pattern 3: Infinite Scroll with Virtualization

For large feeds, use virtualization:

```typescript
import { useVirtualizer } from '@tanstack/react-virtual';

export function VirtualizedFeed({ posts }: { posts: Post[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: posts.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 200,
    overscan: 5,
  });

  return (
    <div ref={parentRef} className="feed-container">
      <div
        style={{
          height: `${virtualizer.getTotalSize()}px`,
          width: '100%',
          position: 'relative',
        }}
      >
        {virtualizer.getVirtualItems().map((virtualItem) => (
          <div
            key={virtualItem.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualItem.size}px`,
              transform: `translateY(${virtualItem.start}px)`,
            }}
          >
            <PostCard post={posts[virtualItem.index]} />
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

---

## 7. Configuration

This section covers all configuration properties, database setup, and migration requirements for the Recommendation Engine.

### 7.1 Application Properties

All recommendation engine configuration is managed through Spring Boot's `@ConfigurationProperties` mechanism. Properties are prefixed with `youthmind.recommendation.*` and can be configured in `application.yml` or `application.properties`.

#### Core Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `youthmind.recommendation.enabled` | Boolean | `true` | Master switch to enable/disable the recommendation engine |
| `youthmind.recommendation.defaultBlockSize` | Integer | `20` | Default number of posts per feed block |
| `youthmind.recommendation.blockExpiryHours` | Integer | `1` | Hours until a cached feed block expires |
| `youthmind.recommendation.rvectorCacheTtlMinutes` | Integer | `30` | TTL for R vector cache in minutes |
| `youthmind.recommendation.eventQueueMaxDepth` | Integer | `10000` | Maximum depth of event queue |
| `youthmind.recommendation.blockPrecalcEnabled` | Boolean | `true` | Enable block pre-calculation for next block |
| `youthmind.recommendation.fallbackToChronological` | Boolean | `true` | Fallback to chronological feed if recommendation fails |
| `youthmind.recommendation.minPostsPerEmotion` | Integer | `3` | Minimum posts required per emotion category |

#### Redis Streams Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `youthmind.recommendation.streams.rvectorUpdatesStream` | String | `"recommendation:rvector-updates"` | Redis stream name for R vector update events |
| `youthmind.recommendation.streams.blockPrecalcStream` | String | `"recommendation:block-precalc"` | Redis stream name for block pre-calculation events |
| `youthmind.recommendation.streams.consumerGroup` | String | `"recommendation-engine-workers"` | Consumer group name for Redis Streams |
| `youthmind.recommendation.streams.maxRetries` | Integer | `3` | Maximum retries for stream operations |
| `youthmind.recommendation.streams.retryDelayMs` | Long | `1000` | Retry delay in milliseconds |

#### Rate Limiting Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `youthmind.recommendation.rateLimiting.maxRequestsPerMinute` | Integer | `100` | Maximum requests per minute per user |
| `youthmind.recommendation.rateLimiting.burstCapacity` | Integer | `10` | Burst capacity for rate limiting |

#### Circuit Breaker Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `youthmind.recommendation.circuitBreaker.failureRateThreshold` | Integer | `50` | Failure rate threshold percentage (1-100) |
| `youthmind.recommendation.circuitBreaker.waitDurationSeconds` | Integer | `30` | Wait duration in seconds before retrying |
| `youthmind.recommendation.circuitBreaker.slidingWindowSize` | Integer | `10` | Sliding window size for failure calculation |
| `youthmind.recommendation.circuitBreaker.minimumCalls` | Integer | `5` | Minimum number of calls before calculating failure rate |

#### Performance Targets Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `youthmind.recommendation.performanceTargets.rVectorUpdateMs` | Integer | `5` | Target R vector update time in milliseconds |
| `youthmind.recommendation.performanceTargets.topEmotionSelectMs` | Integer | `1` | Target top emotion selection time in milliseconds |
| `youthmind.recommendation.performanceTargets.singleEmotionSelectMs` | Integer | `50` | Target single emotion selection time in milliseconds |
| `youthmind.recommendation.performanceTargets.fullBlockGenerateMs` | Integer | `200` | Target full block generation time in milliseconds |
| `youthmind.recommendation.performanceTargets.eventPublishMs` | Integer | `10` | Target event publishing time in milliseconds |

#### Cache Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `youthmind.recommendation.cache.mappingRulesTtlHours` | Integer | `1` | TTL for mapping rules cache in hours |
| `youthmind.recommendation.cache.rvectorTtlMinutes` | Integer | `30` | TTL for R vector cache in minutes |
| `youthmind.recommendation.cache.feedBlocksTtlHours` | Integer | `1` | TTL for feed blocks cache in hours |
| `youthmind.recommendation.cache.shownPostsTtlDays` | Integer | `7` | TTL for shown posts cache in days |
| `youthmind.recommendation.cache.shownPostsMaxSize` | Integer | `500` | Maximum size of shown posts cache |

#### Example Configuration File

```yaml
# application.yml
youthmind:
  recommendation:
    enabled: true
    defaultBlockSize: 20
    blockExpiryHours: 1
    rvectorCacheTtlMinutes: 30
    eventQueueMaxDepth: 10000
    blockPrecalcEnabled: true
    fallbackToChronological: true
    minPostsPerEmotion: 3
    
    streams:
      rvectorUpdatesStream: "recommendation:rvector-updates"
      blockPrecalcStream: "recommendation:block-precalc"
      consumerGroup: "recommendation-engine-workers"
      maxRetries: 3
      retryDelayMs: 1000
    
    rateLimiting:
      maxRequestsPerMinute: 100
      burstCapacity: 10
    
    circuitBreaker:
      failureRateThreshold: 50
      waitDurationSeconds: 30
      slidingWindowSize: 10
      minimumCalls: 5
    
    performanceTargets:
      rVectorUpdateMs: 5
      topEmotionSelectMs: 1
      singleEmotionSelectMs: 50
      fullBlockGenerateMs: 200
      eventPublishMs: 10
    
    cache:
      mappingRulesTtlHours: 1
      rvectorTtlMinutes: 30
      feedBlocksTtlHours: 1
      shownPostsTtlDays: 7
      shownPostsMaxSize: 500
```

#### Environment-Specific Configuration

```yaml
# application-dev.yml
youthmind:
  recommendation:
    enabled: true
    defaultBlockSize: 10  # Smaller blocks for testing
    blockExpiryHours: 0.5  # Faster expiry for testing

# application-prod.yml
youthmind:
  recommendation:
    enabled: true
    defaultBlockSize: 20
    blockExpiryHours: 1
    cache:
      shownPostsTtlDays: 14  # Longer cache for production
```

---

### 7.2 Database Configuration

#### Required Tables

The recommendation engine requires the following database tables:

1. **`user_recommendation_vectors`** - Stores user R vectors (emotion weights)
2. **`feed_block_cache`** - Caches generated feed blocks
3. **`user_feed_state`** - Tracks user feed state and progress
4. **`recommendation_mapping_rules`** - Stores emotion mapping rules (20 predefined rules)

#### Migration Files

The recommendation engine uses Flyway for database migrations. Key migration files:

| Migration | Description | Key Changes |
|-----------|-------------|-------------|
| `V38__create_feed_block_cache.sql` | Creates `feed_block_cache` table | JSONB columns for post IDs and emotion distribution |
| `V39__create_user_feed_state.sql` | Creates `user_feed_state` table | JSONB columns for shown post IDs and block mappings |
| `V45__seed_recommendation_mapping_rules.sql` | Seeds 20 mapping rules | Predefined emotion combinations |
| **`V48__convert_r_vector_to_integer_scale.sql`** | **Converts R vector from DECIMAL to INTEGER** | **Critical migration: Converts [0.0000, 1.0000] → [2, 40]** |

#### V48 Migration Details

The V48 migration is critical as it converts the R vector data model from `DECIMAL [0.0000, 1.0000]` to `INTEGER [2, 40]` to match the specification.

**Conversion Formula**: `INTEGER = ROUND((DECIMAL * 38) + 2)`

**Migration Steps**:
1. Add temporary INTEGER columns
2. Convert existing DECIMAL data using formula
3. Drop old DECIMAL columns
4. Rename INTEGER columns to original names
5. Add NOT NULL constraints and defaults (10)
6. Add CHECK constraints for [2, 40] range

**Verification Queries** (included in migration):
- Check column types are INTEGER
- Verify all values are within [2, 40] range
- Ensure no NULL values exist
- Display conversion summary

#### Database Indexes

**`user_recommendation_vectors`**:
- Primary key: `id`
- Index: `idx_user_recommendation_vectors_user_id` on `(user_id)`
- Index: `idx_user_recommendation_vectors_user_version` on `(user_id, version)`

**`feed_block_cache`**:
- Primary key: `id`
- Index: `idx_feed_block_user_block` on `(user_id, block_number)`
- Index: `idx_feed_block_user_id` on `(user_id)`
- Index: `idx_feed_block_expires_at` on `(expires_at)`

**`user_feed_state`**:
- Primary key: `id`
- Unique constraint: `user_id`
- Index: `idx_user_feed_state_user_id` on `(user_id)`
- Index: `idx_user_feed_state_last_accessed` on `(last_accessed_at)`

#### Schema Overview

```sql
-- user_recommendation_vectors table
CREATE TABLE user_recommendation_vectors (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    version INTEGER NOT NULL,
    sad_weight INTEGER NOT NULL DEFAULT 10 CHECK (sad_weight >= 2 AND sad_weight <= 40),
    fear_weight INTEGER NOT NULL DEFAULT 10 CHECK (fear_weight >= 2 AND fear_weight <= 40),
    angry_weight INTEGER NOT NULL DEFAULT 10 CHECK (angry_weight >= 2 AND angry_weight <= 40),
    happy_weight INTEGER NOT NULL DEFAULT 10 CHECK (happy_weight >= 2 AND happy_weight <= 40),
    conflicted_weight INTEGER NOT NULL DEFAULT 10 CHECK (conflicted_weight >= 2 AND conflicted_weight <= 40),
    surprised_weight INTEGER NOT NULL DEFAULT 10 CHECK (surprised_weight >= 2 AND surprised_weight <= 40),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- feed_block_cache table
CREATE TABLE feed_block_cache (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    block_number INTEGER NOT NULL,
    post_ids JSONB NOT NULL,
    emotion_distribution JSONB NOT NULL,
    r_vector_version INTEGER NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_consumed BOOLEAN NOT NULL DEFAULT false,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- user_feed_state table
CREATE TABLE user_feed_state (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    current_block_number INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP NOT NULL,
    shown_post_ids JSONB NOT NULL DEFAULT '[]',
    current_r_vector_version INTEGER NOT NULL DEFAULT 1,
    total_blocks_generated INTEGER NOT NULL DEFAULT 0,
    block_r_vector_mapping JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

---

## 8. Code Examples

This section provides practical code examples for common operations in the Recommendation Engine.

### 8.1 Backend Examples

#### Creating a Post and Updating R Vector

```java
// PostService.java
@Autowired
private RVectorUpdateService rVectorUpdateService;

@Autowired
private PostRepository postRepository;

public Post createPost(UUID userId, PostCreateRequest request) {
    // 1. Create and save post
    Post post = new Post();
    post.setUserId(userId);
    post.setContent(request.getContent());
    Post savedPost = postRepository.save(post);
    
    // 2. Classify emotion (AI or user-provided)
    String emotionStr = request.getEmotion();
    if (emotionStr == null) {
        // AI classification
        EmotionClassificationResult result = aiEmotionClassificationService
            .classifyEmotion(savedPost.getContent());
        emotionStr = result.getEmotion();
    }
    savedPost.setEmotion(emotionStr);
    savedPost = postRepository.save(savedPost);
    
    // 3. Update R vector with +6 weight (posting behavior)
    try {
        ai.youthmind.enums.Emotion emotion = 
            ai.youthmind.enums.Emotion.fromString(emotionStr);
        if (emotion != null) {
            rVectorUpdateService.updateOnPost(userId, savedPost.getId(), emotion);
        }
    } catch (Exception e) {
        logger.error("Failed to update R vector: {}", e.getMessage());
        // Don't fail post creation
    }
    
    return savedPost;
}
```

#### Liking a Post and Updating R Vector

```java
// PostService.java or LikeService.java
@Autowired
private RVectorUpdateService rVectorUpdateService;

public PostDto toggleResonance(UUID userId, UUID postId) {
    // 1. Get post
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    
    // 2. Toggle like status
    boolean wasLiked = post.getResonances().contains(userId);
    if (wasLiked) {
        post.getResonances().remove(userId);
    } else {
        post.getResonances().add(userId);
    }
    post = postRepository.save(post);
    
    // 3. Update R vector only if it's a like action (not unlike)
    if (!wasLiked) {
        try {
            String emotionStr = post.getEmotion();
            if (emotionStr != null) {
                ai.youthmind.enums.Emotion emotion = 
                    ai.youthmind.enums.Emotion.fromString(emotionStr);
                if (emotion != null) {
                    // Update with +4 weight (liking behavior)
                    rVectorUpdateService.updateOnLike(userId, postId, emotion);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update R vector: {}", e.getMessage());
            // Don't fail like operation
        }
    }
    
    return postMapper.toDto(post);
}
```

#### Viewing a Post and Updating R Vector

```java
// PostService.java
@Autowired
private RVectorUpdateService rVectorUpdateService;

public PostDto trackPostViewWithDuration(UUID userId, UUID postId, Integer durationSeconds) {
    // 1. Get post
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    
    // 2. Update view count
    post.setViewCount(post.getViewCount() + 1);
    post = postRepository.save(post);
    
    // 3. Update R vector only if duration >= 15 seconds
    if (durationSeconds != null && durationSeconds >= 15) {
        try {
            String emotionStr = post.getEmotion();
            if (emotionStr != null) {
                ai.youthmind.enums.Emotion emotion = 
                    ai.youthmind.enums.Emotion.fromString(emotionStr);
                if (emotion != null) {
                    // Update with +2 weight (viewing behavior, ≥15s)
                    rVectorUpdateService.updateOnView(userId, postId, emotion, durationSeconds);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update R vector: {}", e.getMessage());
            // Don't fail view tracking
        }
    }
    
    return postMapper.toDto(post);
}
```

#### Generating a Feed Block

```java
// FeedBlockGeneratorService.java
@Autowired
private UserRecommendationVectorRepository vectorRepository;

@Autowired
private PostSelectionService postSelectionService;

@Autowired
private RecommendationMappingService mappingService;

public FeedBlockResponse generateFeedBlock(UUID userId, Integer blockNumber) {
    // 1. Get or create user feed state
    UserFeedState feedState = getOrCreateFeedState(userId);
    
    // 2. Determine R vector version (R(N-2) for block N)
    Integer rVectorVersion;
    if (blockNumber <= 2) {
        // Blocks 1-2 use default distribution
        rVectorVersion = null;
    } else {
        // Block N uses R(N-2)
        rVectorVersion = feedState.getRVectorVersionForBlock(blockNumber - 2);
    }
    
    // 3. Get R vector (if personalized)
    UserRecommendationVector rVector = null;
    if (rVectorVersion != null) {
        rVector = vectorRepository.findByUserIdAndVersion(userId, rVectorVersion)
            .orElse(null);
    }
    
    // 4. Get top 3 emotions
    List<String> topEmotions;
    if (rVector != null) {
        topEmotions = rVector.getTopThreeEmotions();
    } else {
        // Default distribution for blocks 1-2
        topEmotions = Arrays.asList("sad", "angry", "happy");
    }
    
    // 5. Get mapping rule
    RecommendationMappingRule mappingRule = mappingService
        .getMappingRuleForEmotions(topEmotions);
    
    // 6. Select posts for each emotion
    Map<String, List<Post>> postsByEmotion = new HashMap<>();
    for (String emotion : topEmotions) {
        int percentage = mappingRule.getPercentageForEmotion(emotion);
        int count = (int) Math.round(blockSize * percentage / 100.0);
        List<Post> posts = postSelectionService.selectPostsForEmotion(
            emotion, count, userId, feedState.getShownPostIds());
        postsByEmotion.put(emotion, posts);
    }
    
    // 7. Merge and shuffle posts
    List<Post> allPosts = mergeAndShufflePosts(postsByEmotion, mappingRule);
    
    // 8. Cache the block
    FeedBlockCache blockCache = new FeedBlockCache();
    blockCache.setUserId(userId);
    blockCache.setBlockNumber(blockNumber);
    blockCache.setPostIds(allPosts.stream().map(Post::getId).collect(Collectors.toList()));
    blockCache.setEmotionDistribution(calculateDistribution(postsByEmotion));
    blockCache.setRVectorVersion(rVectorVersion != null ? rVectorVersion : 0);
    blockCache.setGeneratedAt(LocalDateTime.now());
    blockCache.setExpiresAt(LocalDateTime.now().plusHours(1));
    feedBlockCacheRepository.save(blockCache);
    
    // 9. Build response
    FeedBlockResponse response = new FeedBlockResponse();
    response.setPosts(postMapper.toDtoList(allPosts));
    response.setBlockNumber(blockNumber);
    response.setBlockMetadata(buildBlockMetadata(blockCache, rVectorVersion));
    response.setNextBlockAvailable(true);
    
    return response;
}
```

#### Selecting Posts for an Emotion

```java
// PostSelectionService.java
public List<Post> selectPostsForEmotion(
        String emotion, 
        int count, 
        UUID userId, 
        Set<UUID> excludePostIds) {
    
    List<Post> selectedPosts = new ArrayList<>();
    
    // Time windows in priority order
    List<TimeWindow> timeWindows = Arrays.asList(
        new TimeWindow(0, 12),   // 0-12 hours
        new TimeWindow(12, 24),   // 12-24 hours
        new TimeWindow(24, 48),   // 24-48 hours
        new TimeWindow(48, 168)   // 2-7 days
    );
    
    // Popularity tiers
    int hotCount = (int) Math.round(count * 0.4);      // 40% hot
    int normalCount = (int) Math.round(count * 0.3);   // 30% normal
    int coldCount = count - hotCount - normalCount;     // 30% cold
    
    // Select posts by time window and popularity
    for (TimeWindow window : timeWindows) {
        if (selectedPosts.size() >= count) break;
        
        // Hot posts (≥100 views)
        if (selectedPosts.size() < hotCount) {
            List<Post> hotPosts = postRepository.findByEmotionAndTimeWindow(
                emotion, window.startHours, window.endHours, 100, Integer.MAX_VALUE,
                excludePostIds, hotCount - selectedPosts.size());
            selectedPosts.addAll(hotPosts);
        }
        
        // Normal posts (50-99 views)
        if (selectedPosts.size() < hotCount + normalCount) {
            List<Post> normalPosts = postRepository.findByEmotionAndTimeWindow(
                emotion, window.startHours, window.endHours, 50, 99,
                excludePostIds, normalCount - (selectedPosts.size() - hotCount));
            selectedPosts.addAll(normalPosts);
        }
        
        // Cold posts (<50 views)
        if (selectedPosts.size() < count) {
            List<Post> coldPosts = postRepository.findByEmotionAndTimeWindow(
                emotion, window.startHours, window.endHours, 0, 49,
                excludePostIds, count - selectedPosts.size());
            selectedPosts.addAll(coldPosts);
        }
    }
    
    return selectedPosts;
}
```

---

### 8.2 Frontend Examples

#### Fetching Personalized Feed (React/TypeScript)

```typescript
// hooks/useFeedBlocks.ts
import { useState, useEffect, useCallback } from 'react';
import { FeedService } from '../services/feedService';

export function useFeedBlocks() {
  const [blocks, setBlocks] = useState<Post[][]>([]);
  const [currentBlockNumber, setCurrentBlockNumber] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const loadBlock = useCallback(async (blockNumber: number) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await FeedService.getFeedBlock(blockNumber, 20, true);
      
      setBlocks((prev) => {
        const newBlocks = [...prev];
        newBlocks[blockNumber - 1] = response.data.posts;
        return newBlocks;
      });
      
      setCurrentBlockNumber(blockNumber);
      return response.data;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Failed to load feed block');
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBlock(1);
  }, [loadBlock]);

  return {
    blocks: blocks.flat(),
    currentBlockNumber,
    loading,
    error,
    loadNextBlock: () => loadBlock(currentBlockNumber + 1),
  };
}
```

#### Tracking Post View with Duration

```typescript
// hooks/usePostViewTracking.ts
import { useEffect, useRef } from 'react';
import { PostService } from '../services/postService';

export function usePostViewTracking(postId: string) {
  const startTimeRef = useRef<number | null>(null);

  useEffect(() => {
    // Start tracking when component mounts
    startTimeRef.current = Date.now();

    // Track view when component unmounts
    return () => {
      if (startTimeRef.current) {
        const durationSeconds = Math.floor(
          (Date.now() - startTimeRef.current) / 1000
        );
        
        // Only track if duration >= 15 seconds
        if (durationSeconds >= 15) {
          PostService.trackPostView(postId, durationSeconds).catch((error) => {
            console.error('Failed to track view:', error);
          });
        }
      }
    };
  }, [postId]);
}
```

#### Implementing Infinite Scroll

```typescript
// components/FeedView.tsx
import { useEffect, useRef, useCallback } from 'react';
import { useFeedBlocks } from '../hooks/useFeedBlocks';
import { PostCard } from './PostCard';

export function FeedView() {
  const {
    blocks,
    currentBlockNumber,
    loading,
    error,
    hasNextBlock,
    loadNextBlock,
  } = useFeedBlocks();

  const observerRef = useRef<IntersectionObserver | null>(null);
  const loadMoreRef = useCallback(
    (node: HTMLDivElement | null) => {
      if (loading) return;
      if (observerRef.current) observerRef.current.disconnect();

      observerRef.current = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && hasNextBlock) {
          loadNextBlock();
        }
      });

      if (node) observerRef.current.observe(node);
    },
    [loading, hasNextBlock, loadNextBlock]
  );

  return (
    <div className="feed-view">
      {blocks.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}
      
      {loading && <div className="loading">Loading...</div>}
      
      {hasNextBlock && (
        <div ref={loadMoreRef} className="load-more-trigger">
          Scroll for more
        </div>
      )}
    </div>
  );
}
```

#### Handling Errors

```typescript
// utils/errorHandler.ts
import axios from 'axios';

export function handleApiError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;
    
    switch (status) {
      case 400:
        return `Invalid request: ${message}`;
      case 401:
        return 'Authentication required. Please log in.';
      case 403:
        return 'Access denied. Admin privileges required.';
      case 404:
        return 'Resource not found.';
      case 500:
        return 'Server error. Please try again later.';
      default:
        return `Unexpected error: ${message}`;
    }
  }
  
  return 'An unexpected error occurred';
}

// Usage in component
try {
  const response = await FeedService.getFeedBlock(blockNumber);
} catch (error) {
  const errorMessage = handleApiError(error);
  setError(errorMessage);
  // Show error to user
}
```

---

## 9. Data Models

This section describes the data models used by the Recommendation Engine, including entities, DTOs, and their JSON representations.

### 9.1 UserRecommendationVector

The `UserRecommendationVector` entity represents a user's emotion-based recommendation vector (R vector).

#### Entity Structure

```java
@Entity
@Table(name = "user_recommendation_vectors")
public class UserRecommendationVector {
    private UUID id;
    private UUID userId;
    private Integer version;
    private Integer sadWeight;        // [2, 40]
    private Integer fearWeight;       // [2, 40]
    private Integer angryWeight;      // [2, 40]
    private Integer happyWeight;      // [2, 40]
    private Integer conflictedWeight; // [2, 40]
    private Integer surprisedWeight;  // [2, 40]
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Field Descriptions

| Field | Type | Range | Description |
|-------|------|-------|-------------|
| `id` | UUID | - | Primary key |
| `userId` | UUID | - | User ID (foreign key) |
| `version` | Integer | ≥1 | R vector version (increments on each update) |
| `sadWeight` | Integer | [2, 40] | Weight for sad emotion content |
| `fearWeight` | Integer | [2, 40] | Weight for fear emotion content |
| `angryWeight` | Integer | [2, 40] | Weight for angry emotion content |
| `happyWeight` | Integer | [2, 40] | Weight for happy emotion content |
| `conflictedWeight` | Integer | [2, 40] | Weight for conflicted emotion content |
| `surprisedWeight` | Integer | [2, 40] | Weight for surprised emotion content |
| `createdAt` | LocalDateTime | - | Creation timestamp |
| `updatedAt` | LocalDateTime | - | Last update timestamp |

#### Example JSON Representation

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "version": 5,
  "sadWeight": 12,
  "fearWeight": 8,
  "angryWeight": 15,
  "happyWeight": 18,
  "conflictedWeight": 10,
  "surprisedWeight": 9,
  "createdAt": "2024-01-10T08:00:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

#### Helper Methods

- `getTopThreeEmotions()`: Returns top 3 emotions by weight with tie-breaking
- `needsHalving()`: Checks if any dimension > 40
- `halveWeights()`: Halves all weights (except those at 2) with rounding
- `getAllEmotionWeights()`: Returns map of all emotion weights
- `getTotalWeight()`: Returns sum of all weights

---

### 9.2 FeedBlockCache

The `FeedBlockCache` entity caches generated feed blocks for fast retrieval.

#### Entity Structure

```java
@Entity
@Table(name = "feed_block_cache")
public class FeedBlockCache {
    private UUID id;
    private UUID userId;
    private Integer blockNumber;
    private List<UUID> postIds;                    // JSONB
    private Map<String, Integer> emotionDistribution; // JSONB
    private Integer rVectorVersion;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private Boolean isConsumed;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `userId` | UUID | User ID |
| `blockNumber` | Integer | Block sequence number (1-based) |
| `postIds` | List<UUID> | Ordered list of post IDs in this block (JSONB) |
| `emotionDistribution` | Map<String, Integer> | Emotion distribution counts (JSONB) |
| `rVectorVersion` | Integer | R vector version used to generate this block |
| `generatedAt` | LocalDateTime | When block was generated |
| `expiresAt` | LocalDateTime | When block expires |
| `isConsumed` | Boolean | Whether block has been consumed |
| `consumedAt` | LocalDateTime | When block was consumed |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

#### Example JSON Representation

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "blockNumber": 3,
  "postIds": [
    "123e4567-e89b-12d3-a456-426614174010",
    "123e4567-e89b-12d3-a456-426614174011",
    "123e4567-e89b-12d3-a456-426614174012"
  ],
  "emotionDistribution": {
    "happy": 8,
    "sad": 5,
    "angry": 3,
    "conflicted": 2,
    "surprised": 1,
    "fear": 1
  },
  "rVectorVersion": 15,
  "generatedAt": "2024-01-15T10:30:00",
  "expiresAt": "2024-01-16T10:30:00",
  "isConsumed": false,
  "consumedAt": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### 9.3 UserFeedState

The `UserFeedState` entity tracks user feed state and progress across sessions.

#### Entity Structure

```java
@Entity
@Table(name = "user_feed_state")
public class UserFeedState {
    private UUID id;
    private UUID userId;                    // UNIQUE
    private Integer currentBlockNumber;
    private LocalDateTime lastAccessedAt;
    private Set<UUID> shownPostIds;          // JSONB
    private Integer currentRVectorVersion;
    private Integer totalBlocksGenerated;
    private Map<Integer, Integer> blockRVectorMapping; // JSONB
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `userId` | UUID | User ID (unique, one state per user) |
| `currentBlockNumber` | Integer | Current block number (0-based) |
| `lastAccessedAt` | LocalDateTime | Last feed access timestamp |
| `shownPostIds` | Set<UUID> | Set of all post IDs shown to user (JSONB) |
| `currentRVectorVersion` | Integer | Current R vector version |
| `totalBlocksGenerated` | Integer | Total blocks generated for user |
| `blockRVectorMapping` | Map<Integer, Integer> | Maps block number → R vector version (JSONB) |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

#### Example JSON Representation

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "currentBlockNumber": 3,
  "lastAccessedAt": "2024-01-15T10:30:00",
  "shownPostIds": [
    "123e4567-e89b-12d3-a456-426614174010",
    "123e4567-e89b-12d3-a456-426614174011",
    "123e4567-e89b-12d3-a456-426614174012"
  ],
  "currentRVectorVersion": 15,
  "totalBlocksGenerated": 3,
  "blockRVectorMapping": {
    "1": 0,
    "2": 0,
    "3": 13
  },
  "createdAt": "2024-01-10T08:00:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### 9.4 DTOs

#### Request DTOs

**PostViewDurationRequest**:
```json
{
  "durationSeconds": 20
}
```

**FeedBlockRequest**:
```json
{
  "blockNumber": 1,
  "pageSize": 20,
  "prefetch": true
}
```

**RVectorUpdateRequest** (if implemented):
```json
{
  "emotion": "happy",
  "weight": 6
}
```

#### Response DTOs

**FeedBlockResponse**:
```json
{
  "success": true,
  "message": "Feed block retrieved successfully",
  "data": {
    "posts": [
      {
        "id": "123e4567-e89b-12d3-a456-426614174010",
        "content": "Post content here...",
        "emotion": "happy",
        "author": {
          "id": "123e4567-e89b-12d3-a456-426614174001",
          "username": "user123"
        },
        "resonance": 5,
        "viewCount": 42,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "blockNumber": 1,
    "blockMetadata": {
      "emotionDistribution": {
        "happy": 8,
        "sad": 5,
        "angry": 3,
        "conflicted": 2,
        "surprised": 1,
        "fear": 1
      },
      "rVectorVersion": 15,
      "generatedAt": "2024-01-15T10:30:00",
      "expiresAt": "2024-01-16T10:30:00",
      "isPersonalized": true
    },
    "nextBlockAvailable": true,
    "nextBlockNumber": 2,
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 20,
    "pageSize": 20,
    "hasNext": true,
    "isFirst": true,
    "isLast": false
  }
}
```

**RVectorResponse**:
```json
{
  "version": 5,
  "weights": {
    "sad": 12,
    "fear": 8,
    "angry": 15,
    "happy": 18,
    "conflicted": 10,
    "surprised": 9
  },
  "topEmotions": ["happy", "angry", "sad"],
  "updatedAt": "2024-01-15T10:30:00",
  "totalWeight": 72,
  "userId": "123e4567-e89b-12d3-a456-426614174001"
}
```

**BlockMetadata** (nested in FeedBlockResponse):
```json
{
  "emotionDistribution": {
    "happy": 8,
    "sad": 5,
    "angry": 3,
    "conflicted": 2,
    "surprised": 1,
    "fear": 1
  },
  "rVectorVersion": 15,
  "generatedAt": "2024-01-15T10:30:00",
  "expiresAt": "2024-01-16T10:30:00",
  "isPersonalized": true
}
```

---

## 10. Error Handling

This section covers common errors, error response formats, and best practices for handling errors in the Recommendation Engine.

### 10.1 Common Errors

#### Invalid User ID

**Error**: User ID is null, invalid UUID format, or user doesn't exist.

**HTTP Status**: `400 Bad Request` or `404 Not Found`

**Error Response**:
```json
{
  "success": false,
  "message": "Invalid user ID: user ID cannot be null",
  "data": null
}
```

**Backend Handling**:
```java
// RVectorUpdateService.java
public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
    if (userId == null) {
        throw new IllegalArgumentException("User ID cannot be null");
    }
    // ... rest of implementation
}
```

**Frontend Handling**:
```typescript
try {
  await FeedService.getFeedBlock(blockNumber);
} catch (error) {
  if (error.response?.status === 400) {
    // Handle invalid user ID
    console.error('Invalid user ID');
    // Redirect to login or show error message
  }
}
```

#### Missing R Vector

**Error**: R vector not found for user (new user or deleted vector).

**HTTP Status**: `404 Not Found`

**Error Response**:
```json
{
  "success": false,
  "message": "R vector not found for user",
  "data": null
}
```

**Backend Handling**:
```java
// RVectorUpdateService.java
private UserRecommendationVector getOrCreateCurrentVector(UUID userId) {
    Optional<UserRecommendationVector> current = vectorRepository
        .findLatestByUserId(userId);
    
    if (current.isEmpty()) {
        // Create new R vector with default values (all = 10)
        return initializeForNewUser(userId);
    }
    
    return current.get();
}
```

**Frontend Handling**:
```typescript
// Frontend should handle 404 gracefully
try {
  const rVector = await RVectorService.getRVector();
} catch (error) {
  if (error.response?.status === 404) {
    // R vector doesn't exist yet - this is normal for new users
    // System will create one on first interaction
    console.log('R vector not found - will be created on first interaction');
  }
}
```

#### Invalid Emotion

**Error**: Emotion string doesn't match valid emotion enum values.

**Valid Emotions**: `sad`, `fear`, `angry`, `happy`, `conflicted`, `surprised`

**HTTP Status**: `400 Bad Request`

**Error Response**:
```json
{
  "success": false,
  "message": "Invalid emotion: 'invalid_emotion'. Valid emotions are: sad, fear, angry, happy, conflicted, surprised",
  "data": null
}
```

**Backend Handling**:
```java
// RVectorUpdateService.java
public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
    if (emotion == null) {
        throw new IllegalArgumentException("Emotion cannot be null");
    }
    
    // Emotion enum validation happens at conversion
    try {
        Emotion emotionEnum = Emotion.fromString(emotionString);
        if (emotionEnum == null) {
            throw new IllegalArgumentException("Invalid emotion: " + emotionString);
        }
    } catch (IllegalArgumentException e) {
        logger.warn("Invalid emotion provided: {}", emotionString);
        throw e;
    }
}
```

**Frontend Handling**:
```typescript
// Validate emotion before sending request
const validEmotions = ['sad', 'fear', 'angry', 'happy', 'conflicted', 'surprised'];

function validateEmotion(emotion: string): boolean {
  return validEmotions.includes(emotion.toLowerCase());
}

// Use validation
if (!validateEmotion(postEmotion)) {
  console.error('Invalid emotion:', postEmotion);
  // Handle error or use default
}
```

#### Block Generation Failures

**Error**: Feed block generation fails due to insufficient posts, database errors, or service unavailability.

**HTTP Status**: `500 Internal Server Error` or `503 Service Unavailable`

**Error Response**:
```json
{
  "success": false,
  "message": "Failed to generate feed block: Insufficient posts available",
  "data": null
}
```

**Backend Handling**:
```java
// FeedBlockGeneratorService.java
public FeedBlockResponse generateFeedBlock(UUID userId, Integer blockNumber) {
    try {
        // ... block generation logic
        
        if (allPosts.size() < minBlockSize) {
            logger.warn("Insufficient posts for block {} - found: {}, required: {}", 
                      blockNumber, allPosts.size(), minBlockSize);
            
            // Fallback to chronological feed if enabled
            if (properties.isFallbackToChronological()) {
                return generateChronologicalFeed(userId, blockNumber);
            }
            
            throw new InsufficientContentException(
                "Insufficient posts available for block generation");
        }
        
        return buildFeedBlockResponse(allPosts, blockNumber, rVectorVersion);
        
    } catch (Exception e) {
        logger.error("Failed to generate feed block for user {}: {}", userId, e.getMessage(), e);
        
        // Fallback mechanism
        if (properties.isFallbackToChronological()) {
            return generateChronologicalFeed(userId, blockNumber);
        }
        
        throw new FeedGenerationException("Failed to generate feed block", e);
    }
}
```

**Frontend Handling**:
```typescript
// Retry with exponential backoff
async function fetchFeedBlockWithRetry(blockNumber: number, retries = 3): Promise<FeedBlockResponse> {
  for (let i = 0; i < retries; i++) {
    try {
      return await FeedService.getFeedBlock(blockNumber);
    } catch (error) {
      if (i === retries - 1) throw error;
      
      // Exponential backoff: 1s, 2s, 4s
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, i) * 1000));
    }
  }
  throw new Error('Failed to fetch feed block after retries');
}
```

#### Error Response Formats

All error responses follow a consistent format:

```json
{
  "success": false,
  "message": "Human-readable error message",
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "details": "Additional error details",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

**Common Error Codes**:
- `INVALID_USER_ID`: User ID is invalid or missing
- `R_VECTOR_NOT_FOUND`: R vector doesn't exist for user
- `INVALID_EMOTION`: Emotion value is invalid
- `INSUFFICIENT_CONTENT`: Not enough posts to generate block
- `BLOCK_GENERATION_FAILED`: Block generation failed
- `SERVICE_UNAVAILABLE`: Recommendation service is temporarily unavailable
- `RATE_LIMIT_EXCEEDED`: Too many requests
- `AUTHENTICATION_REQUIRED`: User not authenticated

---

### 10.2 Best Practices

#### Graceful Error Handling

**Backend Best Practices**:

1. **Never fail user operations due to R vector errors**:
```java
// PostService.java - Post creation
try {
    rVectorUpdateService.updateOnPost(userId, postId, emotion);
    logger.info("R vector updated successfully");
} catch (Exception e) {
    logger.error("Failed to update R vector: {}", e.getMessage(), e);
    // Don't fail post creation - R vector update is secondary
}
```

2. **Use fallback mechanisms**:
```java
// FeedBlockGeneratorService.java
if (rVector == null || !rVector.hasNonZeroWeights()) {
    logger.warn("R vector not available - using default distribution");
    return generateDefaultFeedBlock(userId, blockNumber);
}
```

3. **Validate inputs early**:
```java
// RVectorUpdateService.java
public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
    // Validate all inputs before processing
    if (userId == null) {
        throw new IllegalArgumentException("User ID cannot be null");
    }
    if (emotion == null) {
        throw new IllegalArgumentException("Emotion cannot be null");
    }
    if (weight <= 0) {
        throw new IllegalArgumentException("Weight must be positive");
    }
    // ... rest of implementation
}
```

**Frontend Best Practices**:

1. **Show user-friendly error messages**:
```typescript
function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message || error.message;
    
    // Map technical errors to user-friendly messages
    if (message.includes('R vector not found')) {
      return 'Your preferences are being set up. Please try again in a moment.';
    }
    if (message.includes('Insufficient posts')) {
      return 'Not enough content available. Please try again later.';
    }
    if (error.response?.status === 500) {
      return 'Something went wrong. Please try again later.';
    }
    
    return message;
  }
  return 'An unexpected error occurred';
}
```

2. **Implement retry logic**:
```typescript
async function retryOperation<T>(
  operation: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await operation();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      
      // Don't retry on client errors (4xx)
      if (axios.isAxiosError(error) && error.response?.status && error.response.status < 500) {
        throw error;
      }
      
      await new Promise(resolve => setTimeout(resolve, delay * (i + 1)));
    }
  }
  throw new Error('Operation failed after retries');
}
```

3. **Handle errors at component level**:
```typescript
// components/FeedView.tsx
export function FeedView() {
  const [error, setError] = useState<string | null>(null);
  
  const handleError = useCallback((err: unknown) => {
    const message = getErrorMessage(err);
    setError(message);
    
    // Log error for debugging
    console.error('Feed error:', err);
    
    // Report to error tracking service
    errorTrackingService.reportError(err);
  }, []);
  
  return (
    <div>
      {error && (
        <div className="error-banner">
          <p>{error}</p>
          <button onClick={() => { setError(null); loadFeed(); }}>
            Retry
          </button>
        </div>
      )}
      {/* Feed content */}
    </div>
  );
}
```

#### Retry Strategies

**Exponential Backoff**:
```typescript
async function fetchWithExponentialBackoff<T>(
  fn: () => Promise<T>,
  maxRetries: number = 3
): Promise<T> {
  let delay = 1000; // Start with 1 second
  
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      
      await new Promise(resolve => setTimeout(resolve, delay));
      delay *= 2; // Double the delay each retry
    }
  }
  
  throw new Error('Max retries exceeded');
}
```

**Circuit Breaker Pattern** (Backend):
```java
// RecommendationEngineProperties.java
@ConfigurationProperties(prefix = "youthmind.recommendation.circuitBreaker")
public class CircuitBreaker {
    private Integer failureRateThreshold = 50;  // 50% failure rate
    private Integer waitDurationSeconds = 30;  // Wait 30s before retry
    private Integer slidingWindowSize = 10;    // Last 10 calls
    private Integer minimumCalls = 5;          // Need 5 calls before checking
}
```

#### Fallback Mechanisms

**Chronological Feed Fallback**:
```java
// FeedBlockGeneratorService.java
public FeedBlockResponse generateFeedBlock(UUID userId, Integer blockNumber) {
    try {
        // Try personalized feed
        return generatePersonalizedFeedBlock(userId, blockNumber);
    } catch (Exception e) {
        logger.warn("Personalized feed generation failed: {}", e.getMessage());
        
        // Fallback to chronological feed
        if (properties.isFallbackToChronological()) {
            return generateChronologicalFeed(userId, blockNumber);
        }
        
        throw e;
    }
}
```

**Default R Vector Fallback**:
```java
// FeedBlockGeneratorService.java
private UserRecommendationVector getRVectorForBlock(UUID userId, Integer blockNumber) {
    if (blockNumber <= 2) {
        // Blocks 1-2 use default distribution
        return null;
    }
    
    Integer rVectorVersion = feedState.getRVectorVersionForBlock(blockNumber - 2);
    
    if (rVectorVersion == null) {
        logger.warn("R vector version not found for block {} - using default", blockNumber);
        return null; // Will use default distribution
    }
    
    return vectorRepository.findByUserIdAndVersion(userId, rVectorVersion)
        .orElse(null);
}
```

---

## 11. Performance Considerations

This section covers performance optimization strategies, caching, pre-computation, and best practices for achieving optimal performance.

### 11.1 Caching Strategy

#### R Vector Caching

**Cache TTL**: 30 minutes (configurable via `youthmind.recommendation.cache.rvectorTtlMinutes`)

**Cache Key Format**: `rvector:{userId}:{version}`

**Implementation**:
```java
// RVectorUpdateService.java
@Cacheable(value = "rvector", key = "#userId + ':' + #version")
public UserRecommendationVector getRVectorByVersion(UUID userId, Integer version) {
    return vectorRepository.findByUserIdAndVersion(userId, version)
        .orElseThrow(() -> new ResourceNotFoundException("R vector not found"));
}

// Cache eviction on update
@CacheEvict(value = "rvector", key = "#userId + ':*'")
public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
    // ... update logic
}
```

**Frontend Caching**:
```typescript
// Cache R vector in memory for session
class RVectorCache {
  private static cache = new Map<string, { data: RVectorResponse; timestamp: number }>();
  private static TTL = 30 * 60 * 1000; // 30 minutes

  static get(userId: string): RVectorResponse | null {
    const cached = this.cache.get(userId);
    if (!cached) return null;
    
    if (Date.now() - cached.timestamp > this.TTL) {
      this.cache.delete(userId);
      return null;
    }
    
    return cached.data;
  }

  static set(userId: string, data: RVectorResponse): void {
    this.cache.set(userId, { data, timestamp: Date.now() });
  }
}
```

#### Feed Block Caching

**Cache TTL**: 1 hour (configurable via `youthmind.recommendation.cache.feedBlocksTtlHours`)

**Cache Storage**: Database (`feed_block_cache` table)

**Cache Key**: `{userId}:{blockNumber}`

**Implementation**:
```java
// FeedBlockGeneratorService.java
public FeedBlockResponse generateFeedBlock(UUID userId, Integer blockNumber) {
    // 1. Check cache first
    Optional<FeedBlockCache> cached = feedBlockCacheRepository
        .findByUserIdAndBlockNumber(userId, blockNumber);
    
    if (cached.isPresent() && cached.get().isValid()) {
        logger.debug("Returning cached block {} for user {}", blockNumber, userId);
        return buildResponseFromCache(cached.get());
    }
    
    // 2. Generate new block
    FeedBlockResponse response = generateNewBlock(userId, blockNumber);
    
    // 3. Cache the block
    cacheFeedBlock(userId, blockNumber, response);
    
    return response;
}
```

**Cache Invalidation**:
```java
// Invalidate cache when user interacts (updates R vector)
@CacheEvict(value = "feedBlocks", key = "#userId + ':*'")
public void invalidateUserFeedCache(UUID userId) {
    feedBlockCacheRepository.deleteByUserId(userId);
}
```

#### Cache TTLs Summary

| Cache Type | TTL | Config Property |
|------------|-----|-----------------|
| R Vector | 30 minutes | `youthmind.recommendation.cache.rvectorTtlMinutes` |
| Feed Blocks | 1 hour | `youthmind.recommendation.cache.feedBlocksTtlHours` |
| Mapping Rules | 1 hour | `youthmind.recommendation.cache.mappingRulesTtlHours` |
| Shown Posts | 7 days | `youthmind.recommendation.cache.shownPostsTtlDays` |

---

### 11.2 Pre-Computation

#### How Pre-Computation Improves Performance

**Problem**: Generating a feed block can take 100-200ms, causing noticeable delay when user requests next block.

**Solution**: Pre-compute the next block while user is viewing current block.

**Performance Improvement**:
- **Without pre-computation**: User waits 100-200ms for block generation
- **With pre-computation**: Block is ready instantly (0ms wait time)

#### When Blocks Are Pre-Computed

**Trigger**: When user starts viewing Block N, system pre-computes Block N+1.

**Implementation**:
```java
// FeedBlockGeneratorService.java
public FeedBlockResponse generateFeedBlock(UUID userId, Integer blockNumber) {
    // Generate current block
    FeedBlockResponse response = generateNewBlock(userId, blockNumber);
    
    // Pre-compute next block asynchronously
    if (properties.isBlockPrecalcEnabled()) {
        Integer nextBlockNumber = blockNumber + 1;
        CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Pre-computing block {} for user {}", nextBlockNumber, userId);
                generateNewBlock(userId, nextBlockNumber);
                logger.debug("Pre-computed block {} ready", nextBlockNumber);
            } catch (Exception e) {
                logger.warn("Failed to pre-compute block {}: {}", nextBlockNumber, e.getMessage());
            }
        });
    }
    
    return response;
}
```

**Frontend Trigger**:
```typescript
// Trigger pre-computation when user scrolls near bottom
useEffect(() => {
  const handleScroll = () => {
    const scrollPosition = window.innerHeight + window.scrollY;
    const documentHeight = document.documentElement.scrollHeight;
    const threshold = documentHeight - 500; // 500px before bottom
    
    if (scrollPosition >= threshold && hasNextBlock) {
      // Trigger prefetch
      FeedService.getFeedBlock(currentBlockNumber + 1, 20, true)
        .catch(err => console.error('Prefetch failed:', err));
    }
  };
  
  window.addEventListener('scroll', handleScroll);
  return () => window.removeEventListener('scroll', handleScroll);
}, [currentBlockNumber, hasNextBlock]);
```

#### Performance Metrics

**Target Performance** (from `RecommendationEngineProperties`):

| Operation | Target Time | Actual (Typical) |
|-----------|-------------|-----------------|
| R Vector Update | 5ms | 3-8ms |
| Top Emotion Selection | 1ms | <1ms |
| Single Emotion Post Selection | 50ms | 30-60ms |
| Full Block Generation | 200ms | 150-250ms |
| Event Publishing | 10ms | 5-15ms |

**Monitoring**:
```java
// RVectorUpdateService.java
@Timed(value = "rvector.update", description = "R vector update time")
public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
    long startTime = System.currentTimeMillis();
    
    try {
        // ... update logic
        return updatedVector;
    } finally {
        long duration = System.currentTimeMillis() - startTime;
        metricsService.recordRVectorUpdateTime(duration);
        
        if (duration > properties.getPerformanceTargets().getRVectorUpdateMs()) {
            logger.warn("R vector update exceeded target: {}ms (target: {}ms)", 
                      duration, properties.getPerformanceTargets().getRVectorUpdateMs());
        }
    }
}
```

---

### 11.3 Optimization Tips

#### Best Practices for Frontend

1. **Batch API Calls**:
```typescript
// Don't call API for each post view
// Instead, batch views and send periodically
class ViewTracker {
  private views: Map<string, number> = new Map();
  private flushInterval = 5000; // 5 seconds

  trackView(postId: string, duration: number) {
    this.views.set(postId, duration);
  }

  startFlush() {
    setInterval(() => {
      if (this.views.size > 0) {
        this.flush();
      }
    }, this.flushInterval);
  }

  private async flush() {
    const views = Array.from(this.views.entries());
    this.views.clear();
    
    // Batch send all views
    await PostService.batchTrackViews(views);
  }
}
```

2. **Use Intersection Observer for Lazy Loading**:
```typescript
// Only load posts when they're about to be visible
const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const postId = entry.target.dataset.postId;
      // Start tracking view
      startViewTracking(postId);
    }
  });
});

// Observe all post elements
postElements.forEach(el => observer.observe(el));
```

3. **Debounce View Tracking**:
```typescript
// Don't track view on every scroll event
const debouncedTrackView = debounce((postId: string, duration: number) => {
  PostService.trackPostView(postId, duration);
}, 1000); // Wait 1 second after last scroll
```

#### Reducing API Calls

1. **Cache Feed Blocks Locally**:
```typescript
// Cache blocks in localStorage
class FeedBlockCache {
  private static KEY = 'feed_blocks_cache';
  private static TTL = 60 * 60 * 1000; // 1 hour

  static set(blockNumber: number, data: FeedBlockResponse): void {
    const cache = this.getCache();
    cache[blockNumber] = {
      data,
      timestamp: Date.now()
    };
    localStorage.setItem(this.KEY, JSON.stringify(cache));
  }

  static get(blockNumber: number): FeedBlockResponse | null {
    const cache = this.getCache();
    const cached = cache[blockNumber];
    
    if (!cached) return null;
    if (Date.now() - cached.timestamp > this.TTL) {
      delete cache[blockNumber];
      localStorage.setItem(this.KEY, JSON.stringify(cache));
      return null;
    }
    
    return cached.data;
  }

  private static getCache(): Record<number, { data: FeedBlockResponse; timestamp: number }> {
    const cached = localStorage.getItem(this.KEY);
    return cached ? JSON.parse(cached) : {};
  }
}
```

2. **Prefetch Next Block**:
```typescript
// Prefetch next block when user is 80% through current block
useEffect(() => {
  if (currentBlockNumber > 0 && hasNextBlock) {
    // Prefetch next block
    FeedService.getFeedBlock(currentBlockNumber + 1, 20, true)
      .then(response => {
        // Cache the prefetched block
        FeedBlockCache.set(currentBlockNumber + 1, response.data);
      })
      .catch(err => console.error('Prefetch failed:', err));
  }
}, [currentBlockNumber, hasNextBlock]);
```

#### Efficient Data Loading

1. **Use Pagination**:
```typescript
// Load blocks incrementally
const [blocks, setBlocks] = useState<Post[][]>([]);

const loadNextBlock = async () => {
  const nextBlockNumber = blocks.length + 1;
  const response = await FeedService.getFeedBlock(nextBlockNumber);
  setBlocks(prev => [...prev, response.data.posts]);
};
```

2. **Virtual Scrolling for Large Feeds**:
```typescript
// Use virtualization for large lists
import { useVirtualizer } from '@tanstack/react-virtual';

export function VirtualizedFeed({ posts }: { posts: Post[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: posts.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 200,
    overscan: 5, // Render 5 extra items
  });

  return (
    <div ref={parentRef} style={{ height: '100vh', overflow: 'auto' }}>
      <div style={{ height: `${virtualizer.getTotalSize()}px`, position: 'relative' }}>
        {virtualizer.getVirtualItems().map(virtualItem => (
          <div
            key={virtualItem.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualItem.size}px`,
              transform: `translateY(${virtualItem.start}px)`,
            }}
          >
            <PostCard post={posts[virtualItem.index]} />
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

## 12. Testing

This section covers testing strategies for the Recommendation Engine, including unit tests, integration tests, and end-to-end testing.

### 12.1 Testing R Vector Updates

#### Verifying R Vectors Update Correctly

**Test Case**: R vector updates with correct weight when user creates a post.

```java
@Test
public void testRVectorUpdateOnPost() {
    // Given
    UUID userId = UUID.randomUUID();
    Emotion emotion = Emotion.HAPPY;
    
    // Initialize R vector
    rVectorUpdateService.initializeForNewUser(userId);
    UserRecommendationVector initial = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    
    // Verify initial state (all = 10)
    assertEquals(10, initial.getHappyWeight());
    
    // When: User creates a post with happy emotion
    rVectorUpdateService.updateOnPost(userId, UUID.randomUUID(), emotion);
    
    // Then: Happy weight should increase by 6
    UserRecommendationVector updated = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    
    assertEquals(16, updated.getHappyWeight()); // 10 + 6
    assertEquals(initial.getVersion() + 1, updated.getVersion());
}
```

#### Testing Behavioral Weights

**Test Case**: Verify different behaviors apply correct weights.

```java
@Test
public void testBehavioralWeights() {
    UUID userId = UUID.randomUUID();
    Emotion emotion = Emotion.SAD;
    
    rVectorUpdateService.initializeForNewUser(userId);
    
    // Test posting weight (+6)
    rVectorUpdateService.updateOnPost(userId, UUID.randomUUID(), emotion);
    UserRecommendationVector afterPost = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    assertEquals(16, afterPost.getSadWeight()); // 10 + 6
    
    // Test liking weight (+4)
    rVectorUpdateService.updateOnLike(userId, UUID.randomUUID(), emotion);
    UserRecommendationVector afterLike = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    assertEquals(20, afterLike.getSadWeight()); // 16 + 4
    
    // Test viewing weight (+2, only if >= 15s)
    rVectorUpdateService.updateOnView(userId, UUID.randomUUID(), emotion, 20);
    UserRecommendationVector afterView = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    assertEquals(22, afterView.getSadWeight()); // 20 + 2
    
    // Test viewing with duration < 15s (should not update)
    rVectorUpdateService.updateOnView(userId, UUID.randomUUID(), emotion, 10);
    UserRecommendationVector afterShortView = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    assertEquals(22, afterShortView.getSadWeight()); // No change
}
```

#### Testing Halving Mechanism

**Test Case**: Verify halving triggers when any dimension > 40.

```java
@Test
public void testHalvingMechanism() {
    UUID userId = UUID.randomUUID();
    Emotion emotion = Emotion.ANGRY;
    
    // Create R vector with angry weight = 38
    UserRecommendationVector vector = new UserRecommendationVector();
    vector.setUserId(userId);
    vector.setVersion(1);
    vector.initializeDefaults();
    vector.setAngryWeight(38);
    vector = vectorRepository.save(vector);
    
    // Add 6 (posting) - should trigger halving (38 + 6 = 44 > 40)
    rVectorUpdateService.updateOnPost(userId, UUID.randomUUID(), emotion);
    
    UserRecommendationVector updated = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    
    // After halving: 44 / 2 = 22
    assertEquals(22, updated.getAngryWeight());
    
    // Verify value-2 protection
    vector.setAngryWeight(2);
    vector.setVersion(2);
    vector = vectorRepository.save(vector);
    
    // Add 6 - should not halve value at 2
    rVectorUpdateService.updateOnPost(userId, UUID.randomUUID(), emotion);
    
    UserRecommendationVector protected = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    
    // Value at 2 should remain 2 (protected)
    // But other values should be halved if any > 40
    // In this case, only angry was > 40, so only angry is halved
    // But since angry was at 2, it stays at 2
    // However, if we add 6 to 2, we get 8, which is < 40, so no halving
    assertEquals(8, protected.getAngryWeight()); // 2 + 6 = 8
}
```

---

### 12.2 Testing Feed Generation

#### Testing Block Generation

**Test Case**: Verify feed blocks are generated correctly.

```java
@Test
public void testFeedBlockGeneration() {
    // Given: User with R vector
    UUID userId = UUID.randomUUID();
    UserRecommendationVector rVector = createTestRVector(userId, 
        Map.of("happy", 20, "sad", 15, "angry", 12));
    
    // When: Generate block 3
    FeedBlockResponse response = feedBlockGeneratorService
        .generateFeedBlock(userId, 3);
    
    // Then: Verify block structure
    assertNotNull(response);
    assertEquals(3, response.getBlockNumber());
    assertNotNull(response.getPosts());
    assertTrue(response.getPosts().size() > 0);
    assertNotNull(response.getBlockMetadata());
    assertTrue(response.getBlockMetadata().getIsPersonalized());
}
```

#### Testing R(N-2) Lag

**Test Case**: Verify Block N uses R vector version (N-2).

```java
@Test
public void testRVectorLag() {
    UUID userId = UUID.randomUUID();
    
    // Create R vectors with versions 1, 2, 3, 4, 5
    for (int i = 1; i <= 5; i++) {
        UserRecommendationVector vector = createTestRVector(userId, i);
        vectorRepository.save(vector);
    }
    
    // Block 3 should use R vector version 1 (3 - 2 = 1)
    FeedBlockResponse block3 = feedBlockGeneratorService.generateFeedBlock(userId, 3);
    assertEquals(1, block3.getBlockMetadata().getRVectorVersion());
    
    // Block 4 should use R vector version 2 (4 - 2 = 2)
    FeedBlockResponse block4 = feedBlockGeneratorService.generateFeedBlock(userId, 4);
    assertEquals(2, block4.getBlockMetadata().getRVectorVersion());
    
    // Block 5 should use R vector version 3 (5 - 2 = 3)
    FeedBlockResponse block5 = feedBlockGeneratorService.generateFeedBlock(userId, 5);
    assertEquals(3, block5.getBlockMetadata().getRVectorVersion());
}
```

#### Testing Pre-Computation

**Test Case**: Verify next block is pre-computed.

```java
@Test
public void testBlockPreComputation() {
    UUID userId = UUID.randomUUID();
    
    // Generate block 1
    FeedBlockResponse block1 = feedBlockGeneratorService.generateFeedBlock(userId, 1);
    
    // Wait a bit for pre-computation
    Thread.sleep(100);
    
    // Verify block 2 was pre-computed
    Optional<FeedBlockCache> cachedBlock2 = feedBlockCacheRepository
        .findByUserIdAndBlockNumber(userId, 2);
    
    assertTrue(cachedBlock2.isPresent());
    assertFalse(cachedBlock2.get().isExpired());
    assertFalse(cachedBlock2.get().wasConsumed());
}
```

---

### 12.3 Integration Testing

#### End-to-End Flow Testing

**Test Case**: Complete user interaction flow.

```java
@Test
public void testEndToEndFlow() {
    // 1. Create user
    UUID userId = UUID.randomUUID();
    User user = createTestUser(userId);
    
    // 2. User creates a post (happy emotion)
    Post post = postService.createPost(userId, new PostCreateRequest()
        .setContent("I'm feeling great today!")
        .setEmotion("happy"));
    
    // 3. Verify R vector was updated
    UserRecommendationVector rVector = vectorRepository
        .findLatestByUserId(userId).orElseThrow();
    assertEquals(16, rVector.getHappyWeight()); // 10 + 6
    
    // 4. User likes a post (sad emotion)
    Post sadPost = createTestPost("sad");
    postService.toggleResonance(userId, sadPost.getId());
    
    // 5. Verify R vector was updated
    rVector = vectorRepository.findLatestByUserId(userId).orElseThrow();
    assertEquals(14, rVector.getSadWeight()); // 10 + 4
    
    // 6. User views a post for 20 seconds (angry emotion)
    Post angryPost = createTestPost("angry");
    postService.trackPostViewWithDuration(angryPost.getId(), userId, 20);
    
    // 7. Verify R vector was updated
    rVector = vectorRepository.findLatestByUserId(userId).orElseThrow();
    assertEquals(12, rVector.getAngryWeight()); // 10 + 2
    
    // 8. Generate feed block 3 (should use R vector version 1)
    FeedBlockResponse feed = feedBlockGeneratorService.generateFeedBlock(userId, 3);
    
    // 9. Verify feed contains posts matching top emotions
    List<String> topEmotions = rVector.getTopThreeEmotions();
    // Feed should contain posts matching these emotions
    assertTrue(feed.getPosts().stream()
        .anyMatch(p -> topEmotions.contains(p.getEmotion())));
}
```

#### API Testing Examples

**Test Case**: Test feed block API endpoint.

```java
@Test
public void testFeedBlockApi() throws Exception {
    // Given: Authenticated user
    UUID userId = UUID.randomUUID();
    String token = generateAuthToken(userId);
    
    // When: Request feed block
    mockMvc.perform(get("/v1/posts/feed/blocks")
            .param("blockNumber", "1")
            .param("pageSize", "20")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.blockNumber").value(1))
        .andExpect(jsonPath("$.data.posts").isArray())
        .andExpect(jsonPath("$.data.blockMetadata").exists());
}
```

---

## 13. Troubleshooting

This section covers common issues, debugging tips, and solutions for problems in the Recommendation Engine.

### 13.1 Common Issues

#### R Vectors Not Updating

**Symptoms**:
- R vector weights remain at default values (10)
- User interactions don't affect recommendations
- R vector version doesn't increment

**Possible Causes**:
1. Recommendation engine is disabled
2. Event consumer is not running
3. Redis Streams connection issue
4. Exception in R vector update service

**Solutions**:

1. **Check if recommendation engine is enabled**:
```yaml
# application.yml
youthmind:
  recommendation:
    enabled: true  # Must be true
```

2. **Verify event consumer is running**:
```java
// Check logs for RecommendationEventConsumer
// Should see: "Started RecommendationEventConsumer"
```

3. **Check Redis connection**:
```bash
# Test Redis connection
redis-cli ping
# Should return: PONG
```

4. **Check for exceptions in logs**:
```bash
# Search logs for R vector update errors
grep "Failed to update R vector" backend.log
```

5. **Manually trigger R vector update** (for testing):
```java
// In a test or admin endpoint
rVectorUpdateService.updateOnPost(userId, postId, Emotion.HAPPY);
```

#### Blocks Not Generating

**Symptoms**:
- Feed returns empty or error
- Blocks fail to generate
- Timeout errors

**Possible Causes**:
1. Insufficient posts in database
2. Database connection issues
3. Cache expiration
4. Service unavailable

**Solutions**:

1. **Check post availability**:
```sql
-- Check if there are enough posts
SELECT emotion, COUNT(*) 
FROM posts 
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY emotion;
```

2. **Check database connection**:
```java
// Verify database is accessible
@Autowired
private DataSource dataSource;

public void testConnection() {
    try (Connection conn = dataSource.getConnection()) {
        // Connection successful
    } catch (SQLException e) {
        // Connection failed
    }
}
```

3. **Clear and regenerate cache**:
```java
// Clear feed block cache
feedBlockCacheRepository.deleteByUserId(userId);

// Regenerate block
feedBlockGeneratorService.generateFeedBlock(userId, blockNumber);
```

4. **Check service health**:
```bash
# Check application health endpoint
curl http://localhost:8080/actuator/health
```

#### Pre-Computation Not Working

**Symptoms**:
- Next block not available immediately
- Blocks take time to generate
- No pre-computed blocks in cache

**Possible Causes**:
1. Pre-computation is disabled
2. Background thread pool exhausted
3. Cache write failures

**Solutions**:

1. **Enable pre-computation**:
```yaml
youthmind:
  recommendation:
    blockPrecalcEnabled: true
```

2. **Check thread pool configuration**:
```java
// Verify async executor is configured
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
```

3. **Check cache write permissions**:
```sql
-- Verify feed_block_cache table is writable
INSERT INTO feed_block_cache (user_id, block_number, ...) 
VALUES (...);
```

#### Event Processing Not Working

**Symptoms**:
- Events published but not processed
- R vectors not updating from events
- Events accumulating in Redis Stream

**Possible Causes**:
1. Event consumer not subscribed
2. Consumer group not created
3. Message deserialization failures

**Solutions**:

1. **Verify consumer subscription**:
```java
// Check logs for subscription confirmation
// Should see: "Subscribed to stream: recommendation:rvector-updates"
```

2. **Create consumer group manually** (if needed):
```bash
redis-cli XGROUP CREATE recommendation:rvector-updates recommendation-engine-workers 0
```

3. **Check for deserialization errors**:
```bash
# Check logs for deserialization errors
grep "Failed to deserialize event" backend.log
```

4. **Manually process pending messages**:
```bash
# Check pending messages
redis-cli XPENDING recommendation:rvector-updates recommendation-engine-workers
```

---

### 13.2 Debugging Tips

#### Logging Locations

**Key Log Points**:

1. **R Vector Updates**:
```java
// RVectorUpdateService.java
log.debug("Updating R vector for user {} - emotion: {}, weight: +{}", 
         userId, emotion, weight);
log.info("R vector updated - user: {}, version: {}, weights: {}", 
        userId, newVersion, newVector.getAllEmotionWeights());
```

2. **Feed Block Generation**:
```java
// FeedBlockGeneratorService.java
log.debug("Generating feed block {} for user {}", blockNumber, userId);
log.info("Feed block generated - user: {}, block: {}, posts: {}", 
        userId, blockNumber, allPosts.size());
```

3. **Event Processing**:
```java
// RecommendationEventConsumer.java
log.debug("Received event: {}", event);
log.info("Processing event - type: {}, userId: {}", 
        event.getClass().getSimpleName(), event.getUserId());
```

#### How to Check R Vector Values

**Via Database**:
```sql
-- Get latest R vector for user
SELECT 
    user_id,
    version,
    sad_weight,
    fear_weight,
    angry_weight,
    happy_weight,
    conflicted_weight,
    surprised_weight,
    updated_at
FROM user_recommendation_vectors
WHERE user_id = 'USER_ID_HERE'
ORDER BY version DESC
LIMIT 1;
```

**Via API** (if implemented):
```bash
curl -X GET "http://localhost:8080/v1/recommendation/rvector" \
  -H "Authorization: Bearer TOKEN"
```

**Via Admin Endpoint**:
```bash
curl -X GET "http://localhost:8080/admin/v1/users/{userId}/recommendation-status" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

#### How to Verify Event Processing

**Check Redis Stream**:
```bash
# View recent events
redis-cli XREAD COUNT 10 STREAMS recommendation:rvector-updates 0

# Check consumer group status
redis-cli XINFO GROUPS recommendation:rvector-updates

# Check pending messages
redis-cli XPENDING recommendation:rvector-updates recommendation-engine-workers
```

**Check Application Logs**:
```bash
# Search for event processing logs
grep "Processing event" backend.log
grep "Event processed successfully" backend.log
grep "Failed to process event" backend.log
```

**Monitor Metrics** (if Micrometer is configured):
```bash
# Check event processing metrics
curl http://localhost:8080/actuator/metrics/recommendation.events.processed
curl http://localhost:8080/actuator/metrics/recommendation.events.failed
```

---

## 14. Future Enhancements

This section outlines potential improvements and enhancements for the Recommendation Engine.

### 14.1 Potential Improvements

#### Machine Learning Integration

**Current State**: Rule-based recommendation using fixed behavioral weights (+6, +4, +2).

**Potential Enhancement**: Use machine learning to optimize weights and personalize recommendations.

**Implementation Approach**:
```java
// ML-based weight optimization
public class MLWeightOptimizer {
    private MLModel weightModel;
    
    public int getOptimalWeight(UUID userId, Emotion emotion, BehaviorType behavior) {
        // Extract features
        FeatureVector features = extractFeatures(userId, emotion, behavior);
        
        // Predict optimal weight
        double predictedWeight = weightModel.predict(features);
        
        // Clamp to valid range
        return (int) Math.max(2, Math.min(40, predictedWeight));
    }
    
    private FeatureVector extractFeatures(UUID userId, Emotion emotion, BehaviorType behavior) {
        UserProfile profile = userProfileService.getProfile(userId);
        RVectorHistory history = rVectorRepository.getHistory(userId);
        
        return FeatureVector.builder()
            .userAge(profile.getAge())
            .userActivityLevel(profile.getActivityLevel())
            .currentEmotionWeight(rVector.getEmotionWeight(emotion))
            .historicalEngagement(history.getEngagementRate(emotion))
            .timeOfDay(LocalTime.now().getHour())
            .dayOfWeek(LocalDate.now().getDayOfWeek())
            .build();
    }
}
```

**Benefits**:
- Personalized behavioral weights per user
- Adaptive to user patterns
- Improved recommendation accuracy

#### A/B Testing Framework

**Current State**: Fixed recommendation algorithm.

**Potential Enhancement**: A/B testing framework to compare different recommendation strategies.

**Implementation Approach**:
```java
// A/B testing service
@Service
public class ABTestingService {
    
    public RecommendationStrategy getStrategyForUser(UUID userId) {
        // Assign user to variant based on consistent hashing
        int variant = Math.abs(userId.hashCode() % 100);
        
        if (variant < 50) {
            return RecommendationStrategy.CURRENT; // Control group
        } else if (variant < 75) {
            return RecommendationStrategy.ML_WEIGHTS; // Variant A
        } else {
            return RecommendationStrategy.TIME_DECAY; // Variant B
        }
    }
    
    public void trackExperimentResult(UUID userId, RecommendationStrategy strategy, 
                                     FeedEngagementMetrics metrics) {
        ExperimentResult result = ExperimentResult.builder()
            .userId(userId)
            .strategy(strategy)
            .engagementRate(metrics.getEngagementRate())
            .timeSpent(metrics.getTimeSpent())
            .postsViewed(metrics.getPostsViewed())
            .timestamp(LocalDateTime.now())
            .build();
        
        experimentResultRepository.save(result);
    }
}
```

**Benefits**:
- Data-driven optimization
- Compare different strategies
- Measure impact of changes

#### Advanced Analytics

**Current State**: Basic metrics (R vector updates, block generation time).

**Potential Enhancement**: Comprehensive analytics dashboard and reporting.

**Implementation Approach**:
```java
// Analytics service
@Service
public class RecommendationAnalyticsService {
    
    public RecommendationAnalytics getAnalytics(UUID userId, LocalDate startDate, LocalDate endDate) {
        // User engagement metrics
        EngagementMetrics engagement = calculateEngagement(userId, startDate, endDate);
        
        // Emotion distribution over time
        Map<LocalDate, Map<String, Integer>> emotionTrends = 
            getEmotionTrends(userId, startDate, endDate);
        
        // Recommendation accuracy
        double accuracy = calculateRecommendationAccuracy(userId, startDate, endDate);
        
        // Top performing content
        List<PostPerformance> topPosts = getTopPerformingPosts(userId, startDate, endDate);
        
        return RecommendationAnalytics.builder()
            .engagementMetrics(engagement)
            .emotionTrends(emotionTrends)
            .recommendationAccuracy(accuracy)
            .topPerformingPosts(topPosts)
            .build();
    }
    
    private double calculateRecommendationAccuracy(UUID userId, LocalDate start, LocalDate end) {
        // Compare recommended posts vs. user interactions
        List<Post> recommended = getRecommendedPosts(userId, start, end);
        List<Post> interacted = getInteractedPosts(userId, start, end);
        
        long matches = recommended.stream()
            .filter(interacted::contains)
            .count();
        
        return recommended.isEmpty() ? 0.0 : (double) matches / recommended.size();
    }
}
```

**Benefits**:
- Understand user behavior
- Measure recommendation effectiveness
- Identify improvement opportunities

#### Temporal Decay

**Current State**: R vector weights accumulate indefinitely.

**Potential Enhancement**: Implement temporal decay to reduce weights over time.

**Implementation Approach**:
```java
// Temporal decay service
@Service
public class TemporalDecayService {
    
    private static final double DECAY_RATE = 0.95; // 5% decay per day
    
    public void applyTemporalDecay(UUID userId) {
        UserRecommendationVector current = vectorRepository
            .findLatestByUserId(userId).orElse(null);
        
        if (current == null) return;
        
        long daysSinceUpdate = ChronoUnit.DAYS.between(
            current.getUpdatedAt(), LocalDateTime.now());
        
        if (daysSinceUpdate > 7) { // Only decay after 7 days of inactivity
            UserRecommendationVector decayed = createNewVersion(current);
            
            // Apply decay: weight = weight * (decay_rate ^ days)
            double decayFactor = Math.pow(DECAY_RATE, daysSinceUpdate - 7);
            
            decayed.setSadWeight(applyDecay(current.getSadWeight(), decayFactor));
            decayed.setFearWeight(applyDecay(current.getFearWeight(), decayFactor));
            decayed.setAngryWeight(applyDecay(current.getAngryWeight(), decayFactor));
            decayed.setHappyWeight(applyDecay(current.getHappyWeight(), decayFactor));
            decayed.setConflictedWeight(applyDecay(current.getConflictedWeight(), decayFactor));
            decayed.setSurprisedWeight(applyDecay(current.getSurprisedWeight(), decayFactor));
            
            // Ensure minimum value of 2
            ensureMinimumWeights(decayed);
            
            vectorRepository.save(decayed);
        }
    }
    
    private int applyDecay(int weight, double decayFactor) {
        int decayed = (int) Math.round(weight * decayFactor);
        return Math.max(2, decayed); // Minimum is 2
    }
}
```

**Benefits**:
- Prevents stale emotional profiles
- Allows natural drift toward baseline
- Maintains responsiveness to recent behavior

#### Multi-Emotion Posts

**Current State**: Posts have single emotion tag.

**Potential Enhancement**: Support posts with multiple emotions and weighted distribution.

**Implementation Approach**:
```java
// Multi-emotion post entity
@Entity
public class Post {
    // ... existing fields
    
    @Column(name = "primary_emotion")
    private String primaryEmotion;
    
    @Column(name = "emotion_distribution", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> emotionDistribution; // e.g., {"happy": 0.7, "surprised": 0.3}
}

// Update R vector with weighted emotions
public void updateRVectorForMultiEmotionPost(UUID userId, UUID postId, Post post) {
    Map<String, Double> distribution = post.getEmotionDistribution();
    
    for (Map.Entry<String, Double> entry : distribution.entrySet()) {
        String emotion = entry.getKey();
        double weight = entry.getValue();
        
        // Apply proportional weight
        int adjustedWeight = (int) Math.round(WEIGHT_POSTING * weight);
        updateEmotionWeight(userId, Emotion.fromString(emotion), adjustedWeight);
    }
}
```

**Benefits**:
- More accurate emotion representation
- Better reflects complex emotional content
- Improved recommendation matching

---

### 14.2 Implementation Roadmap

**Phase 1: Foundation** (Current)
- ✅ Basic recommendation engine
- ✅ R vector updates
- ✅ Feed block generation
- ✅ Pre-computation

**Phase 2: Optimization** (Next 3 months)
- [ ] Performance optimization
- [ ] Advanced caching strategies
- [ ] Analytics dashboard
- [ ] Monitoring and alerting

**Phase 3: Intelligence** (6-12 months)
- [ ] Machine learning integration
- [ ] A/B testing framework
- [ ] Temporal decay
- [ ] Multi-emotion support

**Phase 4: Advanced Features** (12+ months)
- [ ] Real-time personalization
- [ ] Cross-platform synchronization
- [ ] Advanced analytics and insights
- [ ] User preference controls

---

## Conclusion

This documentation provides a comprehensive guide to the YouthMind Recommendation Engine, covering:

- **System Architecture**: Overall design and component relationships
- **Core Concepts**: R vectors, behavioral weights, block generation, post selection
- **Technical Implementation**: Deep dive into how the system works
- **API Integration**: Complete API documentation and frontend integration guide
- **Configuration**: All properties and database setup
- **Code Examples**: Practical examples for backend and frontend
- **Data Models**: Complete entity and DTO documentation
- **Error Handling**: Common errors and best practices
- **Performance**: Optimization strategies and caching
- **Testing**: Testing strategies and examples
- **Troubleshooting**: Common issues and debugging tips
- **Future Enhancements**: Potential improvements and roadmap

The Recommendation Engine is a sophisticated emotion-based content recommendation system that personalizes user feeds based on behavioral patterns. It balances personalization with content diversity while maintaining high performance through caching and pre-computation.

For questions or contributions, please refer to the development team or create an issue in the project repository.

---

**Documentation Version**: 1.0  
**Last Updated**: 2024-01-15  
**Status**: Complete ✅

