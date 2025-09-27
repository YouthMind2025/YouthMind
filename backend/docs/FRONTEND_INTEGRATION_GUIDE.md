# Frontend Integration Guide

## 🎯 Complete Backend-Frontend Integration Strategy

This guide provides a complete roadmap for integrating the YouthMind backend with the new frontend.

## 📋 Integration Checklist

### Phase 1: Core API Integration
- [ ] **API Client Setup** - Axios/Fetch configuration with interceptors
- [ ] **Authentication Flow** - Login, register, token management
- [ ] **User Management** - Profile, preferences, settings
- [ ] **Type Definitions** - TypeScript interfaces for all entities

### Phase 2: Content Management
- [ ] **Post System** - Create, read, update, delete posts
- [ ] **Feed System** - Infinite scroll, emotion filtering, search
- [ ] **Engagement System** - Likes, resonance, collect, views
- [ ] **Comment System** - Threaded comments, real-time updates

### Phase 3: Advanced Features
- [ ] **AI Chat Integration** - Real-time conversations
- [ ] **Draft System** - Auto-save, publish workflow
- [ ] **Search & Discovery** - Full-text search, trending content
- [ ] **Analytics Dashboard** - User insights, engagement metrics

### Phase 4: Real-time & Performance
- [ ] **WebSocket Integration** - Live notifications, real-time updates
- [ ] **Caching Strategy** - Client-side caching, optimistic updates
- [ ] **Performance Optimization** - Lazy loading, code splitting
- [ ] **Error Handling** - Comprehensive error boundaries

## 🏗️ Frontend Architecture

```
frontend/
├── src/
│   ├── api/                    # API integration layer
│   │   ├── client.ts           # HTTP client configuration
│   │   ├── auth.ts             # Authentication APIs
│   │   ├── posts.ts            # Post management APIs
│   │   ├── users.ts            # User management APIs
│   │   ├── comments.ts         # Comment system APIs
│   │   ├── chat.ts             # AI chat APIs
│   │   └── types.ts            # API type definitions
│   │
│   ├── stores/                 # State management
│   │   ├── authStore.ts        # Authentication state
│   │   ├── postStore.ts        # Posts and feed state
│   │   ├── userStore.ts        # User profile state
│   │   ├── chatStore.ts        # AI chat state
│   │   └── uiStore.ts          # UI state management
│   │
│   ├── services/               # Business logic layer
│   │   ├── authService.ts      # Authentication logic
│   │   ├── postService.ts      # Post management logic
│   │   ├── cacheService.ts     # Client-side caching
│   │   ├── socketService.ts    # WebSocket management
│   │   └── notificationService.ts # Push notifications
│   │
│   ├── hooks/                  # Custom React hooks
│   │   ├── useAuth.ts          # Authentication hook
│   │   ├── usePosts.ts         # Posts management hook
│   │   ├── useRealtime.ts      # Real-time updates hook
│   │   └── useInfiniteScroll.ts # Infinite scroll hook
│   │
│   ├── components/             # Reusable components
│   │   ├── common/             # Common UI components
│   │   ├── forms/              # Form components
│   │   ├── feed/               # Feed-related components
│   │   ├── chat/               # AI chat components
│   │   └── modals/             # Modal components
│   │
│   └── utils/                  # Utility functions
│       ├── validation.ts       # Form validation
│       ├── formatters.ts       # Data formatting
│       ├── constants.ts        # App constants
│       └── helpers.ts          # Helper functions
```

## 🔌 API Integration Patterns

### 1. HTTP Client Configuration
```typescript
// api/client.ts
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Handle token refresh
      await refreshToken();
    }
    return Promise.reject(error);
  }
);
```

