# Contributing to YouthMind
## Developer Contribution Guidelines

*Thank you for your interest in contributing to YouthMind, a platform dedicated to improving youth mental health through technology.*

---

## Welcome Contributors! 🌟

YouthMind is an open-source project focused on creating safe, anonymous spaces for youth to express themselves and receive AI-powered emotional support. We welcome contributions from developers, mental health professionals, researchers, and anyone passionate about youth wellness.

### Our Mission
- **Accessibility**: Make mental health support available to all youth
- **Safety**: Maintain the highest standards of user privacy and content safety
- **Innovation**: Leverage cutting-edge AI technology for emotional intelligence
- **Community**: Foster understanding and empathy among young people

---

## How to Contribute

### **Areas Where We Need Help**

#### **Development Contributions**
- **iOS App Development**: SwiftUI interfaces, performance optimization, accessibility features
- **Backend Services**: API development, database optimization, security enhancements
- **AI/ML Engineering**: Emotion detection models, question generation, content safety
- **DevOps & Infrastructure**: Deployment automation, monitoring, scalability improvements

#### **Non-Development Contributions**
- **Mental Health Expertise**: Content safety guidelines, crisis intervention protocols
- **UX/UI Design**: User experience research, interface design, accessibility auditing
- **Documentation**: Technical writing, user guides, API documentation
- **Testing**: Quality assurance, security testing, accessibility testing
- **Translation**: Internationalization support for multiple languages

---

## Getting Started

### **1. Development Environment Setup**

#### Prerequisites
- **macOS** (for iOS development): macOS 12+ with Xcode 15+
- **Backend Development**: Java 17+, Maven 3.6+, Docker
- **AI Development**: Python 3.9+, CUDA-capable GPU (recommended)
- **General**: Git, Docker, PostgreSQL, Redis

#### Local Setup
```bash
# Clone the repository
git clone https://github.com/Kevin0304-li/YouthMind.git
cd YouthMind

# Set up backend
cd backend
mvn clean install
docker-compose up -d postgres redis

# Set up AI services
cd ../ai-services
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt

# Set up iOS project
cd ../ios-app
# Open YouthMind.xcodeproj in Xcode
```

#### Environment Configuration
```bash
# Create environment file
cp .env.example .env

# Configure required variables
DB_PASSWORD=your_secure_password
JWT_SECRET=your_256_bit_secret
AI_SERVICE_API_KEY=your_api_key
```

### **2. Understanding the Codebase**

#### **Project Structure**
```
YouthMind/
├── ios-app/                 # iOS application (SwiftUI)
├── backend/                 # Spring Boot API services
├── ai-services/            # Python FastAPI AI microservices
├── admin-panel/            # React administrative interface
├── docs/                   # Project documentation
├── scripts/                # Deployment and utility scripts
└── docker-compose.yml      # Local development environment
```

#### **Key Technologies**
- **iOS**: SwiftUI, Combine, CoreData, URLSession
- **Backend**: Spring Boot, PostgreSQL, Redis, JPA/Hibernate
- **AI**: FastAPI, HuggingFace Transformers, PyTorch
- **Infrastructure**: Docker, Nginx, AWS/Cloud services

---

## Development Workflow

### **3. Contribution Process**

