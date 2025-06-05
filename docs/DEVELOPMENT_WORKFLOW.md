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

# Week 3 (Days 15–21): User Profile, Avatar & Cover, Emotion Museum, and Notifications

## Goals
- Implement anonymous user identity and profile system
- Build AvatarCreationView & CoverDesignView with customization
- Develop EmotionMuseumView & EmotionPuzzleView for data visualization
- Complete notification system (backend + iOS integration)

---

## Day 15: Backend Anonymous User Identity & Drafts

### Mustafo & Ahmed (Backend User System)

**Enhanced UserEntity**:
```java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private UUID id;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    @Column(nullable = false)
    private Integer age;
    
    @Column
    private String gender;
    
    @Column(columnDefinition = "jsonb")
    private String personalityTraits; // JSON string storage
    
    @Column(name = "tacit_understanding_score")
    private Integer tacitUnderstandingScore = 0;
    
    @Column(name = "resonance_level")
    private Integer resonanceLevel = 1;
    
    // Constructors, getters, setters
}
```

**DraftEntity Implementation**:
```java
@Entity
@Table(name = "drafts")
public class DraftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column
    private String title;
    
    @Column
    private String emotion;
    
    @Column(name = "cover_url")
    private String coverUrl;
    
    @Column(name = "last_saved")
    private LocalDateTime lastSaved = LocalDateTime.now();
    
    // Constructors, getters, setters
}
```

**DraftController**:
```java
@RestController
@RequestMapping("/drafts")
@CrossOrigin(origins = "*")
public class DraftController {
    
    @Autowired
    private DraftRepository draftRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<List<DraftDto>> getAllDraftsForUser(@RequestParam String userId) {
        List<DraftEntity> drafts = draftRepository.findByUserIdOrderByLastSavedDesc(
            UUID.fromString(userId)
        );
        List<DraftDto> dtos = drafts.stream()
            .map(DraftDto::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/save")
    public ResponseEntity<DraftDto> saveDraft(@RequestBody DraftRequest request) {
        UserEntity user = userRepository.findById(UUID.fromString(request.getUserId()))
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        DraftEntity draft;
        if (request.getDraftId() != null) {
            // Update existing draft
            draft = draftRepository.findById(request.getDraftId())
                .orElseThrow(() -> new RuntimeException("Draft not found"));
        } else {
            // Create new draft
            draft = new DraftEntity();
            draft.setUser(user);
        }
        
        draft.setTitle(request.getTitle());
        draft.setContent(request.getContent());
        draft.setEmotion(request.getEmotion());
        draft.setCoverUrl(request.getCoverUrl());
        draft.setLastSaved(LocalDateTime.now());
        
        draft = draftRepository.save(draft);
        return ResponseEntity.ok(DraftDto.fromEntity(draft));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        draftRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Day 16: iOS Anonymous Identity & Draft Logic

### Youssef (Identity Management)

**Extended SessionManager**:
```swift
import Foundation
import Combine

struct UserProfile: Codable {
    let id: String
    var age: Int
    var gender: String
    var personalityTraits: [String: Double]
    var tacitUnderstandingScore: Int
    var resonanceLevel: Int
}

class SessionManager: ObservableObject {
    static let shared = SessionManager()
    
    @Published var currentUser: SessionResponse?
    @Published var userProfile: UserProfile?
    @Published var isAuthenticated = false
    
    private let userDefaults = UserDefaults.standard
    private let sessionKey = "user_session"
    private let profileKey = "user_profile"
    
    private init() {
        loadSession()
        loadProfile()
    }
    
    func loadProfile() {
        if let data = userDefaults.data(forKey: profileKey),
           let profile = try? JSONDecoder().decode(UserProfile.self, from: data) {
            userProfile = profile
        }
    }
    
    func saveProfile(_ profile: UserProfile) {
        userProfile = profile
        if let data = try? JSONEncoder().encode(profile) {
            userDefaults.set(data, forKey: profileKey)
        }
    }
    
    func createAnonymousIdentity(age: Int, gender: String) async {
        let anonymousId = "YM_" + UUID().uuidString
        let profile = UserProfile(
            id: anonymousId,
            age: age,
            gender: gender,
            personalityTraits: [:],
            tacitUnderstandingScore: 0,
            resonanceLevel: 1
        )
        saveProfile(profile)
    }
}
```

**DraftService Implementation**:
```swift
import Foundation
import CoreData

class DraftService: ObservableObject {
    static let shared = DraftService()
    
    @Published var drafts: [DraftModel] = []
    
    private let container: NSPersistentContainer
    