### 2. Type Definitions
```typescript
// api/types.ts
export interface User {
  id: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  bio?: string;
  avatar?: string;
  postsCount: number;
  likesCount: number;
  bookmarksCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Post {
  id: string;
  userId: string;
  title: string;
  content: string;
  emotion: EmotionType;
  topics?: string;
  preImg?: string;
  likes: number;
  bookmarks: number;
  views: number;
  resonanceCount: number;
  collectCount: number;
  isAnonymous: boolean;
  flagged: boolean;
  createdAt: string;
  updatedAt: string;
}

export type EmotionType = 
  | 'HAPPY' | 'SAD' | 'ANGRY' | 'ANXIOUS' | 'EXCITED' | 'CALM'
  | 'FRUSTRATED' | 'HOPEFUL' | 'LONELY' | 'GRATEFUL' | 'CONFUSED'
  | 'OVERWHELMED' | 'CONTENT' | 'WORRIED' | 'PROUD' | 'DISAPPOINTED';
```

### 3. API Service Layer
```typescript
// api/posts.ts
export const postAPI = {
  // Get feed with pagination
  getFeed: (page = 0, size = 20, emotion?: EmotionType) => 
    apiClient.get('/api/posts/feed', { 
      params: { page, size, emotion } 
    }),

  // Create new post
  createPost: (post: CreatePostRequest) => 
    apiClient.post('/api/posts', post),

  // Get post by ID
  getPost: (id: string) => 
    apiClient.get(`/api/posts/${id}`),

  // Update post
  updatePost: (id: string, post: UpdatePostRequest) => 
    apiClient.put(`/api/posts/${id}`, post),

  // Delete post
  deletePost: (id: string) => 
    apiClient.delete(`/api/posts/${id}`),

  // Engagement actions
  addEngagement: (postId: string, type: 'RESONANCE' | 'COLLECT' | 'VIEW') =>
    apiClient.post(`/api/posts/${postId}/engage`, { type }),

  removeEngagement: (postId: string, type: 'RESONANCE' | 'COLLECT') =>
    apiClient.delete(`/api/posts/${postId}/engage`, { data: { type } }),

  // Search posts
  searchPosts: (query: string, page = 0, size = 20) =>
    apiClient.get('/api/posts/search', { 
      params: { q: query, page, size } 
    }),

  // Get trending posts
  getTrendingPosts: (days = 7, page = 0, size = 20) =>
    apiClient.get('/api/posts/trending', { 
      params: { days, page, size } 
    }),
};
```

## 🔄 State Management Integration

### React Query/TanStack Query Pattern
```typescript
// hooks/usePosts.ts
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { postAPI } from '../api/posts';

export const useFeed = (emotion?: EmotionType) => {
  return useInfiniteQuery({
    queryKey: ['feed', emotion],
    queryFn: ({ pageParam = 0 }) => 
      postAPI.getFeed(pageParam, 20, emotion),
    getNextPageParam: (lastPage, pages) => 
      lastPage.data.hasNext ? pages.length : undefined,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

export const useCreatePost = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: postAPI.createPost,
    onSuccess: () => {
      // Invalidate and refetch feed
      queryClient.invalidateQueries({ queryKey: ['feed'] });
    },
    onError: (error) => {
      // Handle error
      console.error('Failed to create post:', error);
    },
  });
};
```

### Zustand Store Pattern
```typescript
// stores/authStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  refreshAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      login: async (credentials) => {
        try {
          const response = await authAPI.login(credentials);
          const { user, accessToken, refreshToken } = response.data;
          
          set({ 
            user, 
            accessToken, 
            refreshToken, 
            isAuthenticated: true 
          });
        } catch (error) {
          throw error;
        }
      },

      logout: () => {
        set({ 
          user: null, 
          accessToken: null, 
          refreshToken: null, 
          isAuthenticated: false 
        });
      },

      refreshAuth: async () => {
        try {
          const { refreshToken } = get();
          if (!refreshToken) throw new Error('No refresh token');
          
          const response = await authAPI.refreshToken(refreshToken);
          const { accessToken: newAccessToken } = response.data;
          
          set({ accessToken: newAccessToken });
        } catch (error) {
          get().logout();
          throw error;
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ 
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
```

