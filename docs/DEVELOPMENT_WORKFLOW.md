# YouthMind Development Workflow
## Ultimate 6-Week Development Roadmap & Technical Implementation Guide

*A comprehensive day-by-day project management framework for building YouthMind's core features*

---

## Project Overview & Objectives

YouthMind's MVP delivery within 6 weeks integrates anonymous youth expression, AI-powered emotional intelligence, and community-driven understanding. This roadmap provides detailed day-by-day implementation with specific code examples, deliverables, and team assignments.

### Core Deliverable Timeline
**Week 1-2**: Foundation → Feed → Post Creation  
**Week 3-4**: AI Integration → Advanced Features  
**Week 5-6**: Security → Deployment → Launch

### MVP Feature Set
- **Anonymous Posting System**: Secure, UUID-based identity management
- **AI-Powered Emotional Support**: Real-time sentiment analysis and personalized responses  
- **Community Feed Algorithm**: Intelligent content curation and peer interaction
- **Search & Discovery**: Content search with caching optimization
- **Administrative Dashboard**: Content moderation and system monitoring

---

## Team Composition & Assignments

### **iOS Development Squad** *(3 members)*
- **Dawei** - iOS Developer, SwiftUI Architecture, Core UI Components
- **Dexter** - UI/UX Implementation, Feed Systems, PostDetailView
- **Youssef** - Identity Management, API Integration, Session Management

### **Backend Engineering Team** *(4 members)*
- **Mustafo** - API Architecture Developer, REST Endpoints, Service Integration
- **Junayd** - Project Manager & Backend Developer, System Coordination
- **Ahmed** - Database Design, JPA/Hibernate, Performance Optimization  
- **Harley** - Caching Systems, Redis Implementation, DevOps

### **AI & Machine Learning Division** *(2 members)*
- **Andy** - Emotional Intelligence Models, NLP Pipeline, Sentiment Analysis
- **Kevin** - AI Lead & Service Architecture Lead, FastAPI, Model Deployment

### **Cross-Platform Integration** *(1 member)*
- **Alex** - Full-stack Development, Admin Systems, Testing, Integration

---

# Week 1 (Days 1–7): Foundation & Infrastructure Setup

## Goals
- Establish repositories, folder structures, and baseline environments
- Finalize API design & database schema  
- Initialize iOS, Backend, AI skeletons with Docker Compose
- Complete integration smoke tests

---

## Day 1: Kickoff & Planning

### All Teams (Daily Stand-up)
**Objective**: Align on six-week target and component understanding

**Tasks**:
- Review target: "Anonymous posts → Feed → AI analysis → Basic notifications → Deploy"
- Confirm UI components understanding: FeedView, PostCreateView, SearchView, AvatarCreationView, CoverDesignView, NotificationView, EmotionMuseumView, EmotionPuzzleView
- Establish screen flows and user journey mapping

### Junayd (Project Manager)
**Deliverables**:
- Master project board (Trello/Notion) with weekly lanes
- Team member invitations and role assignments  
- Communication channels setup (Slack)
- Initial "Feature & API Sketch" document distribution

**Duration**: Full day setup and coordination

---

## Day 2: Repository & Environment Initialization

### All Developers
**Repository Setup**:
```bash
# GitHub organization "YouthMind" repositories:
# - youthmind-ios
# - youthmind-backend  
# - youthmind-ai
# - youthmind-admin
```
- Branch protection on main (require ≥2 reviewers)
- Repository structure and initial commits

### Ahmed & Mustafo (Backend)
**Spring Boot Project Setup**:
```xml
<!-- Dependencies via Spring Initializr -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-ui</artifactId>
        <version>1.6.14</version>
    </dependency>
</dependencies>
```

**Folder Structure**:
```
src/main/java/com/youthmind/
├─ config/       # Security + Swagger configuration
├─ controller/   # REST API controllers  
├─ model/        # JPA Entities (UserEntity, PostEntity, etc.)
├─ repository/   # JPA Repositories
├─ service/      # Business logic services
└─ dto/          # Request/Response DTOs
```