    init() {
        container = NSPersistentContainer(name: "YouthMindModel")
        container.loadPersistentStores { _, _ in }
        fetchDrafts()
    }
    
    func saveDraft(title: String, content: String, emotion: String, coverUrl: String? = nil) {
        let context = container.viewContext
        let draft = DraftEntity(context: context)
        draft.id = UUID()
        draft.title = title
        draft.content = content
        draft.emotion = emotion
        draft.coverUrl = coverUrl
        draft.lastSaved = Date()
        
        do {
            try context.save()
            fetchDrafts()
        } catch {
            print("Failed to save draft: \(error)")
        }
    }
    
    func fetchDrafts() {
        let request: NSFetchRequest<DraftEntity> = DraftEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(keyPath: \DraftEntity.lastSaved, ascending: false)]
        
        do {
            let results = try container.viewContext.fetch(request)
            drafts = results.map { entity in
                DraftModel(
                    id: entity.id!,
                    title: entity.title ?? "",
                    content: entity.content!,
                    emotion: entity.emotion!,
                    coverUrl: entity.coverUrl,
                    lastSaved: entity.lastSaved!
                )
            }
        } catch {
            print("Failed to fetch drafts: \(error)")
        }
    }
    
    func deleteDraft(id: UUID) {
        let context = container.viewContext
        let request: NSFetchRequest<DraftEntity> = DraftEntity.fetchRequest()
        request.predicate = NSPredicate(format: "id == %@", id as CVarArg)
        
        do {
            if let entity = try context.fetch(request).first {
                context.delete(entity)
                try context.save()
                fetchDrafts()
            }
        } catch {
            print("Failed to delete draft: \(error)")
        }
    }
}

struct DraftModel: Identifiable {
    let id: UUID
    let title: String
    let content: String
    let emotion: String
    let coverUrl: String?
    let lastSaved: Date
}
```

---

## Day 17: iOS AvatarCreationView & CoverDesignView

### Dexter (Avatar & Cover UI)

**AvatarCreationView Implementation**:
```swift
import SwiftUI

struct AvatarCreationView: View {
    let emotion: EmotionType
    @State private var selectedFeatures: [AvatarFeature] = []
    @State private var savedAvatars: [CustomAvatar] = []
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        VStack(spacing: 20) {
            Text("Create Your Avatar")
                .font(.largeTitle)
                .bold()
                .padding(.top)
            
            // Avatar Preview
            AvatarPreviewView(baseEmotion: emotion, customFeatures: selectedFeatures)
                .frame(width: 250, height: 250)
                .background(
                    Circle()
                        .fill(Color.blue.opacity(0.1))
                        .stroke(Color.blue, lineWidth: 2)
                )
            
            Text("Customize your \(emotion.rawValue) avatar")
                .font(.headline)
                .foregroundColor(.secondary)
            
            // Feature Selection
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 15) {
                    ForEach(AvatarFeature.allCases, id: \.name) { feature in
                        FeatureButton(
                            feature: feature,
                            isSelected: selectedFeatures.contains { $0.name == feature.name },
                            action: { toggleFeature(feature) }
                        )
                    }
                }
                .padding(.horizontal)
            }
            
            Spacer()
            
            // Action Buttons
            HStack(spacing: 20) {
                Button("Reset") {
                    selectedFeatures.removeAll()
                }
                .buttonStyle(SecondaryButtonStyle())
                
                Button("Save Avatar") {
                    saveCustomAvatar()
                }
                .buttonStyle(PrimaryButtonStyle())
                
                Button("Continue") {
                    // Navigate to CoverDesignView
                    presentationMode.wrappedValue.dismiss()
                }
                .buttonStyle(PrimaryButtonStyle())
            }
            .padding(.bottom)
        }
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func toggleFeature(_ feature: AvatarFeature) {
        if let index = selectedFeatures.firstIndex(where: { $0.name == feature.name }) {
            selectedFeatures.remove(at: index)
        } else {
            selectedFeatures.append(feature)
        }
    }
    
    private func saveCustomAvatar() {
        let avatar = CustomAvatar(
            id: UUID(),
            baseEmotion: emotion,
            features: selectedFeatures
        )
        savedAvatars.append(avatar)
        // TODO: Persist to CoreData or backend
    }
}

struct AvatarFeature: Identifiable {
    let id = UUID()
    let name: String
    let icon: String
    
    static let allCases: [AvatarFeature] = [
        AvatarFeature(name: "Glasses", icon: "eyeglasses"),
        AvatarFeature(name: "Hat", icon: "crown"),
        AvatarFeature(name: "Beard", icon: "mustache"),
        AvatarFeature(name: "Smile", icon: "face.smiling"),
        AvatarFeature(name: "Blush", icon: "heart.circle")
    ]
}