## 🔄 Real-time Integration

### WebSocket Service
```typescript
// services/socketService.ts
import { io, Socket } from 'socket.io-client';

class SocketService {
  private socket: Socket | null = null;

  connect(token: string) {
    this.socket = io(process.env.REACT_APP_WS_URL!, {
      auth: { token },
      transports: ['websocket'],
    });

    this.socket.on('connect', () => {
      console.log('Connected to WebSocket');
    });

    this.socket.on('newPost', (post: Post) => {
      // Handle new post notification
      this.handleNewPost(post);
    });

    this.socket.on('postEngagement', (data: PostEngagement) => {
      // Handle engagement updates
      this.handlePostEngagement(data);
    });
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
    }
  }

  joinRoom(roomId: string) {
    this.socket?.emit('joinRoom', roomId);
  }

  leaveRoom(roomId: string) {
    this.socket?.emit('leaveRoom', roomId);
  }

  private handleNewPost(post: Post) {
    // Update feed in real-time
    // Trigger notifications
  }

  private handlePostEngagement(data: PostEngagement) {
    // Update post engagement counts
    // Show engagement animations
  }
}

export const socketService = new SocketService();
```

## 📱 Component Integration Examples

### Feed Component
```typescript
// components/feed/Feed.tsx
import React from 'react';
import { useFeed } from '../../hooks/usePosts';
import { PostCard } from './PostCard';
import { InfiniteScroll } from '../common/InfiniteScroll';

export const Feed: React.FC = () => {
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    error,
  } = useFeed();

  if (isLoading) return <FeedSkeleton />;
  if (error) return <ErrorMessage error={error} />;

  const posts = data?.pages.flatMap(page => page.data.content) ?? [];

  return (
    <InfiniteScroll
      hasMore={hasNextPage}
      loadMore={fetchNextPage}
      className="feed-container"
    >
      {posts.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}
    </InfiniteScroll>
  );
};
```

### AI Chat Component
```typescript
// components/chat/AIChat.tsx
import React, { useState, useEffect } from 'react';
import { useChat } from '../../hooks/useChat';
import { MessageList } from './MessageList';
import { MessageInput } from './MessageInput';

export const AIChat: React.FC = () => {
  const { 
    messages, 
    sendMessage, 
    isTyping, 
    conversationId 
  } = useChat();

  const handleSendMessage = async (text: string) => {
    await sendMessage(text);
  };

  return (
    <div className="ai-chat-container">
      <MessageList 
        messages={messages} 
        isTyping={isTyping} 
      />
      <MessageInput 
        onSendMessage={handleSendMessage}
        disabled={isTyping}
      />
    </div>
  );
};
```

## 🔧 Development Commands You'll Need

```bash
# Install additional dependencies
npm install @tanstack/react-query axios socket.io-client zustand
npm install -D @types/node

# Development server with proxy
npm run dev

# Build for production
npm run build

# Run tests
npm run test

# Type checking
npm run type-check
```

## 🚀 Integration Steps

1. **Create API layer** - All the API integration files
2. **Set up state management** - Zustand/React Query stores
3. **Implement authentication flow** - Login, register, token refresh
4. **Build core components** - Feed, posts, comments, chat
5. **Add real-time features** - WebSocket integration
6. **Implement caching** - Client-side optimization
7. **Add error handling** - Comprehensive error boundaries
8. **Performance optimization** - Code splitting, lazy loading

## 📋 Testing Strategy

- **Unit Tests**: Component testing with Jest/Vitest
- **Integration Tests**: API integration testing
- **E2E Tests**: Full user flow testing with Playwright
- **Performance Tests**: Bundle size and runtime performance

This integration will give you a production-ready frontend that seamlessly connects with your powerful backend! 🚀