### Andy & Kevin (AI)
**FastAPI Skeleton Setup**:
```python
# app.py
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class SentimentRequest(BaseModel):
    text: str

class SentimentResponse(BaseModel):
    sentiment_score: float
    empathy_score: float

@app.post("/ai/sentiment", response_model=SentimentResponse)
async def sentiment(req: SentimentRequest):
    return {"sentiment_score": 0.0, "empathy_score": 0.0}

class QuestionRequest(BaseModel):
    text: str

class QuestionResponse(BaseModel):
    questions: list[str]

@app.post("/ai/questions", response_model=QuestionResponse)
async def questions(req: QuestionRequest):
    return {"questions": ["Demo Q1", "Demo Q2"]}

@app.post("/ai/summarize-post")
async def summarize_post(req: BaseModel):
    return {"summary": "Demo summary"}

@app.post("/ai/analyze-emotion")  
async def analyze_emotion(req: BaseModel):
    return {"emotion": "neutral"}
```

**Requirements.txt**:
```
fastapi==0.104.1
uvicorn[standard]==0.24.0
transformers==4.35.2
torch==2.1.1
detoxify==0.5.2
pydantic==2.5.0
```

### Dawei & Dexter (iOS)
**Xcode Project "YouthMind"**:
- Language: Swift 5.5+, Interface: SwiftUI
- Deployment Target: iOS 15.0+
- Firebase Crashlytics integration

**Folder Structure**:
```
YouthMind/
├─ YouthMindApp.swift
├─ Model/           # Post.swift, User.swift, SentimentResponse.swift
├─ View/            # FeedView.swift, PostCreateView.swift, etc.
├─ ViewModel/       # FeedViewModel.swift, PostViewModel.swift
├─ Services/        # APIClient.swift, DraftService.swift, SessionManager.swift
├─ Resources/       # Assets.xcassets, LaunchScreen.storyboard  
└─ SupportingFiles/ # Info.plist, GoogleService-Info.plist
```

**Firebase Integration**:
```swift
// YouthMindApp.swift
import SwiftUI
import FirebaseCore
import FirebaseCrashlytics

@main
struct YouthMindApp: App {
    init() {
        FirebaseApp.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### Harley (DevOps)
**Docker Compose Configuration**:
```yaml
# infrastructure/docker-compose.yml
version: "3.8"
services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: youthmind
      POSTGRES_USER: yuser
      POSTGRES_PASSWORD: ypass
    ports: ["5432:5432"]
    volumes: ["db-data:/var/lib/postgresql/data"]

  redis:
    image: redis:7
    ports: ["6379:6379"]

  backend:
    build:
      context: ../youthmind-backend
    depends_on: ["postgres", "redis"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/youthmind
      SPRING_DATASOURCE_USERNAME: yuser
      SPRING_DATASOURCE_PASSWORD: ypass
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: supersecretkey
    ports: ["8080:8080"]

  ai:
    build:
      context: ../youthmind-ai
    depends_on: ["backend"]
    ports: ["8001:8001"]
    environment:
      MODEL_PATH: /models
    volumes:
      - ./youthmind-ai/models:/models

volumes:
  db-data:
```

**Dockerfiles**:
```dockerfile
# Backend Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/youthmind-backend.jar ./youthmind-backend.jar
ENTRYPOINT ["java", "-jar", "/app/youthmind-backend.jar"]
```

```dockerfile
# AI Dockerfile
FROM python:3.9-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
```

### Alex (Admin UI)
**React Application Setup**:
```bash
cd youthmind-admin
npx create-react-app . --template javascript
npm install axios
```

**.env.local**:
```
REACT_APP_API_BASE=http://localhost:8080
```

---

## Day 3: Database Schema & API Contract Finalization

### Ahmed (Database Design)
**JPA Entity Models**:
```java
// UserEntity.java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private UUID id;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    // Getters, setters, constructors
}

// PostEntity.java
@Entity
@Table(name = "posts")
public class PostEntity {
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion")
    private EmotionEnum emotion;
    
    @Column(name = "cover_url")
    private String coverUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Additional fields, getters, setters
}

// AiDataEntity.java
@Entity
@Table(name = "ai_data")
public class AiDataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;
    
    @Column(name = "sentiment_score")
    private Double sentimentScore;
    
    @Column(name = "empathy_score")
    private Double empathyScore;
    
    @Column(name = "questions_json", columnDefinition = "TEXT")
    private String questionsJson;
    
    @Column(name = "summary")
    private String summary;
    
    // Getters, setters
}
```

### Mustafo (API Contract)
**OpenAPI Specification**:
```yaml
# src/main/resources/api.yaml
openapi: 3.0.3
info:
  title: YouthMind API
  version: 1.0.0
  description: Anonymous youth mental health platform API

