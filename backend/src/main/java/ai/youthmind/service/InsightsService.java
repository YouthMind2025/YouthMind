package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Comment;
import ai.youthmind.entity.CommentEngagement;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserEngagement;
import ai.youthmind.repository.CommentEngagementRepository;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.UserEngagementRepository;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
public class InsightsService {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserEngagementRepository userEngagementRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentEngagementRepository commentEngagementRepository;

    public UserActivitySummaryDto getUserActivitySummary(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        UserActivitySummaryDto activitySummaryDto = new UserActivitySummaryDto();
        activitySummaryDto.setContentPatternsDto(analyzeContentPatterns(userId, timeRange));
        activitySummaryDto.setEngagementPatternsDto(analyzeEngagementPatterns(userId));
        activitySummaryDto.setActivityPatternsDto(analyzeActivityPatterns(userId));
        activitySummaryDto.setTimeRange(timeRange);
        return activitySummaryDto;
    }

    public List<String> calculateMostUsedWords(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        List<PostDto> postDtos = postService.getPostsByUser(userId, userId);

        List<String> words = postDtos.stream()
                .filter(postDto -> isBetween(postDto.getCreatedAt(), timeRange))
                .map(PostDto::getContent)
                .flatMap(content -> Arrays.stream(content.toLowerCase().split("[,.\\s]")))
                .toList();

        Map<String, Integer> wordsWithCount = new HashMap<>();
        words.forEach(word -> wordsWithCount.put(word,
                wordsWithCount.getOrDefault(word, 0) + 1));

        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<String, Integer> entry : wordsWithCount.entrySet()) {
            if (minHeap.size() < 50) {
                minHeap.offer(entry);
            } else if (entry.getValue() > minHeap.peek().getValue()) {
                minHeap.poll();
                minHeap.offer(entry);
            }
        }

        List<String> mostUsedWords = new ArrayList<>(minHeap.size());
        while (!minHeap.isEmpty()) {
            mostUsedWords.add(minHeap.poll().getKey());
        }