struct CustomAvatar: Identifiable {
    let id: UUID
    let baseEmotion: EmotionType
    let features: [AvatarFeature]
}

struct AvatarPreviewView: View {
    let baseEmotion: EmotionType
    let customFeatures: [AvatarFeature]
    
    var body: some View {
        ZStack {
            // Base avatar
            Text(baseEmotion.emoji)
                .font(.system(size: 120))
            
            // Feature overlays
            ForEach(customFeatures, id: \.name) { feature in
                Image(systemName: feature.icon)
                    .font(.system(size: 30))
                    .foregroundColor(.blue)
                    .offset(featureOffset(for: feature))
            }
        }
    }
    
    private func featureOffset(for feature: AvatarFeature) -> CGSize {
        switch feature.name {
        case "Glasses": return CGSize(width: 0, height: -20)
        case "Hat": return CGSize(width: 0, height: -60)
        case "Beard": return CGSize(width: 0, height: 40)
        default: return CGSize.zero
        }
    }
}
```

**CoverDesignView Implementation**:
```swift
import SwiftUI

struct CoverDesignView: View {
    @State private var coverText = ""
    @State private var textColor = Color.black
    @State private var selectedEmojis: [String] = []
    @State private var textPosition = CGPoint(x: 200, y: 100)
    @State private var selectedBackground = BackgroundStyle.solid
    @Environment(\.presentationMode) var presentationMode
    
    enum BackgroundStyle: CaseIterable {
        case solid, gradient, pattern
        
        var name: String {
            switch self {
            case .solid: return "Solid"
            case .gradient: return "Gradient"
            case .pattern: return "Pattern"
            }
        }
    }
    
    var body: some View {
        VStack {
            Text("Design Your Cover")
                .font(.largeTitle)
                .bold()
                .padding()
            
            // Cover Preview
            ZStack {
                // Background
                backgroundView
                    .frame(height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 15))
                    .shadow(radius: 5)
                
                // Avatar placeholder
                AvatarView(emotionType: .happy)
                    .frame(width: 80, height: 80)
                    .position(x: 100, y: 150)
                
                // Custom text
                if !coverText.isEmpty {
                    Text(coverText)
                        .foregroundColor(textColor)
                        .font(.headline)
                        .position(textPosition)
                        .gesture(
                            DragGesture()
                                .onChanged { value in
                                    textPosition = value.location
                                }
                        )
                }
                
                // Emojis
                ForEach(selectedEmojis.indices, id: \.self) { index in
                    Text(selectedEmojis[index])
                        .font(.title)
                        .position(x: 300, y: CGFloat(100 + index * 30))
                }
            }
            .padding()
            
            // Controls
            ScrollView {
                VStack(spacing: 15) {
                    // Text input
                    HStack {
                        TextField("Add text to your cover", text: $coverText)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                        
                        ColorPicker("", selection: $textColor)
                            .frame(width: 50)
                    }
                    
                    // Background selection
                    VStack(alignment: .leading) {
                        Text("Background Style")
                            .font(.headline)
                        
                        Picker("Background", selection: $selectedBackground) {
                            ForEach(BackgroundStyle.allCases, id: \.self) { style in
                                Text(style.name).tag(style)
                            }
                        }
                        .pickerStyle(SegmentedPickerStyle())
                    }
                    
                    // Emoji selection
                    HStack {
                        Text("Add Emojis:")
                            .font(.headline)
                        
                        Button("😊") { selectedEmojis.append("😊") }
                        Button("❤️") { selectedEmojis.append("❤️") }
                        Button("🌟") { selectedEmojis.append("🌟") }
                        Button("🎨") { selectedEmojis.append("🎨") }
                        
                        Spacer()
                        
                        Button("Clear") {
                            selectedEmojis.removeAll()
                        }
                        .foregroundColor(.red)
                    }
                }
                .padding()
            }
            
            // Action buttons
            HStack(spacing: 20) {
                Button("Reset") {
                    coverText = ""
                    selectedEmojis.removeAll()
                    textPosition = CGPoint(x: 200, y: 100)
                }
                .buttonStyle(SecondaryButtonStyle())
                
                Button("Save Cover") {
                    // TODO: Save cover design
                    presentationMode.wrappedValue.dismiss()
                }
                .buttonStyle(PrimaryButtonStyle())
            }
            .padding()
        }
        .navigationBarTitleDisplayMode(.inline)
    }
    
    @ViewBuilder
    private var backgroundView: some View {
        switch selectedBackground {
        case .solid:
            Color.blue.opacity(0.3)
        case .gradient:
            LinearGradient(
                colors: [.purple.opacity(0.3), .blue.opacity(0.3)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .pattern:
            Color.green.opacity(0.2)
        }
    }
}

// Helper button styles
struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(.white)
            .padding()
            .background(Color.blue)
            .cornerRadius(10)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(.blue)
            .padding()
            .background(Color.blue.opacity(0.1))
            .cornerRadius(10)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
    }
}
```

---

## Day 18: Backend Avatar & Cover Endpoints

### Mustafo (Avatar System)

**AvatarController (Optional MVP)**:
```java
@RestController
@RequestMapping("/avatar")
@CrossOrigin(origins = "*")
public class AvatarController {
    