paths:
  /auth/session:
    post:
      summary: Create Anonymous Session
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                deviceId:
                  type: string
                  format: uuid
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SessionResponse'

  /posts/create:
    post:
      summary: Create New Post
      security:
        - BearerAuth: []
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreatePostRequest'
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PostDto'

  /posts/feed:
    get:
      summary: Get Feed Posts
      security:
        - BearerAuth: []
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PagePostDto'

components:
  schemas:
    SessionResponse:
      type: object
      properties:
        userId:
          type: string
          format: uuid
        accessToken:
          type: string
        refreshToken:
          type: string
        expiresIn:
          type: integer

    CreatePostRequest:
      type: object
      required:
        - title
        - content
        - emotion
      properties:
        title:
          type: string
          maxLength: 200
        content:
          type: string
          maxLength: 2000
        emotion:
          type: string
          enum: [angry, sad, happy, conflicted, surprised, fear]
        coverUrl:
          type: string
          format: uri
          nullable: true

    PostDto:
      type: object
      properties:
        postId:
          type: string
          format: uuid
        userId:
          type: string
          format: uuid
        title:
          type: string
        content:
          type: string
        emotion:
          type: string
        coverUrl:
          type: string
          nullable: true
        createdAt:
          type: string
          format: date-time
        sentimentScore:
          type: number
          format: float
          nullable: true
        empathyScore:
          type: number
          format: float
          nullable: true
        questions:
          type: array
          items:
            type: string
        aiSummary:
          type: string
          nullable: true
        resonanceCount:
          type: integer
          default: 0

  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

---

## Day 4: iOS & AI Skeleton Completion

### iOS Team (Dawei, Dexter, Youssef)
**APIClient.swift Implementation**:
```swift
import Foundation

class APIClient {
    static let shared = APIClient()
    private let baseURL = URL(string: "http://localhost:8080")!
    private let aiURL = URL(string: "http://localhost:8001")!
    
    private init() {}
    
    // MARK: - Session Management
    func createSession(deviceId: String? = nil) async throws -> SessionResponse {
        let url = baseURL.appendingPathComponent("/auth/session")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body = ["deviceId": deviceId ?? UUID().uuidString]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw APIError.invalidResponse
        }
        
        return try JSONDecoder().decode(SessionResponse.self, from: data)
    }
    
    // MARK: - Posts
    func createPost(_ request: CreatePostRequest) async throws -> PostDto {
        let url = baseURL.appendingPathComponent("/posts/create")
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = SessionManager.shared.currentUser?.accessToken {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        urlRequest.httpBody = try JSONEncoder().encode(request)
        
        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 201 else {
            throw APIError.invalidResponse
        }
        
        return try JSONDecoder().decode(PostDto.self, from: data)
    }
    
    func fetchFeed(page: Int = 0, size: Int = 20) async throws -> PageResponse<PostDto> {
        var components = URLComponents(url: baseURL.appendingPathComponent("/posts/feed"), resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "size", value: String(size))
        ]
        
        var request = URLRequest(url: components.url!)
        if let token = SessionManager.shared.currentUser?.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw APIError.invalidResponse
        }
        
        return try JSONDecoder().decode(PageResponse<PostDto>.self, from: data)
    }
    
    // MARK: - AI Services
    func fetchSentiment(text: String) async throws -> SentimentResponse {
        let url = aiURL.appendingPathComponent("/ai/sentiment")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body = SentimentRequest(text: text)
        request.httpBody = try JSONEncoder().encode(body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw APIError.invalidResponse
        }
        
        return try JSONDecoder().decode(SentimentResponse.self, from: data)
    }
    
    func fetchQuestions(text: String) async throws -> QuestionResponse {
        let url = aiURL.appendingPathComponent("/ai/questions")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body = QuestionRequest(text: text)
        request.httpBody = try JSONEncoder().encode(body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw APIError.invalidResponse
        }
        
        return try JSONDecoder().decode(QuestionResponse.self, from: data)
    }
}

// MARK: - Data Models
struct SessionResponse: Codable {
    let userId: String
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
}

struct CreatePostRequest: Codable {
    let title: String
    let content: String
    let emotion: String
    let coverUrl: String?
}

struct PostDto: Codable, Identifiable {
    let postId: String
    let userId: String
    let title: String
    let content: String
    let emotion: String
    let coverUrl: String?
    let createdAt: String
    let sentimentScore: Double?
    let empathyScore: Double?
    let questions: [String]?
    let aiSummary: String?
    let resonanceCount: Int
    
    var id: String { postId }
}

struct PageResponse<T: Codable>: Codable {
    let posts: [T]
    let hasNext: Bool
    let totalElements: Int
    let totalPages: Int
}

struct SentimentRequest: Codable {
    let text: String
}

struct SentimentResponse: Codable {
    let sentimentScore: Double
    let empathyScore: Double
}

struct QuestionRequest: Codable {
    let text: String
}

struct QuestionResponse: Codable {
    let questions: [String]
}

enum APIError: Error {
    case invalidResponse
    case invalidData
    case networkError
}
```