        return mostUsedWords;
    }

    public List<String> calculateEmotionalTrends(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        List<PostDto> postDtos = postService.getPostsByUser(userId, userId);
        return postDtos.stream()
                .filter(postDto -> isBetween(postDto.getCreatedAt(), timeRange))
                .sorted(Comparator.comparing(PostDto::getCreatedAt))
                .map(PostDto::getEmotion)
                .toList();
    }

    public List<String> calculateTopicPreferences(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        List<PostDto> postDtos = postService.getPostsByUser(userId, userId);

        List<String> topicList = postDtos.stream()
                .filter(postDto -> isBetween(postDto.getCreatedAt(), timeRange))
                .map(PostDto::getTopics)
                .flatMap(topics -> Arrays.stream(topics.split("[,\\s]")))
                .toList();

        Map<String, Integer> topicsWithCount = new HashMap<>();
        topicList.forEach(topic -> topicsWithCount.put(topic,
                topicsWithCount.getOrDefault(topic, 0) + 1));

        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<String, Integer> entry : topicsWithCount.entrySet()) {
            if (minHeap.size() < 5) {
                minHeap.offer(entry);
            } else if (entry.getValue() > minHeap.peek().getValue()) {
                minHeap.poll();
                minHeap.offer(entry);
            }
        }

        List<String> topicPreferences = new ArrayList<>(minHeap.size());
        while (!minHeap.isEmpty()) {
            topicPreferences.add(minHeap.poll().getKey());
        }

        return topicPreferences;
    }

    public List<String> calculateCoveredTopics(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        List<PostDto> postDtos = postService.getPostsByUser(userId, userId);

        List<String> topicList = postDtos.stream()
                .filter(postDto -> isBetween(postDto.getCreatedAt(), timeRange))
                .map(PostDto::getTopics)
                .flatMap(topics -> Arrays.stream(topics.split("[,\\s]")))
                .toList();

        return new HashSet<>(topicList).stream().toList();
    }

    public ContentPatternsDto analyzeContentPatterns(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        ContentPatternsDto contentPatterns = new ContentPatternsDto();
        contentPatterns.setPostMostUsedWords(calculateMostUsedWords(userId, timeRange));
        contentPatterns.setEmotionalTrends(calculateEmotionalTrends(userId, timeRange));
        contentPatterns.setTopicPreferences(calculateTopicPreferences(userId, timeRange));
        contentPatterns.setCoveredTopics(calculateCoveredTopics(userId, timeRange));
        return contentPatterns;
    }

    public List<Long> calculateResponseTimes(UUID userId) {
        List<CommentEngagement> commentEngagements = commentEngagementRepository
                .findByUserIdAndType(userId, CommentEngagement.EngagementType.LIKE);

        List<Long> responseTimes = new ArrayList<>();
        for (CommentEngagement commentEngagement : commentEngagements) {
            Optional<Comment> commentOptional = commentRepository.findById(commentEngagement.getCommentId());
            if (commentOptional.isEmpty())
                continue;

            Comment comment = commentOptional.get();
            Duration duration = Duration.between(comment.getCreatedAt(), commentEngagement.getCreatedAt());
            responseTimes.add(duration.getSeconds());
        }
        return responseTimes;
    }

    public List<String> calculateMostEngagedTopics(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        List<PostDto> engagedPosts = postService.getLikedPostsByUser(userId);
        engagedPosts.addAll(postService.getBookmarkedPostsByUser(userId));
        commentRepository.findByAuthorIdAndParentIdIsNullOrderByCreatedAtDesc(userId).stream()
                .filter(comment -> isBetween(comment.getCreatedAt(), timeRange))
                .map(comment -> postService.getPostById(comment.getPostId(), userId))
                .forEach(engagedPosts::add);

        List<String> topicList = engagedPosts.stream()
                .map(PostDto::getTopics)
                .flatMap(topics -> Arrays.stream(topics.split("[,\\s]")))
                .toList();

        Map<String, Integer> topicsWithCount = new HashMap<>();
        topicList.forEach(topic -> topicsWithCount.put(topic,
                topicsWithCount.getOrDefault(topic, 0) + 1));

        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<String, Integer> entry : topicsWithCount.entrySet()) {
            if (minHeap.size() < 5) {
                minHeap.offer(entry);
            } else if (entry.getValue() > minHeap.peek().getValue()) {
                minHeap.poll();
                minHeap.offer(entry);
            }
        }

        List<String> mostEngagedTopics = new ArrayList<>(minHeap.size());
        while (!minHeap.isEmpty()) {
            mostEngagedTopics.add(minHeap.poll().getKey());
        }

        return mostEngagedTopics;
    }

    public List<UUID> calculateMostPerformancedPosts(UUID userId, ToIntFunction<PostDto> metric) {
        List<PostDto> postDtos = postService.getPostsByUser(userId, userId);
        return postDtos.stream()
                .sorted(Comparator.comparingInt(metric).reversed())
                .map(PostDto::getId)
                .limit(3).toList();
    }

    public EngagementPatternsDto analyzeEngagementPatterns(UUID userId) {
        User user = userService.getUserById(userId).orElseThrow(
                () -> new IllegalArgumentException("User with id " + userId + " not found")
        );

        List<UserEngagement> resonances = userEngagementRepository
                .findByUserIdAndType(userId, UserEngagement.EngagementType.RESONANCE);
        List<UserEngagement> collections = userEngagementRepository
                .findByUserIdAndType(userId, UserEngagement.EngagementType.COLLECT);

        Map<Integer, Long> resonanceHours = resonances.stream()
                .map(resonance -> resonance.getCreatedAt().getHour())
                .collect(Collectors.groupingBy(hour -> hour, Collectors.counting()));
        for (int i = 0; i <= 23; i++) {
            resonanceHours.putIfAbsent(i, 0L);
        }

        Map<Integer, Long> resonanceDays = resonances.stream()
                .map(resonance -> resonance.getCreatedAt().toLocalDate().getDayOfWeek().getValue())
                .collect(Collectors.groupingBy(day -> day, Collectors.counting()));
        for (int i = 1; i <= 7; i++) {
            resonanceDays.putIfAbsent(i, 0L);
        }

        Map<Integer, Long> collectionHours = collections.stream()
                .map(collection -> collection.getCreatedAt().getHour())
                .collect(Collectors.groupingBy(hour -> hour, Collectors.counting()));
        for (int i = 0; i <= 23; i++) {
            collectionHours.putIfAbsent(i, 0L);
        }

        Map<Integer, Long> collectionDays = collections.stream()
                .map(collection -> collection.getCreatedAt().toLocalDate().getDayOfWeek().getValue())
                .collect(Collectors.groupingBy(day -> day, Collectors.counting()));
        for (int i = 1; i <= 7; i++) {
            collectionDays.putIfAbsent(i, 0L);
        }

        List<Comment> comments = commentRepository.findByAuthorIdAndParentIdIsNullOrderByCreatedAtDesc(userId);

        Map<Integer, Long> commentHours = comments.stream()
                .map(comment -> comment.getCreatedAt().getHour())
                .collect(Collectors.groupingBy(hour -> hour, Collectors.counting()));
        for (int i = 0; i <= 23; i++) {
            commentHours.putIfAbsent(i, 0L);
        }

        Map<Integer, Long> commentDays = comments.stream()
                .map(comment -> comment.getCreatedAt().toLocalDate().getDayOfWeek().getValue())
                .collect(Collectors.groupingBy(day -> day, Collectors.counting()));
        for (int i = 1; i <= 7; i++) {
            commentDays.putIfAbsent(i, 0L);
        }

        EngagementPatternsDto engagementPatterns = new EngagementPatternsDto();
        engagementPatterns.setResonancesCount(user.getLikesCount());
        engagementPatterns.setCommentsCount(commentRepository.countByAuthorId(userId));
        engagementPatterns.setCollectionsCount(user.getBookmarksCount());
        engagementPatterns.setEngagementRate(user.getPostsCount() != 0 ?
                user.getLikesCount() / (double) user.getPostsCount() : 0);
        engagementPatterns.setResponseTimes(calculateResponseTimes(userId));
        engagementPatterns.setMostEngagedTopic(calculateMostEngagedTopics(userId, Pair.of(LocalDateTime.MIN, LocalDateTime.MAX)));
        engagementPatterns.setResonanceActivityHour(resonanceHours);
        engagementPatterns.setResonanceActivityDay(resonanceDays);
        engagementPatterns.setCollectionActivityHour(collectionHours);
        engagementPatterns.setCollectionActivityDay(collectionDays);
        engagementPatterns.setCommentActivityHour(commentHours);
        engagementPatterns.setCommentActivityDay(commentDays);
        engagementPatterns.setMostResonancePosts(calculateMostPerformancedPosts(userId, PostDto::getResonance));
        engagementPatterns.setMostCommentPosts(calculateMostPerformancedPosts(userId, PostDto::getCommentsCount));
        engagementPatterns.setMostViewPosts(calculateMostPerformancedPosts(userId, PostDto::getViews));

        return engagementPatterns;
    }

    public Map<String, Integer> calculatePostEmotionDistribution(
            UUID userId, Pair<LocalDateTime, LocalDateTime> timeRange) {
        List<PostDto> posts = postService.getPostsByUser(userId, userId);

        Map<String, List<PostDto>> postByEmotion = posts.stream()
                .filter(postDto -> isBetween(postDto.getCreatedAt(), timeRange))
                .collect(Collectors.groupingBy(PostDto::getEmotion));

        return Maps.transformValues(postByEmotion, List::size);
    }

    public ActivityPatternsDto analyzeActivityPatterns(UUID userId) {
        User user = userService.getUserById(userId).orElseThrow(
                () -> new IllegalArgumentException("User with id " + userId + " not found")
        );

        List<PostDto> posts = postService.getPostsByUser(userId, userId);

        Map<LocalDate, List<PostDto>> postsByDay = posts.stream()
                .collect(Collectors.groupingBy(postDto -> postDto.getCreatedAt().toLocalDate()));
        double postFrequencyDay = postsByDay.values().stream()
                .mapToInt(List::size)
                .average().orElse(0);

        Map<Integer, List<PostDto>> postsByHour = posts.stream()
                .collect(Collectors.groupingBy(postDto -> postDto.getCreatedAt().getHour()));
        for (int i = 0; i <= 23; i++) {
            postsByHour.putIfAbsent(i, new ArrayList<>());
        }

        Map<Integer, List<PostDto>> postsByDayOfWeek = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            postsByDayOfWeek.put(i, new ArrayList<>());
        }
        postsByDay.forEach((localDate, postDtos) -> {
            int dayOfWeek = localDate.getDayOfWeek().getValue();
            List<PostDto> currPostDtos = postsByDayOfWeek.get(dayOfWeek);
            currPostDtos.addAll(postDtos);
            postsByDayOfWeek.put(dayOfWeek, currPostDtos);
        });

        Map<Pair<Integer, Integer>, List<PostDto>> postsByMonth = posts.stream()
                .collect(Collectors.groupingBy(postDto -> {
                    LocalDate localDate = postDto.getCreatedAt().toLocalDate();
                    return Pair.of(localDate.getYear(), localDate.getMonthValue());
                }));

        Map<Pair<Integer, Integer>, List<PostDto>> postsBySeason = posts.stream()
                .collect(Collectors.groupingBy(postDto -> {
                    LocalDate localDate = postDto.getCreatedAt().toLocalDate();
                    int season = localDate.getMonthValue() / 3 % 4 + 1;
                    return Pair.of(localDate.getYear(), season);
                }));

        double postAverageResonances = posts.stream()
                .mapToInt(PostDto::getResonance)
                .average().orElse(0);

        double postAverageComments = posts.stream()
                .mapToInt(PostDto::getCommentsCount)
                .average().orElse(0);

        double postAverageViews = posts.stream()
                .mapToInt(PostDto::getViews)
                .average().orElse(0);

        double postAverageWords = posts.stream()
                .map(PostDto::getContent)
                .mapToInt(content -> content.toLowerCase().split("[,.\\s]").length)
                .average().orElse(0);

        double postAverageCharacters = posts.stream()
                .map(PostDto::getContent)
                .mapToInt(String::length)
                .average().orElse(0);

        ActivityPatternsDto activityPatterns = new ActivityPatternsDto();
        activityPatterns.setPostsCount(user.getPostsCount());
        activityPatterns.setPostFrequencies(postFrequencyDay);
        activityPatterns.setPostActivityHour(Maps.transformValues(postsByHour, List::size));
        activityPatterns.setPostActivityDay(Maps.transformValues(postsByDayOfWeek, List::size));
        activityPatterns.setPostMonthlyTrend(Maps.transformValues(postsByMonth, List::size));
        activityPatterns.setPostSeasonalTrend(Maps.transformValues(postsBySeason, List::size));
        activityPatterns.setPostAverageResonances(postAverageResonances);
        activityPatterns.setPostAverageComments(postAverageComments);
        activityPatterns.setPostAverageViews(postAverageViews);
        activityPatterns.setPostEmotionDistribution(calculatePostEmotionDistribution(userId, Pair.of(LocalDateTime.MIN, LocalDateTime.MAX)));
        activityPatterns.setPostAverageWords(postAverageWords);
        activityPatterns.setPostAverageCharacters(postAverageCharacters);

        return activityPatterns;
    }

    private static boolean isBetween(LocalDateTime time, Pair<LocalDateTime, LocalDateTime> timeRange) {
        return time.isAfter(timeRange.getFirst()) && time.isBefore(timeRange.getSecond());
    }

}