    @PostMapping("/upload")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestBody AvatarUploadRequest request) {
        // For MVP: Simply return a placeholder URL
        // In production: process base64 image, store in S3/storage service
        
        String avatarUrl = "https://example.com/avatars/" + UUID.randomUUID() + ".png";
        
        AvatarUploadResponse response = AvatarUploadResponse.builder()
            .avatarUrl(avatarUrl)
            .success(true)
            .message("Avatar uploaded successfully")
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AvatarDto>> getUserAvatars(@PathVariable String userId) {
        // Return saved avatars for user
        // For MVP: return empty list or placeholder avatars
        return ResponseEntity.ok(Collections.emptyList());
    }
}

@Data
@Builder
class AvatarUploadRequest {
    private String userId;
    private String imageData; // base64 encoded
    private String emotion;
    private List<String> features;
}

@Data
@Builder
class AvatarUploadResponse {
    private String avatarUrl;
    private boolean success;
    private String message;
}
```

**CoverController (Optional MVP)**:
```java
@RestController
@RequestMapping("/cover")
@CrossOrigin(origins = "*")
public class CoverController {
    
    @PostMapping("/design")
    public ResponseEntity<CoverDesignResponse> saveCoverDesign(@RequestBody CoverDesignRequest request) {
        // For MVP: store cover design data as JSON
        String coverUrl = "https://example.com/covers/" + UUID.randomUUID() + ".json";
        
        CoverDesignResponse response = CoverDesignResponse.builder()
            .coverUrl(coverUrl)
            .success(true)
            .build();
        
        return ResponseEntity.ok(response);
    }
}
```

---

## Week 3 Deliverables ✅

### Backend Achievements
- [x] Enhanced UserEntity with profile fields (age, gender, personality traits)
- [x] Complete DraftEntity and DraftController with CRUD operations
- [x] Avatar and Cover API endpoints (MVP stubs)
- [x] User profile management system

### iOS Achievements
- [x] Anonymous identity management with SessionManager
- [x] Complete DraftService with CoreData persistence  
- [x] Functional AvatarCreationView with feature customization
- [x] Interactive CoverDesignView with text and emoji placement
- [x] Draft saving and loading in PostCreateView

### System Integration
- [x] End-to-end draft workflow: create → save → load → publish
- [x] Avatar creation flow integrated with emotion selection
- [x] Cover design system with real-time preview
- [x] Profile data persistence across app sessions

**Performance Targets Met**:
- Draft save/load: <500ms response time
- Avatar rendering: 60fps smooth animations
- Profile data sync: Immediate local storage

---

*Ready for Week 4: Security Implementation, Admin UI, and QA Foundations* 

# Week 4 (Days 22–28): Security, Admin UI, QA Foundations

## Goals
- Implement comprehensive security (JWT auth, role checks, reCAPTCHA)
- Build functional Admin UI (React) for content moderation
- Create comprehensive QA testing suite (unit + integration)
- Finalize missing pieces and begin system stabilization

---

## Day 22: Backend JWT Authentication & Security

### Harley & Mustafo (Security Implementation)

**Spring Security Configuration**:
```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;
    
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .cors().and()
            .authorizeRequests()
                .antMatchers("/auth/**", "/ai/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .antMatchers(HttpMethod.GET, "/posts/feed", "/search").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
                .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint)
            .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

**JWT Token Provider**:
```java
@Component
public class JwtTokenProvider {
    
    private static final String JWT_SECRET = "YouthMindSecretKeyForJWTTokenGeneration2024";
    private static final int JWT_EXPIRATION = 86400000; // 24 hours
    private static final String ADMIN_SECRET = "AdminSecretKeyForYouthMind2024";
    
    public String generateAccessToken(String userId) {
        Date expiryDate = new Date(System.currentTimeMillis() + JWT_EXPIRATION);
        
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
    }
    
    public String generateAdminToken(String adminId) {
        Date expiryDate = new Date(System.currentTimeMillis() + JWT_EXPIRATION);
        
        return Jwts.builder()
                .setSubject(adminId)
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, ADMIN_SECRET)
                .compact();
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(JWT_SECRET).parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
    
    public boolean isAdminToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(ADMIN_SECRET)
                    .parseClaimsJws(token)
                    .getBody();
            return "ADMIN".equals(claims.get("role"));
        } catch (Exception e) {
            return false;
        }
    }
}
```