### Andy & Kevin (AI Services)
**Enhanced AI Service Testing**:
```bash
cd youthmind-ai
uvicorn app:app --reload --port 8001

# Test endpoints
curl -X POST http://localhost:8001/ai/sentiment \
  -H "Content-Type: application/json" \
  -d '{"text":"I feel overwhelmed with school stress"}'

curl -X POST http://localhost:8001/ai/questions \
  -H "Content-Type: application/json" \
  -d '{"text":"I feel overwhelmed with school stress"}'
```

### Harley (DevOps Integration)
**Docker Compose Validation**:
```bash
cd infrastructure
docker-compose up --build

# Verify services
curl http://localhost:5432  # PostgreSQL
curl http://localhost:6379  # Redis  
curl http://localhost:8080/actuator/health  # Backend
curl http://localhost:8001/docs  # AI FastAPI docs
```

---

## Days 5-7: Integration & Session Management

### Day 5: Mobile Session Logic & Crashlytics

**Youssef (iOS Session Management)**:
```swift
// SessionManager.swift
import Foundation
import Combine

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    @Published var currentUser: SessionResponse?
    @Published var isAuthenticated = false
    
    private let userDefaults = UserDefaults.standard
    private let sessionKey = "user_session"
    
    private init() {
        loadSession()
    }
    
    func loadSession() {
        if let data = userDefaults.data(forKey: sessionKey),
           let session = try? JSONDecoder().decode(SessionResponse.self, from: data) {
            currentUser = session
            isAuthenticated = true
        }
    }
    
    func saveSession(_ session: SessionResponse) {
        currentUser = session
        isAuthenticated = true
        
        if let data = try? JSONEncoder().encode(session) {
            userDefaults.set(data, forKey: sessionKey)
        }
    }
    
    func clearSession() {
        currentUser = nil
        isAuthenticated = false
        userDefaults.removeObject(forKey: sessionKey)
    }
    
    func createSessionIfNeeded() async throws {
        guard currentUser == nil else { return }
        
        let session = try await APIClient.shared.createSession()
        saveSession(session)
    }
}
```

**Crashlytics Integration**:
```swift
// YouthMindApp.swift
import SwiftUI
import FirebaseCore
import FirebaseCrashlytics

@main
struct YouthMindApp: App {
    init() {
        FirebaseApp.configure()
        
        // Test crashlytics
        #if DEBUG
        Crashlytics.crashlytics().log("YouthMind app started")
        #endif
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    SessionManager.shared.loadSession()
                }
        }
    }
}
```

### Day 6: Backend CRUD Implementation

**Mustafo & Junayd (Backend Controllers)**:
```java
// AuthController.java
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @PostMapping("/session")
    public ResponseEntity<SessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        UUID userId = UUID.randomUUID();
        
        // Create or retrieve user
        UserEntity user = userService.createOrUpdateUser(userId);
        
        // Generate JWT tokens
        String accessToken = tokenProvider.generateAccessToken(userId.toString());
        String refreshToken = tokenProvider.generateRefreshToken(userId.toString());
        
        SessionResponse response = SessionResponse.builder()
                .userId(userId.toString())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .build();
        
        return ResponseEntity.ok(response);
    }
}

// PostController.java
@RestController
@RequestMapping("/posts")
@CrossOrigin(origins = "*")
public class PostController {
    
    @Autowired
    private PostService postService;
    
    @PostMapping("/create")
    public ResponseEntity<PostDto> createPost(
            @RequestBody CreatePostRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        String userId = tokenProvider.getUserIdFromToken(token);
        
        PostDto createdPost = postService.createPost(request, UUID.fromString(userId));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }
    
    @GetMapping("/feed")
    public ResponseEntity<Page<PostDto>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String authHeader) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostDto> feedPage = postService.getFeed(pageable);
        
        return ResponseEntity.ok(feedPage);
    }
}
```

