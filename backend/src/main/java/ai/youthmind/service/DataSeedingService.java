package ai.youthmind.service;

import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Profile({"dev", "staging"}) // Only run in development and staging environments
public class DataSeedingService implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private DraftRepository draftRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private final Random random = new Random();
    
    private final String[] firstNames = {
        "Emma", "Liam", "Olivia", "Noah", "Ava", "William", "Sophia", "Mason",
        "Isabella", "James", "Charlotte", "Benjamin", "Amelia", "Lucas", "Mia",
        "Henry", "Harper", "Alexander", "Evelyn", "Michael", "Abigail", "Ethan",
        "Emily", "Daniel", "Elizabeth", "Matthew", "Sofia", "Aiden", "Avery", "Jackson"
    };
    
    private final String[] lastNames = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
        "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
        "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson"
    };
    
    private final String[] postTitles = {
        "Feeling grateful today", "Struggling with anxiety", "Had an amazing day!",
        "Need some advice", "Celebrating small wins", "Dealing with stress",
        "Found inner peace", "Excited about the future", "Working through emotions",
        "Sharing my journey", "Looking for support", "Feeling hopeful",
        "Overcoming challenges", "Grateful for friends", "Managing difficult times",
        "Discovering self-love", "Building confidence", "Embracing change",
        "Finding balance", "Staying positive"
    };
    
    private final String[] postContents = {
        "Today I woke up and realized how much I have to be grateful for. Sometimes we get so caught up in our daily struggles that we forget to appreciate the good things in our lives.",
        "I've been dealing with anxiety for a while now, and some days are harder than others. But I'm learning that it's okay to not be okay sometimes.",
        "Had the most incredible day today! Everything just seemed to fall into place, and I felt so connected to the world around me.",
        "I'm going through a tough time right now and could really use some advice from people who might have been in similar situations.",
        "I know it might seem small, but I managed to complete all my tasks today and it feels like a huge accomplishment!",
        "Work has been really stressful lately, and I'm trying to find healthy ways to cope with the pressure.",
        "After weeks of feeling overwhelmed, I finally found some peace during my morning meditation. It's amazing what a few minutes of silence can do.",
        "I'm so excited about what the future holds! There are so many opportunities ahead, and I can't wait to see where life takes me.",
        "Emotions are complex, aren't they? I'm learning to sit with uncomfortable feelings instead of trying to push them away.",
        "I wanted to share my mental health journey with you all. It's been a rollercoaster, but I'm making progress every day.",
        "Sometimes we all need a little support from our community. Thank you for being here and creating such a safe space.",
        "Despite everything that's been happening, I'm choosing to remain hopeful. Better days are coming.",
        "Every challenge we face is an opportunity to grow stronger. I'm learning to see obstacles as stepping stones.",
        "My friends have been such an incredible source of support. I don't know what I'd do without them.",
        "Going through a difficult period right now, but I'm trying to remember that this too shall pass.",
        "Learning to love myself has been the hardest but most rewarding journey I've ever undertaken.",
        "Confidence doesn't come overnight, but I'm working on building it one small step at a time.",
        "Change can be scary, but I'm learning to embrace it as a natural part of life and growth.",
        "Finding balance in life is an ongoing process. Some days I succeed, others I don't, and that's okay.",
        "Choosing to focus on the positive today, even when things feel challenging. Positivity is a practice."
    };
    
    private final String[] topics = {
        "mental-health", "anxiety", "depression", "self-care", "mindfulness", "therapy",
        "relationships", "work-stress", "family", "friends", "goals", "motivation",
        "gratitude", "healing", "growth", "positivity", "support", "community",
        "wellness", "balance", "confidence", "self-love", "hope", "resilience"
    };
    
    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 10) {
            System.out.println("Database already has data, skipping seeding...");
            return;
        }
        
        System.out.println("Starting data seeding...");
        
        try {
            // Create users
            List<User> users = createUsers(100);
            System.out.println("Created " + users.size() + " users");
            
            // Create user preferences
            createUserPreferences(users);
            System.out.println("Created user preferences");
            
            // Create posts
            List<Post> posts = createPosts(users, 500);
            System.out.println("Created " + posts.size() + " posts");
            
            // Create comments
            createComments(users, posts, 2000);
            System.out.println("Created comments");
            
            // Create user engagements
            createUserEngagements(users, posts, 5000);
            System.out.println("Created user engagements");
            
            // Create conversations and messages
            createConversationsAndMessages(users, 200);
            System.out.println("Created conversations and messages");
            
            // Create drafts
            createDrafts(users, 150);
            System.out.println("Created drafts");
            
            System.out.println("Data seeding completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during data seeding: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setEmail("user" + i + "@youthmind.ai");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setFirstName(firstNames[random.nextInt(firstNames.length)]);
            user.setLastName(lastNames[random.nextInt(lastNames.length)]);
            user.setUsername("user" + i);
            user.setBio("Hello! I'm " + user.getFirstName() + " and I'm here to share my mental health journey.");
            user.setPostsCount(0);
            user.setLikesCount(0);
            user.setBookmarksCount(0);
            user.setLoginCount(random.nextInt(50));
            
            if (random.nextBoolean()) {
                user.setLastLoginAt(LocalDateTime.now().minusDays(random.nextInt(30)));
            }
            
            users.add(user);
        }
        
        return userRepository.saveAll(users);
    }
    
    private void createUserPreferences(List<User> users) {
        List<UserPreferences> preferences = new ArrayList<>();
        UserPreferences.Theme[] themes = UserPreferences.Theme.values();
        UserPreferences.PrivacyLevel[] privacyLevels = UserPreferences.PrivacyLevel.values();
        
        for (User user : users) {
            UserPreferences prefs = new UserPreferences();
            prefs.setUserId(user.getId());
            prefs.setTheme(themes[random.nextInt(themes.length)]);
            prefs.setLanguage(random.nextBoolean() ? "en" : "es");
            prefs.setNotificationsEnabled(random.nextBoolean());
            prefs.setEmailNotifications(random.nextBoolean());
            prefs.setPushNotifications(random.nextBoolean());
            prefs.setPrivacyLevel(privacyLevels[random.nextInt(privacyLevels.length)]);
            prefs.setAiSuggestionsEnabled(random.nextBoolean());
            prefs.setAnonymousPostingAllowed(random.nextBoolean());
            
            preferences.add(prefs);
        }
        
        userPreferencesRepository.saveAll(preferences);
    }
    
    private List<Post> createPosts(List<User> users, int count) {
        List<Post> posts = new ArrayList<>();
        Post.Emotion[] emotions = Post.Emotion.values();
        
        for (int i = 0; i < count; i++) {
            User randomUser = users.get(random.nextInt(users.size()));
            
            Post post = new Post();
            post.setUserId(randomUser.getId());
            post.setTitle(postTitles[random.nextInt(postTitles.length)]);
            post.setContent(postContents[random.nextInt(postContents.length)]);
            post.setEmotion(emotions[random.nextInt(emotions.length)]);
            
            // Set random topics
            Set<String> postTopics = new HashSet<>();
            int topicCount = random.nextInt(4) + 1; // 1-4 topics
            for (int j = 0; j < topicCount; j++) {
                postTopics.add(topics[random.nextInt(topics.length)]);
            }
            post.setTopics(String.join(",", postTopics));
            
            // Set random engagement metrics
            post.setViews(random.nextInt(1000));
            post.setLikes(random.nextInt(200));
            post.setBookmarks(random.nextInt(50));
            post.setResonanceCount(random.nextInt(100));
            post.setCollectCount(random.nextInt(30));
            
            // Some posts are anonymous
            post.setIsAnonymous(random.nextDouble() < 0.1); // 10% anonymous
            
            // Some posts are flagged (for testing moderation)
            if (random.nextDouble() < 0.02) { // 2% flagged
                post.setFlagged(true);
                post.setFlagReason("Inappropriate content");
                post.setFlaggedAt(LocalDateTime.now().minusDays(random.nextInt(7)));
            }
            
            // Set creation time in the past
            post.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(90)));
            post.setUpdatedAt(post.getCreatedAt());
            
            posts.add(post);
        }
        
        return postRepository.saveAll(posts);
    }
    
    private void createComments(List<User> users, List<Post> posts, int count) {
        List<Comment> comments = new ArrayList<>();
        List<Comment> parentComments = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            User randomUser = users.get(random.nextInt(users.size()));
            Post randomPost = posts.get(random.nextInt(posts.size()));
            
            Comment comment = new Comment();
            comment.setPostId(randomPost.getId());
            comment.setAuthorId(randomUser.getId());
            comment.setLikes(random.nextInt(50));
            
            // 70% are top-level comments, 30% are replies
            if (random.nextDouble() < 0.7 || parentComments.isEmpty()) {
                // Top-level comment
                comment.setContent("This really resonates with me. Thank you for sharing your experience!");
                parentComments.add(comment);
            } else {
                // Reply comment
                Comment parentComment = parentComments.get(random.nextInt(parentComments.size()));
                comment.setParentId(parentComment.getId());
                comment.setContent("I completely agree with your perspective on this.");
            }
            
            comment.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
            comments.add(comment);
        }
        
        commentRepository.saveAll(comments);
    }
    
    private void createUserEngagements(List<User> users, List<Post> posts, int count) {
        Set<String> createdEngagements = new HashSet<>(); // To avoid duplicates
        List<UserEngagement> engagements = new ArrayList<>();
        UserEngagement.EngagementType[] types = UserEngagement.EngagementType.values();
        
        int attempts = 0;
        while (engagements.size() < count && attempts < count * 2) {
            attempts++;
            
            User randomUser = users.get(random.nextInt(users.size()));
            Post randomPost = posts.get(random.nextInt(posts.size()));
            UserEngagement.EngagementType randomType = types[random.nextInt(types.length)];
            
            String engagementKey = randomUser.getId() + "-" + randomPost.getId() + "-" + randomType;
            
            if (!createdEngagements.contains(engagementKey)) {
                UserEngagement engagement = new UserEngagement();
                engagement.setUserId(randomUser.getId());
                engagement.setPostId(randomPost.getId());
                engagement.setType(randomType);
                engagement.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(60)));
                
                engagements.add(engagement);
                createdEngagements.add(engagementKey);
            }
        }
        
        userEngagementRepository.saveAll(engagements);
    }
    
    private void createConversationsAndMessages(List<User> users, int conversationCount) {
        List<Conversation> conversations = new ArrayList<>();
        List<Message> allMessages = new ArrayList<>();
        
        String[] conversationTitles = {
            "Anxiety Support Chat", "Daily Check-in", "Mindfulness Discussion",
            "Coping Strategies", "Mental Health Resources", "Peer Support",
            "Therapy Insights", "Self-Care Tips", "Emotional Wellness",
            "Recovery Journey"
        };
        
        String[] userMessages = {
            "I'm feeling really anxious today and could use some support.",
            "Can you help me understand why I'm feeling this way?",
            "What are some good coping strategies for stress?",
            "I'm struggling with depression lately.",
            "How do I build better self-esteem?",
            "I need advice on dealing with difficult relationships.",
            "Can you suggest some mindfulness exercises?",
            "I'm having trouble sleeping due to anxiety.",
            "How do I know if I need professional help?",
            "What are some signs of burnout?"
        };
        
        String[] aiResponses = {
            "I understand you're feeling anxious. It's completely normal to have these feelings. Would you like to talk about what might be triggering your anxiety?",
            "Anxiety can have many causes - both internal and external. Let's explore what might be contributing to how you're feeling right now.",
            "There are several effective coping strategies for stress. Deep breathing, mindfulness, and physical exercise are great starting points. Which of these appeals to you most?",
            "I'm sorry you're going through this difficult time. Depression can feel overwhelming, but remember that you're not alone and help is available.",
            "Building self-esteem is a gradual process. It often starts with practicing self-compassion and challenging negative self-talk. What's one thing you appreciate about yourself?",
            "Relationships can be challenging. It's important to set healthy boundaries and communicate your needs clearly. Can you tell me more about the specific difficulties you're facing?",
            "Mindfulness can be very helpful for managing emotions. A simple exercise is the 5-4-3-2-1 technique: notice 5 things you can see, 4 you can touch, 3 you can hear, 2 you can smell, and 1 you can taste.",
            "Sleep difficulties often accompany anxiety. Creating a calming bedtime routine and limiting screen time before bed can help. Have you tried any relaxation techniques?",
            "Seeking professional help is a sign of strength, not weakness. If your symptoms are interfering with daily life, lasting more than two weeks, or causing distress, it may be helpful to speak with a counselor.",
            "Burnout symptoms include emotional exhaustion, cynicism, and reduced sense of accomplishment. If you're experiencing these, it's important to prioritize self-care and consider reducing stressors where possible."
        };
        
        for (int i = 0; i < conversationCount; i++) {
            User randomUser = users.get(random.nextInt(users.size()));
            
            Conversation conversation = new Conversation();
            conversation.setUserId(randomUser.getId());
            conversation.setTitle(conversationTitles[random.nextInt(conversationTitles.length)]);
            conversation.setIsPinned(random.nextDouble() < 0.1); // 10% pinned
            conversation.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
            conversation.setUpdatedAt(conversation.getCreatedAt());
            
            conversations.add(conversation);
        }
        
        conversations = conversationRepository.saveAll(conversations);
        
        // Create messages for each conversation
        for (Conversation conversation : conversations) {
            List<Message> conversationMessages = new ArrayList<>();
            int messageCount = random.nextInt(10) + 2; // 2-11 messages per conversation
            
            for (int j = 0; j < messageCount; j++) {
                Message message = new Message();
                message.setConversationId(conversation.getId());
                
                if (j % 2 == 0) {
                    // User message
                    message.setSender("user");
                    message.setText(userMessages[random.nextInt(userMessages.length)]);
                } else {
                    // AI response
                    message.setSender("ai");
                    message.setText(aiResponses[random.nextInt(aiResponses.length)]);
                }
                
                message.setTimestamp(conversation.getCreatedAt().plusMinutes(j * 5));
                conversationMessages.add(message);
            }
            
            allMessages.addAll(conversationMessages);
        }
        
        messageRepository.saveAll(allMessages);
    }
    
    private void createDrafts(List<User> users, int count) {
        List<Draft> drafts = new ArrayList<>();
        
        String[] draftTitles = {
            "Untitled Draft", "My Thoughts", "Work in Progress", "Daily Reflection",
            "Personal Journey", "Feelings Today", "Random Thoughts", "Draft Post",
            "Incomplete Idea", "Mental Note"
        };
        
        String[] draftContents = {
            "I started writing this post but I'm not sure how to finish it...",
            "These are just some random thoughts I wanted to capture.",
            "I'm working on expressing my feelings about recent events.",
            "This is a draft of something I might share later.",
            "Still thinking about how to word this properly.",
            "I want to write about my experience but need more time to process.",
            "This is a work in progress - not ready to share yet.",
            "Capturing some ideas before I forget them.",
            "I'm not sure if I should post this or not.",
            "This needs more work before I can share it publicly."
        };
        
        for (int i = 0; i < count; i++) {
            User randomUser = users.get(random.nextInt(users.size()));
            
            Draft draft = new Draft();
            draft.setUserId(randomUser.getId());
            draft.setTitle(draftTitles[random.nextInt(draftTitles.length)]);
            draft.setContent(draftContents[random.nextInt(draftContents.length)]);
            draft.setEmotion(Post.Emotion.values()[random.nextInt(Post.Emotion.values().length)].name());
            draft.setTopics(topics[random.nextInt(topics.length)]);
            draft.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(14)));
            draft.setUpdatedAt(draft.getCreatedAt().plusHours(random.nextInt(24)));
            
            drafts.add(draft);
        }
        
        draftRepository.saveAll(drafts);
    }
}