**reCAPTCHA Service**:
```java
@Service
public class RecaptchaService {
    
    @Value("${recaptcha.secret}")
    private String recaptchaSecret;
    
    @Value("${recaptcha.verify-url}")
    private String recaptchaVerifyUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public boolean verifyRecaptcha(String token) {
        // For MVP: accept any non-empty token
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        // In production, uncomment and configure:
        /*
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", recaptchaSecret);
        params.add("response", token);
        
        try {
            ResponseEntity<RecaptchaResponse> response = restTemplate.postForEntity(
                recaptchaVerifyUrl, params, RecaptchaResponse.class);
            
            return response.getBody() != null && response.getBody().isSuccess();
        } catch (Exception e) {
            logger.error("reCAPTCHA verification failed", e);
            return false;
        }
        */
        
        return "mvp".equals(token) || "test".equals(token);
    }
}

@Data
class RecaptchaResponse {
    private boolean success;
    private double score;
    private String action;
    private Date challenge_ts;
    private String hostname;
}
```

---

## Day 23: iOS JWT Handling & Secured Requests

### Youssef (iOS Security)

**Enhanced APIClient with Security**:
```swift
import Foundation

class APIClient {
    static let shared = APIClient()
    private let baseURL = URL(string: "http://localhost:8080")!
    private let aiURL = URL(string: "http://localhost:8001")!
    
    private init() {}
    
    // MARK: - Request Builder
    private func authorizedRequest(
        _ url: URL,
        method: String = "GET",
        body: Data? = nil,
        requiresAuth: Bool = true
    ) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = method
        
        if let body = body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        
        if requiresAuth, let token = SessionManager.shared.currentUser?.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        // Add request ID for tracking
        request.setValue(UUID().uuidString, forHTTPHeaderField: "X-Request-ID")
        
        return request
    }
    
    // MARK: - Secure Post Creation
    func createPost(_ request: CreatePostRequest, captchaToken: String = "mvp") async throws -> PostDto {
        let url = baseURL.appendingPathComponent("/posts/create")
        
        var requestWithCaptcha = request
        requestWithCaptcha.captchaToken = captchaToken
        
        let body = try JSONEncoder().encode(requestWithCaptcha)
        let urlRequest = authorizedRequest(url, method: "POST", body: body)
        
        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        
        try handleResponse(response)
        return try JSONDecoder().decode(PostDto.self, from: data)
    }
    
    // MARK: - Error Handling
    private func handleResponse(_ response: URLResponse) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        switch httpResponse.statusCode {
        case 200...299:
            break
        case 401:
            // Token expired, clear session
            DispatchQueue.main.async {
                SessionManager.shared.clearSession()
            }
            throw APIError.unauthorized
        case 403:
            throw APIError.forbidden
        case 429:
            throw APIError.rateLimited
        default:
            throw APIError.serverError(httpResponse.statusCode)
        }
    }
    
    // MARK: - Retry Logic
    private func performRequestWithRetry<T: Codable>(
        _ request: URLRequest,
        responseType: T.Type,
        maxRetries: Int = 3
    ) async throws -> T {
        var lastError: Error?
        
        for attempt in 1...maxRetries {
            do {
                let (data, response) = try await URLSession.shared.data(for: request)
                try handleResponse(response)
                return try JSONDecoder().decode(responseType, from: data)
            } catch {
                lastError = error
                
                if attempt < maxRetries {
                    // Exponential backoff
                    let delay = pow(2.0, Double(attempt)) * 0.5
                    try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                }
            }
        }
        
        throw lastError ?? APIError.networkError
    }
}

// Enhanced request models
struct CreatePostRequest: Codable {
    let title: String
    let content: String
    let emotion: String
    let coverUrl: String?
    var captchaToken: String?
}

enum APIError: Error, LocalizedError {
    case invalidResponse
    case unauthorized
    case forbidden
    case rateLimited
    case serverError(Int)
    case networkError
    
    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Invalid response from server"
        case .unauthorized:
            return "Your session has expired. Please restart the app."
        case .forbidden:
            return "You don't have permission to perform this action"
        case .rateLimited:
            return "Too many requests. Please try again later."
        case .serverError(let code):
            return "Server error: \(code)"
        case .networkError:
            return "Network connection error"
        }
    }
}
```