#### **Before You Start**
1. **Check existing issues**: Browse [GitHub Issues](https://github.com/Kevin0304-li/YouthMind/issues) for open tasks
2. **Discuss new features**: Create an issue to discuss significant changes before implementation
3. **Read our guidelines**: Familiarize yourself with our coding standards and mental health considerations

#### **Making Changes**
1. **Fork the repository** and create a feature branch from `main`
2. **Follow naming conventions**: `feature/description`, `bugfix/issue-number`, `hotfix/description`
3. **Write tests** for new functionality (minimum 70% coverage)
4. **Update documentation** when adding new features or changing APIs
5. **Ensure security compliance** for any mental health data handling

#### **Code Quality Standards**

##### iOS Development
```swift
// Follow Swift naming conventions
class PostManager {
    private let networkService: NetworkServiceProtocol
    
    func createPost(content: String, emotionTags: [String]) async throws -> Post {
        // Validate input
        guard !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw PostError.emptyContent
        }
        
        // Implementation with proper error handling
    }
}

// Use meaningful variable names and comprehensive documentation
/// Creates a new anonymous post with emotional context
/// - Parameters:
///   - content: The post content (must not be empty)
///   - emotionTags: Array of emotion identifiers
/// - Returns: Created post object with assigned ID
/// - Throws: PostError for validation or network failures
```

##### Backend Development
```java
// Spring Boot service example
@Service
@Transactional
public class PostService {
    
    private final PostRepository postRepository;
    private final AIServiceClient aiServiceClient;
    
    public PostService(PostRepository postRepository, AIServiceClient aiServiceClient) {
        this.postRepository = postRepository;
        this.aiServiceClient = aiServiceClient;
    }
    
    @Async
    public CompletableFuture<Post> createPostAsync(CreatePostRequest request) {
        // Validate content safety
        if (containsSensitiveContent(request.getContent())) {
            throw new ContentSafetyException("Content requires manual review");
        }
        
        // Implementation with proper exception handling
    }
}
```

##### AI Development
```python
# FastAPI service example with safety considerations
from fastapi import HTTPException
from typing import List, Optional

@app.post("/analyze/sentiment")
async def analyze_sentiment(
    request: SentimentAnalysisRequest,
    background_tasks: BackgroundTasks
) -> SentimentResponse:
    """
    Analyze emotional sentiment with crisis detection.
    
    Args:
        request: Text content and analysis options
        background_tasks: For async crisis intervention if needed
        
    Returns:
        Sentiment analysis with safety indicators
        
    Raises:
        HTTPException: For content safety violations
    """
    
    # Content safety check
    if await content_filter.is_harmful(request.text):
        background_tasks.add_task(alert_moderation_team, request)
        raise HTTPException(status_code=400, detail="Content flagged for review")
    
    # AI analysis implementation
```

### **4. Testing Requirements**

#### **Unit Testing**
```swift
// iOS Unit Tests
class PostManagerTests: XCTestCase {
    func testCreatePostWithValidContent() async throws {
        let manager = PostManager(networkService: MockNetworkService())
        let post = try await manager.createPost(
            content: "Feeling anxious about exams",
            emotionTags: ["anxiety", "stress"]
        )
        
        XCTAssertFalse(post.id.isEmpty)
        XCTAssertEqual(post.emotionTags, ["anxiety", "stress"])
    }
    
    func testCreatePostWithEmptyContentThrowsError() async {
        let manager = PostManager(networkService: MockNetworkService())
        
        await XCTAssertThrowsError(
            try await manager.createPost(content: "", emotionTags: [])
        ) { error in
            XCTAssertEqual(error as? PostError, .emptyContent)
        }
    }
}
```

```java
// Backend Unit Tests
@SpringBootTest
class PostServiceTest {
    
    @MockBean
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private PostService postService;
    
    @Test
    void createPost_WithValidContent_ReturnsPost() {
        CreatePostRequest request = new CreatePostRequest("Valid content", List.of("happy"));
        
        Post result = postService.createPost(request);
        
        assertThat(result.getId()).isNotNull();
        assertThat(result.getContent()).isEqualTo("Valid content");
    }
    
    @Test
    void createPost_WithSensitiveContent_ThrowsException() {
        CreatePostRequest request = new CreatePostRequest("Harmful content", List.of());
        
        assertThatThrownBy(() -> postService.createPost(request))
            .isInstanceOf(ContentSafetyException.class);
    }
}
```

#### **Integration Testing**
```python
# AI Service Integration Tests
import pytest
from fastapi.testclient import TestClient

def test_sentiment_analysis_endpoint():
    client = TestClient(app)
    response = client.post("/analyze/sentiment", json={
        "text": "Feeling overwhelmed with school",
        "include_emotions": True
    })
    
    assert response.status_code == 200
    data = response.json()
    assert "sentiment" in data
    assert "emotions" in data
    assert data["crisis_indicators"]["level"] == "low"

def test_crisis_content_detection():
    client = TestClient(app)
    response = client.post("/analyze/sentiment", json={
        "text": "I can't take this anymore and want to hurt myself"
    })
    
    # Should trigger content safety measures
    assert response.status_code == 400
    assert "flagged for review" in response.json()["detail"]
```

---

## Mental Health Considerations

### **5. Safety & Ethics Guidelines**

#### **Content Safety Requirements**
- **Crisis Detection**: All AI models must include crisis intervention capabilities
- **Content Filtering**: Implement robust filtering for harmful content
- **Privacy Protection**: Never log or store sensitive user content unnecessarily
- **Age-Appropriate**: Ensure all content is suitable for young users (13-25)

#### **Ethical AI Development**
```python
# Example: Responsible AI implementation
class EmotionAnalyzer:
    def __init__(self):
        self.bias_detector = BiasDetectionModel()
        self.crisis_classifier = CrisisInterventionModel()
        
    async def analyze_safely(self, text: str) -> AnalysisResult:
        # Check for bias in AI response
        if self.bias_detector.detect_bias(text) > 0.7:
            await self.log_bias_incident(text)
            
        # Crisis intervention check
        crisis_level = self.crisis_classifier.assess(text)
        if crisis_level >= CrisisLevel.HIGH:
            await self.trigger_intervention_protocol(text)
            
        return self.generate_empathetic_response(text)
```

#### **Privacy by Design**
- Use anonymous UUIDs instead of personal identifiers
- Implement data retention policies (auto-delete after 2 years)
- Encrypt all data in transit and at rest
- Provide clear mechanisms for content deletion

---

## Code Review Process

### **6. Review Guidelines**

#### **Reviewer Checklist**
- [ ] **Functionality**: Does the code work as intended?
- [ ] **Safety**: Are mental health safety measures implemented?
- [ ] **Security**: Are there any security vulnerabilities?
- [ ] **Performance**: Is the code efficient and scalable?
- [ ] **Testing**: Are there adequate unit and integration tests?
- [ ] **Documentation**: Is the code well-documented?
- [ ] **Accessibility**: Does the UI meet accessibility standards?

#### **Mental Health Review Criteria**
- [ ] **Crisis Detection**: AI outputs include appropriate crisis intervention
- [ ] **Content Appropriateness**: All generated content is age-appropriate
- [ ] **Bias Prevention**: AI models are checked for harmful biases
- [ ] **Privacy Compliance**: User data handling follows privacy guidelines

### **7. Pull Request Template**

```markdown
## Description
Brief description of changes and motivation.

## Type of Change
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Security improvement

## Mental Health Safety Checklist
- [ ] Crisis detection mechanisms reviewed/implemented
- [ ] Content safety filters applied
- [ ] Age-appropriate content verified
- [ ] Privacy compliance ensured

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Accessibility testing completed

## Screenshots (if applicable)
Add screenshots to help explain your changes.

## Additional Notes
Any additional information reviewers should know.
```

---

## Community Guidelines

### **8. Code of Conduct**

#### **Our Commitment**
We are committed to providing a welcoming, inclusive environment for all contributors, especially given our focus on mental health support for vulnerable populations.

#### **Expected Behavior**
- **Respectful Communication**: Use welcoming and inclusive language
- **Mental Health Awareness**: Be sensitive to mental health topics and language
- **Constructive Feedback**: Provide helpful, actionable feedback in reviews
- **Collaborative Spirit**: Work together towards our shared mission
- **Privacy Respect**: Never share or discuss real user content

#### **Unacceptable Behavior**
- Harassment, discrimination, or offensive language
- Sharing sensitive user data or content
- Inappropriate mental health advice or discussion
- Deliberate introduction of security vulnerabilities
- Spamming or promotional activities

### **9. Getting Help**

#### **Communication Channels**
- **GitHub Issues**: For bug reports and feature requests
- **Discussions**: For general questions and community chat
- **Discord**: Real-time developer communication (invite link in README)
- **Email**: security@youthmind.dev for security-related concerns

#### **Documentation Resources**
- [Development Workflow](./docs/DEVELOPMENT_WORKFLOW.md)
- [API Reference](./docs/API_REFERENCE.md)
- [Team Structure](./docs/TEAM_STRUCTURE.md)
- [Deployment Guide](./docs/DEPLOYMENT.md)

---

## Recognition

### **10. Contributor Recognition**

We value all contributions to YouthMind! Contributors will be:

- **Listed in CONTRIBUTORS.md** with their contributions
- **Mentioned in release notes** for significant contributions
- **Invited to contributor events** and community calls
- **Offered priority consideration** for internship/job opportunities (when available)

### **Types of Contributions We Celebrate**
- Code contributions (features, bug fixes, improvements)
- Documentation improvements
- Mental health expertise and guidance
- Security auditing and improvements
- Testing and quality assurance
- Community support and mentorship
- Accessibility improvements
- Internationalization support

---

## Legal Considerations

### **11. Contribution Agreement**

By contributing to YouthMind, you agree that:

- Your contributions are your original work or you have rights to submit them
- You grant YouthMind a perpetual, worldwide, non-exclusive license to use your contributions
- You understand this is a mental health platform with special safety requirements
- You will follow all applicable laws and regulations regarding mental health data

### **12. Security Reporting**

**Do not report security vulnerabilities through public GitHub issues.**

Instead, please email security@youthmind.dev with:
- Description of the vulnerability
- Steps to reproduce
- Potential impact assessment
- Suggested fixes (if any)

We will respond within 48 hours and work with you to address any security concerns.

---

*Thank you for contributing to YouthMind and helping us create a safer, more supportive digital space for young people. Together, we can make a meaningful difference in youth mental health.*

## Quick Links
- [Development Setup Guide](./docs/DEVELOPMENT_WORKFLOW.md#local-development-setup)
- [API Documentation](./docs/API_REFERENCE.md)
- [Issue Templates](https://github.com/Kevin0304-li/YouthMind/issues/new/choose)
- [Project Roadmap](https://github.com/Kevin0304-li/YouthMind/projects)

---

**Questions?** Feel free to reach out to our community or maintainers. We're here to help! 🚀 