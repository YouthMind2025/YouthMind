# YouthMind Database Schema

## Overview
This document outlines the comprehensive database schema for YouthMind, supporting users, posts, comments, conversations, drafts, and emotion tagging functionality.

## Database Technology
- **Database**: PostgreSQL (Production/Staging), H2 (Development/Testing)
- **Migration Tool**: Flyway with SQL Migration Scripts
- **ORM**: Spring Data JPA with Hibernate
- **Connection Pool**: HikariCP with performance tuning

## Schema Tables

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(255) UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    bio TEXT,
    avatar VARCHAR(500),
    posts_count INTEGER NOT NULL DEFAULT 0,
    likes_count INTEGER NOT NULL DEFAULT 0,
    bookmarks_count INTEGER NOT NULL DEFAULT 0,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**Fields:**
- `id`: UUID primary key, auto-generated
- `email`: User email (VARCHAR 255, unique)
- `password`: Hashed password (VARCHAR 255)
- `username`: Unique username (VARCHAR 255)
- `first_name`, `last_name`: User names
- `bio`: User biography (TEXT)
- `avatar`: Profile image URL (VARCHAR 500)
- `posts_count`, `likes_count`, `bookmarks_count`: User statistics
- `role`: User role (USER/ADMIN)
- `created_at`, `updated_at`: Timestamps

### Posts Table
```sql
CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled Post',
    content TEXT NOT NULL,
    emotion VARCHAR(100),
    pre_img VARCHAR(500),
    resonance INTEGER NOT NULL DEFAULT 0,
    view_count INTEGER NOT NULL DEFAULT 0,
    collect_count INTEGER NOT NULL DEFAULT 0,
    topics TEXT,
    ai_metadata TEXT,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason VARCHAR(500),
    flagged_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Fields:**
- `id`: UUID primary key, auto-generated
- `user_id`: Foreign key referencing users.id
- `title`: Post title (VARCHAR 255)
- `content`: Post content (TEXT)
- `emotion`: Emotion classification tag (VARCHAR 100)
- `pre_img`: Preview image URL (VARCHAR 500)
- `resonance`, `view_count`, `collect_count`: Engagement metrics
- `topics`: Post topics (TEXT)
- `ai_metadata`: AI analysis metadata (TEXT)
- `flagged`, `flag_reason`, `flagged_at`: Moderation fields
- `created_at`, `updated_at`: Timestamps

### Comments Table
```sql
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    likes INTEGER NOT NULL DEFAULT 0,
    parent_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);
```

### Drafts Table
```sql
CREATE TABLE drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    emotion VARCHAR(100),
    topics TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Conversations Table
```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Messages Table
```sql
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    text TEXT NOT NULL,
    sender VARCHAR(10) NOT NULL CHECK (sender IN ('user', 'ai')),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);
```

### User Engagements Table
```sql
CREATE TABLE user_engagements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('RESONANCE', 'COLLECT', 'VIEW')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    UNIQUE(user_id, post_id, type)
);
```

### Comment Engagements Table
```sql
CREATE TABLE comment_engagements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    comment_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('LIKE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    UNIQUE(user_id, comment_id, type)
);
```

### Articles Table
```sql
CREATE TABLE articles (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    intro VARCHAR(500),
    cover_image VARCHAR(500),
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### AI Analysis Table
```sql
CREATE TABLE ai_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    analysis_data TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
```

## Relationships
- **One-to-Many**: Users → Posts, Comments, Drafts, Conversations
- **One-to-Many**: Posts → Comments
- **One-to-Many**: Conversations → Messages
- **Many-to-Many**: Users ↔ Posts (via user_engagements)
- **Many-to-Many**: Users ↔ Comments (via comment_engagements)
- **Self-Referential**: Comments → Comments (parent_id)

## Indexes
Performance indexes created for:
- `posts.user_id`, `posts.created_at`, `posts.emotion`, `posts.resonance`, `posts.view_count`
- `users.username`, `users.email`, `users.role`
- `comments.post_id`, `comments.author_id`, `comments.created_at`
- `drafts.user_id`, `drafts.created_at`
- `conversations.user_id`, `conversations.is_pinned`
- `messages.conversation_id`, `messages.timestamp`
- `user_engagements.user_id`, `user_engagements.post_id`, `user_engagements.type`
- `articles.published`, `articles.created_at`, `articles.title`
- Full-text search indexes for content search

## Migration Files
1. **V1__create_base_schema.sql** - Base users and posts tables
2. **V2__add_performance_indexes.sql** - Performance optimization indexes
3. **V3__create_ai_analysis_table.sql** - AI analysis tracking
4. **V4__create_comprehensive_schema.sql** - Comments, drafts, conversations, messages, engagements
5. **V5__create_post_resonances_table.sql** - Post resonance tracking
6. **V6__create_post_collections_table.sql** - Post collection tracking
7. **V7__add_search_indexes.sql** - Full-text search capabilities
8. **V8__add_user_roles.sql** - Role-based access control
9. **V9__create_articles_table.sql** - Content management

## Configuration Profiles
- **Development**: H2 in-memory database with auto-schema creation
- **Staging**: PostgreSQL with connection pooling and performance tuning
- **Production**: PostgreSQL with validation and monitoring
- **Test**: H2 with test-specific configurations

## Performance Optimizations
- **Connection Pooling**: HikariCP with tuned parameters
- **Database Indexes**: Optimized for common query patterns
- **PostgreSQL Settings**: work_mem, shared_buffers tuning
- **Query Optimization**: Repository methods with pagination

## Monitoring & Health Checks
- **Database Health Indicator**: Comprehensive connectivity and schema validation
- **Redis Health Monitoring**: Cache layer health checks
- **Prometheus Metrics**: Database performance metrics
- **Actuator Endpoints**: Health, info, metrics, prometheus

## Key Features
- UUID primary keys for better distribution
- Foreign key constraints with CASCADE delete
- Proper indexing for performance
- Role-based access control (USER/ADMIN)
- Moderation system with flagging
- Engagement tracking (resonance, views, collections)
- AI analysis integration
- Content management (articles)
- Audit trails with timestamps
- Full-text search capabilities 