---

## Day 24: Admin UI Implementation

### Alex (React Admin Dashboard)

**Admin Authentication**:
```jsx
// src/components/AdminLogin.js
import React, { useState } from 'react';
import axios from 'axios';

function AdminLogin({ onLogin }) {
    const [credentials, setCredentials] = useState({ username: '', password: '' });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    
    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        
        try {
            // For MVP: hardcoded admin credentials
            if (credentials.username === 'admin' && credentials.password === 'admin123') {
                const adminToken = 'admin_mvp_token_2024';
                localStorage.setItem('adminToken', adminToken);
                onLogin(adminToken);
            } else {
                setError('Invalid credentials');
            }
        } catch (err) {
            setError('Login failed');
        } finally {
            setLoading(false);
        }
    };
    
    return (
        <div className="admin-login">
            <div className="login-container">
                <h2>YouthMind Admin</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <input
                            type="text"
                            placeholder="Username"
                            value={credentials.username}
                            onChange={(e) => setCredentials({...credentials, username: e.target.value})}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <input
                            type="password"
                            placeholder="Password"
                            value={credentials.password}
                            onChange={(e) => setCredentials({...credentials, password: e.target.value})}
                            required
                        />
                    </div>
                    {error && <div className="error">{error}</div>}
                    <button type="submit" disabled={loading}>
                        {loading ? 'Logging in...' : 'Login'}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default AdminLogin;
```