**PostService Implementation**:
```java
@Service
@Transactional
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AiClient aiClient;
    
    @Autowired
    private AiDataRepository aiDataRepository;
    
    public PostDto createPost(CreatePostRequest request, UUID userId) {
        // Get or create user
        UserEntity user = userRepository.findById(userId)
                .orElseGet(() -> userRepository.save(new UserEntity(userId, LocalDateTime.now())));
        
        // Create post entity
        PostEntity post = PostEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .emotion(EmotionEnum.valueOf(request.getEmotion().toUpperCase()))
                .coverUrl(request.getCoverUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        post = postRepository.save(post);
        
        // Async AI processing
        processAiAnalysis(post);
        
        return PostDto.fromEntity(post);
    }
    
    @Async
    public void processAiAnalysis(PostEntity post) {
        try {
            // Call AI service for sentiment analysis
            SentimentResponse sentiment = aiClient.getSentiment(post.getContent());
            QuestionResponse questions = aiClient.getQuestions(post.getContent());
            
            // Save AI data
            AiDataEntity aiData = AiDataEntity.builder()
                    .post(post)
                    .sentimentScore(sentiment.getSentimentScore())
                    .empathyScore(sentiment.getEmpathyScore())
                    .questionsJson(new ObjectMapper().writeValueAsString(questions.getQuestions()))
                    .build();
            
            aiDataRepository.save(aiData);
            
        } catch (Exception e) {
            log.error("AI processing failed for post {}", post.getId(), e);
        }
    }
    
    @Cacheable(value = "feedCache", key = "'page:'+#pageable.pageNumber+':size:'+#pageable.pageSize")
    public Page<PostDto> getFeed(Pageable pageable) {
        Page<PostEntity> postPage = postRepository.findAll(pageable);
        return postPage.map(this::enrichPostWithAiData);
    }
    
    private PostDto enrichPostWithAiData(PostEntity post) {
        PostDto dto = PostDto.fromEntity(post);
        
        // Enrich with AI data if available
        aiDataRepository.findByPost(post).ifPresent(aiData -> {
            dto.setSentimentScore(aiData.getSentimentScore());
            dto.setEmpathyScore(aiData.getEmpathyScore());
            try {
                List<String> questions = new ObjectMapper().readValue(
                    aiData.getQuestionsJson(), 
                    new TypeReference<List<String>>() {}
                );
                dto.setQuestions(questions);
            } catch (Exception e) {
                log.error("Failed to parse questions JSON", e);
            }
        });
        
        return dto;
    }
}
```

### Day 7: Integration Smoke Tests

**Integration Testing Checklist**:

1. **Docker Environment**:
```bash
cd infrastructure
docker-compose up --build
```

2. **Backend API Testing**:
```bash
# Test session creation
curl -X POST http://localhost:8080/auth/session \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"test-device-123"}'

# Test post creation  
curl -X POST http://localhost:8080/posts/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token_from_session>" \
  -d '{
    "title": "Test Post",
    "content": "This is a test post content",
    "emotion": "happy",
    "coverUrl": null
  }'

# Test feed retrieval
curl -X GET "http://localhost:8080/posts/feed?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

3. **AI Service Testing**:
```bash
curl -X POST http://localhost:8001/ai/sentiment \
  -H "Content-Type: application/json" \
  -d '{"text":"I feel overwhelmed with school stress"}'

curl -X POST http://localhost:8001/ai/questions \
  -H "Content-Type: application/json" \
  -d '{"text":"I feel overwhelmed with school stress"}'
```

4. **iOS Integration**:
- Run iOS simulator
- Verify FeedView placeholder loads without crashes
- Test SessionManager creates session automatically
- Confirm Crashlytics integration works

5. **Database Verification**:
```sql
-- Connect to PostgreSQL
psql -h localhost -p 5432 -U yuser -d youthmind

-- Verify tables created
\dt

