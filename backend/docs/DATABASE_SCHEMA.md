# YouthMind Database Schema

## Overview
This document outlines the base database schema for YouthMind, supporting users, posts, and emotion tagging functionality.

## Database Technology
- **Database**: PostgreSQL
- **Migration Tool**: SQL Migration Scripts
- **ORM**: Spring Data JPA with Hibernate

## Schema Tables

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**Fields:**
- `id`: UUID primary key, auto-generated
- `email_hash`: Hashed email for privacy (VARCHAR 255, unique)
- `created_at`: Account creation timestamp

### Posts Table
```sql
CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    emotion_tag VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Fields:**
- `id`: UUID primary key, auto-generated
- `user_id`: Foreign key referencing users.id
- `content`: Post content (TEXT for unlimited length)
- `emotion_tag`: Emotion classification tag (VARCHAR 100)
- `created_at`: Post creation timestamp

## Relationships
- **One-to-Many**: Users → Posts (one user can have many posts)
- **Foreign Key**: posts.user_id → users.id with CASCADE delete

## Indexes
Performance indexes created for:
- `posts.user_id` (foreign key queries)
- `posts.created_at` (chronological queries)
- `posts.emotion_tag` (emotion filtering)
- `users.created_at` (user registration analytics)

## Files Created
1. **SQL Migration**: `backend/src/main/resources/db/migration/V1__create_base_schema.sql`
2. **DBML Schema**: `backend/docs/database_schema.dbml`
3. **Documentation**: `backend/docs/DATABASE_SCHEMA.md`

## For Mustafo - Sharing Instructions

### 1. ERD Visualization
- Open [dbdiagram.io](https://dbdiagram.io/)
- Copy the contents of `backend/docs/database_schema.dbml`
- Paste into dbdiagram.io to see the visual ERD

### 2. Implementation
- The SQL migration script is ready at: `backend/src/main/resources/db/migration/V1__create_base_schema.sql`
- Run the application to apply the schema automatically (ddl-auto: update is enabled)
- Or execute the SQL script manually against the PostgreSQL database

### 3. Data Types Used
- ✅ **UUID**: For all primary keys and foreign keys
- ✅ **VARCHAR**: For email_hash (255) and emotion_tag (100)
- ✅ **TEXT**: For post content (unlimited length)
- ✅ **TIMESTAMP**: For created_at fields with DEFAULT NOW()

### 4. Key Features
- ✅ Foreign key constraint with CASCADE delete
- ✅ Proper indexing for performance
- ✅ UUID primary keys for better distribution
- ✅ Default timestamps for audit trails
- ✅ Email hash for privacy compliance

## Next Steps
1. Review the schema with the team
2. Test the migration script
3. Consider adding additional indexes based on query patterns
4. Plan for future schema evolution (emotion categories, user preferences, etc.) 