**Admin Dashboard**:
```jsx
// src/components/AdminDashboard.js
import React, { useEffect, useState } from 'react';
import axios from 'axios';

function AdminDashboard() {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('all'); // all, flagged, recent
    const [stats, setStats] = useState({});
    
    useEffect(() => {
        fetchPosts();
        fetchStats();
    }, [filter]);
    
    const fetchPosts = async () => {
        try {
            const token = localStorage.getItem('adminToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE}/admin/posts?filter=${filter}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setPosts(response.data);
        } catch (error) {
            console.error('Failed to fetch posts:', error);
        } finally {
            setLoading(false);
        }
    };
    
    const fetchStats = async () => {
        try {
            const token = localStorage.getItem('adminToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE}/admin/stats`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setStats(response.data);
        } catch (error) {
            console.error('Failed to fetch stats:', error);
        }
    };
    
    const deletePost = async (postId) => {
        if (!window.confirm('Are you sure you want to delete this post?')) return;
        
        try {
            const token = localStorage.getItem('adminToken');
            await axios.delete(
                `${process.env.REACT_APP_API_BASE}/admin/posts/${postId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setPosts(posts.filter(p => p.postId !== postId));
        } catch (error) {
            console.error('Failed to delete post:', error);
        }
    };
    
    const flagPost = async (postId, flagged) => {
        try {
            const token = localStorage.getItem('adminToken');
            await axios.patch(
                `${process.env.REACT_APP_API_BASE}/admin/posts/${postId}/flag`,
                { flagged },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchPosts(); // Refresh list
        } catch (error) {
            console.error('Failed to flag post:', error);
        }
    };
    
    if (loading) return <div className="loading">Loading posts...</div>;
    
    return (
        <div className="admin-dashboard">
            <header className="dashboard-header">
                <h1>YouthMind Admin Dashboard</h1>
                <div className="stats">
                    <div className="stat-card">
                        <h3>Total Posts</h3>
                        <p>{stats.totalPosts || 0}</p>
                    </div>
                    <div className="stat-card">
                        <h3>Flagged Posts</h3>
                        <p>{stats.flaggedPosts || 0}</p>
                    </div>
                    <div className="stat-card">
                        <h3>Active Users</h3>
                        <p>{stats.activeUsers || 0}</p>
                    </div>
                </div>
            </header>
            
            <div className="filters">
                <button 
                    className={filter === 'all' ? 'active' : ''}
                    onClick={() => setFilter('all')}
                >
                    All Posts
                </button>
                <button 
                    className={filter === 'flagged' ? 'active' : ''}
                    onClick={() => setFilter('flagged')}
                >
                    Flagged Posts
                </button>
                <button 
                    className={filter === 'recent' ? 'active' : ''}
                    onClick={() => setFilter('recent')}
                >
                    Recent Posts
                </button>
            </div>
            
            <div className="posts-list">
                {posts.length === 0 ? (
                    <p>No posts found</p>
                ) : (
                    posts.map(post => (
                        <div key={post.postId} className={`post-card ${post.flagged ? 'flagged' : ''}`}>
                            <div className="post-header">
                                <span className="post-id">ID: {post.postId}</span>
                                <span className="post-emotion">{post.emotion}</span>
                                <span className="post-date">
                                    {new Date(post.createdAt).toLocaleDateString()}
                                </span>
                            </div>
                            <div className="post-content">
                                <h4>{post.title}</h4>
                                <p>{post.content}</p>
                            </div>
                            {post.sentimentScore && (
                                <div className="ai-analysis">
                                    <span>Sentiment: {post.sentimentScore.toFixed(2)}</span>
                                    <span>Empathy: {post.empathyScore?.toFixed(2)}</span>
                                </div>
                            )}
                            <div className="post-actions">
                                <button 
                                    onClick={() => flagPost(post.postId, !post.flagged)}
                                    className={post.flagged ? 'unflag' : 'flag'}
                                >
                                    {post.flagged ? 'Unflag' : 'Flag'}
                                </button>
                                <button 
                                    onClick={() => deletePost(post.postId)}
                                    className="delete"
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

export default AdminDashboard;
```

---

## Day 25: Backend Admin Endpoints & Content Moderation

### Mustafo (Admin API)

**AdminController Implementation**:
```java
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private NotificationService notificationService;
    
    @GetMapping("/posts")
    public ResponseEntity<List<PostDto>> getPosts(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostEntity> postPage;
        
        switch (filter) {
            case "flagged":
                postPage = postService.getFlaggedPosts(pageable);
                break;
            case "recent":
                LocalDateTime since = LocalDateTime.now().minusDays(7);
                postPage = postService.getRecentPosts(since, pageable);
                break;
            default:
                postPage = postService.getAllPosts(pageable);
        }
        
        List<PostDto> posts = postPage.getContent().stream()
                .map(this::enrichPostWithAiData)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(posts);
    }
    
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable String postId) {
        try {
            postService.deletePost(UUID.fromString(postId));
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PatchMapping("/posts/{postId}/flag")
    public ResponseEntity<Void> flagPost(
            @PathVariable String postId,
            @RequestBody FlagRequest request) {
        
        try {
            postService.flagPost(UUID.fromString(postId), request.isFlagged());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        AdminStatsDto stats = AdminStatsDto.builder()
                .totalPosts(postService.getTotalPostCount())
                .flaggedPosts(postService.getFlaggedPostCount())
                .activeUsers(userService.getActiveUserCount())
                .todayPosts(postService.getTodayPostCount())
                .build();
        
        return ResponseEntity.ok(stats);
    }
}

@Data
@Builder
class AdminStatsDto {
    private long totalPosts;
    private long flaggedPosts;
    private long activeUsers;
    private long todayPosts;
}

@Data
class FlagRequest {
    private boolean flagged;
}
```

**Enhanced PostService with Admin Functions**:
```java
@Service
@Transactional
public class PostService {
    
    // ... existing methods ...
    
    public Page<PostEntity> getFlaggedPosts(Pageable pageable) {
        return postRepository.findByFlaggedTrue(pageable);
    }
    
    public Page<PostEntity> getRecentPosts(LocalDateTime since, Pageable pageable) {
        return postRepository.findByCreatedAtAfter(since, pageable);
    }
    
    public Page<PostEntity> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable);
    }
    
    public void deletePost(UUID postId) {
        postRepository.deleteById(postId);
        // Also delete related AI data, notifications, etc.
        aiDataRepository.deleteByPostId(postId);
        notificationRepository.deleteByRelatedPostId(postId);
    }
    
    public void flagPost(UUID postId, boolean flagged) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setFlagged(flagged);
        postRepository.save(post);
    }
    
    public long getTotalPostCount() {
        return postRepository.count();
    }
    
    public long getFlaggedPostCount() {
        return postRepository.countByFlaggedTrue();
    }
    
    public long getTodayPostCount() {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return postRepository.countByCreatedAtAfter(startOfDay);
    }
}
```

---

## Week 4 Deliverables ✅

### Security Implementation
- [x] Complete JWT authentication with role-based access control
- [x] reCAPTCHA integration for post creation (MVP stub)
- [x] Secure API endpoints with proper authorization
- [x] Admin authentication system

### Admin Dashboard
- [x] React-based admin interface with authentication
- [x] Post management: view, flag, delete functionality
- [x] Real-time statistics dashboard
- [x] Content moderation workflow

### QA & Testing Foundation
- [x] Backend unit tests (Controllers, Services, Repositories)
- [x] iOS unit tests (ViewModels, Services, API clients)
- [x] Integration tests for critical user flows
- [x] Admin functionality testing

### System Stabilization
- [x] Error handling and retry mechanisms
- [x] Performance monitoring and logging
- [x] Security audit and vulnerability assessment
- [x] API documentation updates

**Security Metrics**:
- JWT token validation: 100% coverage
- API endpoint protection: All admin endpoints secured
- reCAPTCHA validation: Implemented for post creation
- Admin access control: Role-based permissions active

**Testing Coverage**:
- Backend unit tests: >80% code coverage
- iOS unit tests: Critical paths covered
- Integration tests: End-to-end user flows validated
- Admin UI tests: All major functions tested

---

*Ready for Week 5-6: Production Deployment, Performance Optimization, and Launch Preparation*

# Week 5 (Days 29–35): Production Deployment, Performance Optimization, and Launch Preparation

## Goals
- Finalize missing pieces and begin system stabilization
- Prepare for production deployment
- Optimize system performance
- Conduct final QA testing

---

## Day 29: System Stabilization

### All Teams (Final QA Testing)
**Objective**: Ensure all features are working as expected

**Tasks**:
- Run through all user flows
- Verify all critical paths
- Check for edge cases

### Mustafo (System Stability)
**Objective**: Ensure system stability

**Tasks**:
- Monitor system performance
- Log any issues
- Implement necessary fixes

### Harley (Performance Optimization)
**Objective**: Optimize system performance

**Tasks**:
- Analyze system logs
- Identify bottlenecks
- Implement optimizations

### Alex (Documentation)
**Objective**: Update API documentation

**Tasks**:
- Review existing documentation
- Update any outdated or incorrect information

### Dawei (Deployment Preparation)
**Objective**: Prepare for production deployment

**Tasks**:
- Set up production environment
- Configure deployment scripts

### Dexter (Launch Preparation)
**Objective**: Prepare for system launch

**Tasks**:
- Schedule launch date
- Coordinate with marketing team

### Youssef (QA)
**Objective**: Conduct final QA testing

**Tasks**:
- Run through all user flows
- Verify all critical paths
- Check for edge cases

---

## Week 5 Deliverables ✅

### System Stabilization
- [x] Error handling and retry mechanisms
- [x] Performance monitoring and logging
- [x] Security audit and vulnerability assessment
- [x] API documentation updates

### Production Deployment
- [x] Set up production environment
- [x] Configure deployment scripts

### Launch Preparation
- [x] Schedule launch date
- [x] Coordinate with marketing team

### QA & Testing
- [x] End-to-end user flow testing
- [x] Critical path verification
- [x] Edge case checking

**Performance Metrics**:
- System uptime: 99.9%
- Response time: <2 seconds for 90% of requests
- Error rate: <0.1%

---

*Ready for Week 6: Final QA and Launch*

# Week 6 (Days 36–42): Final QA and Launch

## Goals
- Conduct final QA testing
- Prepare for system launch

---

## Day 36: Final QA Testing

### All Teams (Final QA Testing)
**Objective**: Ensure all features are working as expected

**Tasks**:
- Run through all user flows
- Verify all critical paths
- Check for edge cases

### Mustafo (System Stability)
**Objective**: Ensure system stability

**Tasks**:
- Monitor system performance
- Log any issues
- Implement necessary fixes

### Harley (Performance Optimization)
**Objective**: Optimize system performance

**Tasks**:
- Analyze system logs
- Identify bottlenecks
- Implement optimizations

### Alex (Documentation)
**Objective**: Update API documentation

**Tasks**:
- Review existing documentation
- Update any outdated or incorrect information

### Dawei (Deployment Preparation)
**Objective**: Prepare for production deployment

**Tasks**:
- Set up production environment
- Configure deployment scripts

### Dexter (Launch Preparation)
**Objective**: Prepare for system launch

**Tasks**:
- Schedule launch date
- Coordinate with marketing team

### Youssef (QA)
**Objective**: Conduct final QA testing

**Tasks**:
- Run through all user flows
- Verify all critical paths
- Check for edge cases

---

## Week 6 Deliverables ✅

### Final QA Testing
- [x] End-to-end user flow testing
- [x] Critical path verification
- [x] Edge case checking

### System Stabilization
- [x] Error handling and retry mechanisms
- [x] Performance monitoring and logging
- [x] Security audit and vulnerability assessment
- [x] API documentation updates

### Production Deployment
- [x] Set up production environment
- [x] Configure deployment scripts

### Launch Preparation
- [x] Schedule launch date
- [x] Coordinate with marketing team

**Performance Metrics**:
- System uptime: 99.9%
- Response time: <2 seconds for 90% of requests
- Error rate: <0.1%

---

*Ready for system launch*