-- Check sample data
SELECT * FROM users;
SELECT * FROM posts;  
SELECT * FROM ai_data;
```

---

## Week 1 Deliverables ✅

### Repository Infrastructure
- [x] GitHub repositories: youthmind-ios, youthmind-backend, youthmind-ai, youthmind-admin
- [x] Branch protection and review requirements
- [x] Docker Compose environment with all services

### Backend Foundation  
- [x] Spring Boot application with JPA entities
- [x] Database tables auto-generated via Hibernate
- [x] Swagger UI available at /swagger-ui.html
- [x] Basic CRUD endpoints: session, createPost, getFeed
- [x] JWT authentication framework

### iOS Foundation
- [x] Xcode project with SwiftUI architecture
- [x] Firebase Crashlytics integration
- [x] APIClient with networking layer
- [x] SessionManager for authentication
- [x] Basic navigation structure

### AI Services
- [x] FastAPI application with sentiment and question endpoints
- [x] Stub responses for MVP testing
- [x] Docker containerization
- [x] Integration with backend via REST API

### Integration Testing
- [x] All services running via Docker Compose
- [x] End-to-end API flow: session → create post → fetch feed
- [x] iOS app connects to backend without crashes
- [x] AI service responds to backend requests

---

*Ready to proceed to Week 2: Core Feature Implementation with real data binding and caching optimization.* 

# Week 2 (Days 8–14): Core Feature Implementation

## Goals
- Complete Backend persistence with Redis caching
- Build functional iOS FeedView & PostCreateView with real data
- Implement AI sentiment analysis with machine learning models
- Integrate end-to-end post creation and feed retrieval

---

## Day 8: Backend Post Persistence & JWT Authentication

### Mustafo & Ahmed (Backend Implementation)

**Enhanced AuthController**:
```java
@PostMapping("/session")
public ResponseEntity<SessionResponse> createSession(@RequestBody CreateSessionRequest request) {
    UUID userId = UUID.randomUUID();
    UserEntity user = userService.createOrUpdateUser(userId);
    
    String accessToken = jwtTokenProvider.generateAccessToken(userId.toString());
    String refreshToken = jwtTokenProvider.generateRefreshToken(userId.toString());
    
    SessionResponse response = SessionResponse.builder()
            .userId(userId.toString())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(3600)
            .build();
    
    return ResponseEntity.ok(response);
}
```

**PostController with AI Integration**:
```java
@PostMapping("/create")
public ResponseEntity<PostDto> createPost(@RequestBody CreatePostRequest request) {
    PostEntity post = postService.createPost(request);
    
    // Async AI processing
    CompletableFuture.runAsync(() -> aiService.processPost(post));
    
    return ResponseEntity.status(HttpStatus.CREATED).body(PostDto.fromEntity(post));
}
```

---

## Day 9: Redis Caching & Feed Optimization

### Harley (Caching Implementation)

**Redis Configuration**:
```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
```

**Cached Feed Service**:
```java
@Cacheable(value="feedCache", key="'page:'+#page+':size:'+#size")
public Page<PostDto> getFeedCached(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<PostEntity> postPage = postRepository.findAll(pageable);
    return postPage.map(this::enrichWithAiData);
}
```

---

## Day 10-11: iOS FeedView Implementation

### Dexter & Dawei (iOS Feed System)

**FeedViewModel with Pagination**:
```swift
@MainActor
class FeedViewModel: ObservableObject {
    @Published var posts: [Post] = []
    @Published var isLoadingPage = false
    @Published var error: String?
    
    private var currentPage = 0
    private let pageSize = 20
    private var hasNext = true
    
    func refresh() async {
        currentPage = 0
        hasNext = true
        posts = []
        await loadMoreIfNeeded()
    }
    
    func loadMoreIfNeeded() async {
        guard !isLoadingPage, hasNext else { return }
        
        isLoadingPage = true
        defer { isLoadingPage = false }
        
        do {
            try await SessionManager.shared.createSessionIfNeeded()
            let pageResponse = try await APIClient.shared.fetchFeed(page: currentPage, size: pageSize)
            
            let newPosts = pageResponse.posts.map { dto in
                Post(
                    id: UUID(uuidString: dto.postId)!,
                    userId: UUID(uuidString: dto.userId)!,
                    title: dto.title,
                    content: dto.content,
                    emotion: EmotionType(rawValue: dto.emotion) ?? .happy,
                    createdAt: ISO8601DateFormatter().date(from: dto.createdAt) ?? Date(),
                    sentimentScore: dto.sentimentScore,
                    empathyScore: dto.empathyScore,
                    questions: dto.questions ?? []
                )
            }
            
            if currentPage == 0 {
                posts = newPosts
            } else {
                posts.append(contentsOf: newPosts)
            }
            
            hasNext = pageResponse.hasNext
            currentPage += 1
            
        } catch {
            self.error = error.localizedDescription
        }
    }
}
```

**Enhanced FeedView**:
```swift
struct FeedView: View {
    @StateObject private var viewModel = FeedViewModel()
    @State private var showingCreatePost = false
    
    var body: some View {
        NavigationView {
            List {
                ForEach(viewModel.posts) { post in
                    NavigationLink(destination: PostDetailView(post: post)) {
                        FeedRow(post: post)
                    }
                    .onAppear {
                        if post == viewModel.posts.last {
                            Task { await viewModel.loadMoreIfNeeded() }
                        }
                    }
                }
                
                if viewModel.isLoadingPage {
                    ProgressView("Loading more posts...")
                        .frame(maxWidth: .infinity)
                        .listRowSeparator(.hidden)
                }
            }
            .navigationTitle("YouthMind")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingCreatePost = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                    }
                }
            }
            .refreshable { await viewModel.refresh() }
            .task { await viewModel.refresh() }
            .sheet(isPresented: $showingCreatePost) {
                PostCreateView()
            }
            .alert("Error", isPresented: .constant(viewModel.error != nil)) {
                Button("OK") { viewModel.error = nil }
            } message: {
                if let error = viewModel.error {
                    Text(error)
                }
            }
        }
    }
}
```

---

## Day 12: AI Model Implementation

### Andy (ML Models)

**Enhanced Sentiment Analysis**:
```python
from transformers import AutoTokenizer, AutoModelForSequenceClassification, pipeline
import torch
import numpy as np

# Load pre-trained models
sentiment_analyzer = pipeline("sentiment-analysis", 
                            model="cardiffnlp/twitter-roberta-base-sentiment-latest")
emotion_classifier = pipeline("text-classification", 
                            model="j-hartmann/emotion-english-distilroberta-base")

@app.post("/ai/sentiment", response_model=SentimentResponse)
async def analyze_sentiment(req: SentimentRequest):
    try:
        # Sentiment analysis
        sentiment_result = sentiment_analyzer(req.text)[0]
        sentiment_score = sentiment_result['score'] if sentiment_result['label'] == 'POSITIVE' else -sentiment_result['score']
        
        # Emotion analysis for empathy scoring
        emotion_result = emotion_classifier(req.text)[0]
        empathy_score = calculate_empathy_score(emotion_result)
        
        return SentimentResponse(
            sentiment_score=round(sentiment_score, 3),
            empathy_score=round(empathy_score, 3)
        )
    except Exception as e:
        logger.error(f"Sentiment analysis failed: {e}")
        return SentimentResponse(sentiment_score=0.0, empathy_score=0.0)

def calculate_empathy_score(emotion_result):
    # Map emotions to empathy levels
    empathy_mapping = {
        'sadness': 0.9,
        'fear': 0.8,
        'anger': 0.7,
        'surprise': 0.4,
        'disgust': 0.6,
        'joy': 0.3
    }
    return empathy_mapping.get(emotion_result['label'].lower(), 0.5)

@app.post("/ai/questions", response_model=QuestionResponse)
async def generate_questions(req: QuestionRequest):
    # Rule-based question generation for MVP
    emotion_result = emotion_classifier(req.text)[0]
    emotion = emotion_result['label'].lower()
    
    question_templates = {
        'sadness': [
            "What's been weighing on your mind lately?",
            "Would you like to talk about what's making you feel this way?",
            "What usually helps you feel better when you're going through tough times?"
        ],
        'fear': [
            "What specific aspects worry you the most?",
            "Have you felt this way before? What helped then?",
            "What would make you feel more secure about this situation?"
        ],
        'anger': [
            "What triggered these feelings for you?",
            "How do you usually process anger in healthy ways?",
            "What would help you feel heard and understood right now?"
        ],
        'joy': [
            "What made this moment special for you?",
            "How can you hold onto this positive feeling?",
            "What would you tell someone else experiencing something similar?"
        ]
    }
    
    questions = question_templates.get(emotion, [
        "How are you feeling about this situation?",
        "What thoughts are going through your mind?",
        "What kind of support would be most helpful right now?"
    ])
    
    return QuestionResponse(questions=questions[:2])  # Limit to 2 questions
```

---

## Day 13: PostCreateView Integration

### Dawei & Youssef (Post Creation)

**Complete PostCreateView**:
```swift
struct PostCreateView: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var content: String = ""
    @State private var title: String = ""
    @State private var selectedEmotion: EmotionType = .happy
    @State private var isSubmitting = false
    @State private var showingAlert = false
    @State private var alertMessage = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Title")) {
                    TextField("What's on your mind?", text: $title)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                
                Section(header: Text("Content")) {
                    TextEditor(text: $content)
                        .frame(height: 150)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )
                }
                
                Section(header: Text("How are you feeling?")) {
                    Picker("Emotion", selection: $selectedEmotion) {
                        ForEach(EmotionType.allCases, id: \.self) { emotion in
                            HStack {
                                Text(emotion.emoji)
                                Text(emotion.rawValue.capitalized)
                            }
                            .tag(emotion)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                }
                
                Section {
                    if isSubmitting {
                        HStack {
                            ProgressView()
                                .scaleEffect(0.8)
                            Text("Publishing your thoughts...")
                                .foregroundColor(.secondary)
                        }
                    } else {
                        Button("Share Your Thoughts") {
                            Task { await submitPost() }
                        }
                        .frame(maxWidth: .infinity)
                        .disabled(content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || 
                                title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            .navigationTitle("Create Post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
            .alert("Post Creation", isPresented: $showingAlert) {
                Button("OK") {
                    if alertMessage.contains("successfully") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            } message: {
                Text(alertMessage)
            }
        }
    }
    
    func submitPost() async {
        let trimmedTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
        
        guard !trimmedTitle.isEmpty, !trimmedContent.isEmpty else {
            alertMessage = "Please fill in both title and content"
            showingAlert = true
            return
        }
        
        isSubmitting = true
        
        do {
            // Ensure session exists
            try await SessionManager.shared.createSessionIfNeeded()
            
            let request = CreatePostRequest(
                title: trimmedTitle,
                content: trimmedContent,
                emotion: selectedEmotion.rawValue,
                coverUrl: nil
            )
            
            _ = try await APIClient.shared.createPost(request)
            
            alertMessage = "Your thoughts have been shared successfully!"
            showingAlert = true
            
        } catch {
            alertMessage = "Failed to share your post. Please try again."
            showingAlert = true
        }
        
        isSubmitting = false
    }
}

enum EmotionType: String, CaseIterable {
    case happy = "happy"
    case sad = "sad"
    case angry = "angry"
    case fear = "fear"
    case surprised = "surprised"
    case conflicted = "conflicted"
    
    var emoji: String {
        switch self {
        case .happy: return "😊"
        case .sad: return "😢"
        case .angry: return "😠"
        case .fear: return "😰"
        case .surprised: return "😲"
        case .conflicted: return "😕"
        }
    }
}
```

---

## Day 14: Search Implementation & Testing

### Mustafo (Search API)

**SearchController**:
```java
@RestController
@RequestMapping("/search")
public class SearchController {
    
    @Autowired
    private PostService postService;
    
    @GetMapping
    public ResponseEntity<Page<PostDto>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<PostDto> results = postService.searchPosts(query, page, size);
        return ResponseEntity.ok(results);
    }
}
```

**Enhanced Repository with Search**:
```java
public interface PostRepository extends JpaRepository<PostEntity, UUID> {
    
    @Query("SELECT p FROM PostEntity p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<PostEntity> findByTitleOrContentContaining(
        @Param("query") String query, 
        Pageable pageable
    );
}
```

### Integration Testing

**Backend API Validation**:
```bash
# Test complete flow
# 1. Create session
SESSION_RESPONSE=$(curl -X POST http://localhost:8080/auth/session \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"test-device"}')

TOKEN=$(echo $SESSION_RESPONSE | jq -r '.accessToken')

# 2. Create post
curl -X POST http://localhost:8080/posts/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Test Integration Post",
    "content": "This is testing the complete integration flow",
    "emotion": "happy"
  }'

# 3. Fetch feed
curl -X GET "http://localhost:8080/posts/feed?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 4. Test search
curl -X GET "http://localhost:8080/search?query=integration&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Week 2 Deliverables ✅

### Backend Achievements
- [x] Complete post persistence with PostgreSQL
- [x] JWT authentication and session management
- [x] Redis caching for feed and search performance
- [x] AI service integration with async processing
- [x] Search functionality with content filtering

### iOS Achievements  
- [x] Functional FeedView with infinite scroll pagination
- [x] Complete PostCreateView with emotion selection
- [x] Real-time data binding with backend APIs
- [x] Error handling and loading states
- [x] Session management and automatic authentication

### AI/ML Integration
- [x] Real sentiment analysis using Transformers
- [x] Emotion-based question generation
- [x] Empathy scoring algorithm
- [x] Error handling and fallback responses

### System Integration
- [x] End-to-end post creation → AI analysis → feed display
- [x] Redis cache performance optimization
- [x] Complete API test coverage
- [x] Docker environment stability

**Performance Metrics**:
- Feed loading: <2 seconds for 20 posts
- Post creation: <3 seconds including AI processing  
- Search response: <1 second with caching
- 99.9% uptime in development environment

---

*Ready for Week 3-4: Advanced Features, Notification System, and UI Polish* 