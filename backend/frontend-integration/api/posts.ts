// Posts API Service for YouthMind Frontend Integration
import { api, API_ENDPOINTS } from './client';
import type {
  Post,
  CreatePostRequest,
  UpdatePostRequest,
  EngagementRequest,
  PostEngagementResponse,
  FeedResponse,
  SearchResponse,
  TrendingResponse,
  PaginatedResponse,
  FeedParams,
  SearchParams,
  TrendingParams,
  EmotionType,
  EngagementType,
} from './types';

export class PostsAPI {
  // ==================== FEED OPERATIONS ====================
  
  /**
   * Get personalized feed with pagination and filtering
   */
  static async getFeed(params: FeedParams = {}): Promise<FeedResponse> {
    const {
      page = 0,
      size = 20,
      emotion,
      userId,
      following = false,
      sort = 'createdAt',
      direction = 'DESC',
    } = params;

    const queryParams = {
      page,
      size,
      sort: `${sort},${direction}`,
      ...(emotion && { emotion }),
      ...(userId && { userId }),
      ...(following && { following }),
    };

    const response = await api.get<PaginatedResponse<Post>>(
      API_ENDPOINTS.POSTS.FEED,
      queryParams
    );

    return {
      posts: response.data.content,
      totalElements: response.data.totalElements,
      totalPages: response.data.totalPages,
      currentPage: response.data.number,
      hasNext: !response.data.last,
      hasPrevious: !response.data.first,
    };
  }

  /**
   * Get trending posts based on engagement metrics
   */
  static async getTrendingPosts(params: TrendingParams = {}): Promise<TrendingResponse> {
    const {
      page = 0,
      size = 20,
      timeframe = 'week',
      emotion,
    } = params;

    const queryParams = {
      page,
      size,
      timeframe,
      ...(emotion && { emotion }),
    };

    const response = await api.get<PaginatedResponse<Post>>(
      API_ENDPOINTS.POSTS.TRENDING,
      queryParams
    );

    return {
      posts: response.data.content,
      timeframe,
      totalCount: response.data.totalElements,
    };
  }

  /**
   * Search posts with full-text search and filtering
   */
  static async searchPosts(params: SearchParams): Promise<SearchResponse> {
    const {
      query,
      page = 0,
      size = 20,
      emotion,
      userId,
      dateFrom,
      dateTo,
    } = params;

    const queryParams = {
      q: query,
      page,
      size,
      ...(emotion && { emotion }),
      ...(userId && { userId }),
      ...(dateFrom && { dateFrom }),
      ...(dateTo && { dateTo }),
    };

    const response = await api.get<{
      posts: Post[];
      totalResults: number;
      suggestions: string[];
    }>(API_ENDPOINTS.POSTS.SEARCH, queryParams);

    return {
      posts: response.data.posts,
      users: [], // Posts search doesn't return users
      totalResults: response.data.totalResults,
      query,
      suggestions: response.data.suggestions,
    };
  }

  // ==================== CRUD OPERATIONS ====================

  /**
   * Create a new post
   */
  static async createPost(postData: CreatePostRequest): Promise<Post> {
    const response = await api.post<{ post: Post }>(
      API_ENDPOINTS.POSTS.BASE,
      postData
    );
    return response.data.post;
  }

  /**
   * Get a specific post by ID
   */
  static async getPost(postId: string): Promise<Post> {
    const response = await api.get<Post>(
      API_ENDPOINTS.POSTS.DETAIL(postId)
    );
    return response.data;
  }

  /**
   * Update an existing post
   */
  static async updatePost(postId: string, postData: UpdatePostRequest): Promise<Post> {
    const response = await api.put<{ post: Post }>(
      API_ENDPOINTS.POSTS.DETAIL(postId),
      postData
    );
    return response.data.post;
  }

  /**
   * Delete a post (soft delete)
   */
  static async deletePost(postId: string): Promise<void> {
    await api.delete(API_ENDPOINTS.POSTS.DETAIL(postId));
  }

  // ==================== ENGAGEMENT OPERATIONS ====================

  /**
   * Add engagement to a post (like, resonance, collect, view)
   */
  static async addEngagement(
    postId: string,
    type: EngagementType
  ): Promise<PostEngagementResponse> {
    const response = await api.post<PostEngagementResponse>(
      API_ENDPOINTS.POSTS.ENGAGE(postId),
      { type } as EngagementRequest
    );
    return response.data;
  }

  /**
   * Remove engagement from a post
   */
  static async removeEngagement(
    postId: string,
    type: EngagementType
  ): Promise<PostEngagementResponse> {
    const response = await api.delete<PostEngagementResponse>(
      API_ENDPOINTS.POSTS.ENGAGE(postId),
      { data: { type } as EngagementRequest }
    );
    return response.data;
  }

  /**
   * Toggle engagement (add if not exists, remove if exists)
   */
  static async toggleEngagement(
    postId: string,
    type: EngagementType,
    currentlyEngaged: boolean
  ): Promise<PostEngagementResponse> {
    if (currentlyEngaged) {
      return this.removeEngagement(postId, type);
    } else {
      return this.addEngagement(postId, type);
    }
  }

  /**
   * Increment view count for a post
   */
  static async incrementViews(postId: string): Promise<void> {
    await this.addEngagement(postId, 'VIEW');
  }

  // ==================== USER-SPECIFIC OPERATIONS ====================

  /**
   * Get posts by a specific user
   */
  static async getUserPosts(
    userId: string,
    params: { page?: number; size?: number } = {}
  ): Promise<FeedResponse> {
    const { page = 0, size = 20 } = params;

    const response = await api.get<PaginatedResponse<Post>>(
      API_ENDPOINTS.USERS.POSTS(userId),
      { page, size }
    );

    return {
      posts: response.data.content,
      totalElements: response.data.totalElements,
      totalPages: response.data.totalPages,
      currentPage: response.data.number,
      hasNext: !response.data.last,
      hasPrevious: !response.data.first,
    };
  }

  /**
   * Get user's liked posts
   */
  static async getUserLikedPosts(
    userId: string,
    params: { page?: number; size?: number } = {}
  ): Promise<FeedResponse> {
    const { page = 0, size = 20 } = params;

    const response = await api.get<PaginatedResponse<Post>>(
      `/api/users/${userId}/liked-posts`,
      { page, size }
    );

    return {
      posts: response.data.content,
      totalElements: response.data.totalElements,
      totalPages: response.data.totalPages,
      currentPage: response.data.number,
      hasNext: !response.data.last,
      hasPrevious: !response.data.first,
    };
  }

  /**
   * Get user's bookmarked posts
   */
  static async getUserBookmarkedPosts(
    userId: string,
    params: { page?: number; size?: number } = {}
  ): Promise<FeedResponse> {
    const { page = 0, size = 20 } = params;

    const response = await api.get<PaginatedResponse<Post>>(
      `/api/users/${userId}/bookmarked-posts`,
      { page, size }
    );

    return {
      posts: response.data.content,
      totalElements: response.data.totalElements,
      totalPages: response.data.totalPages,
      currentPage: response.data.number,
      hasNext: !response.data.last,
      hasPrevious: !response.data.first,
    };
  }

  // ==================== EMOTION-BASED OPERATIONS ====================

  /**
   * Get posts filtered by emotion
   */
  static async getPostsByEmotion(
    emotion: EmotionType,
    params: { page?: number; size?: number } = {}
  ): Promise<FeedResponse> {
    return this.getFeed({ ...params, emotion });
  }

  /**
   * Get emotion distribution for analytics
   */
  static async getEmotionDistribution(
    timeframe: 'day' | 'week' | 'month' | 'year' = 'week'
  ): Promise<{ emotion: EmotionType; count: number; percentage: number }[]> {
    const response = await api.get<{
      distribution: { emotion: EmotionType; count: number; percentage: number }[];
    }>(`/api/posts/analytics/emotions`, { timeframe });

    return response.data.distribution;
  }

  // ==================== MODERATION OPERATIONS ====================

  /**
   * Flag a post for moderation
   */
  static async flagPost(postId: string, reason: string): Promise<void> {
    await api.post(`/api/posts/${postId}/flag`, { reason });
  }

  /**
   * Unflag a post (admin only)
   */
  static async unflagPost(postId: string): Promise<void> {
    await api.delete(`/api/posts/${postId}/flag`);
  }

  /**
   * Get flagged posts (admin only)
   */
  static async getFlaggedPosts(
    params: { page?: number; size?: number } = {}
  ): Promise<FeedResponse> {
    const { page = 0, size = 20 } = params;

    const response = await api.get<PaginatedResponse<Post>>(
      '/api/admin/posts/flagged',
      { page, size }
    );

    return {
      posts: response.data.content,
      totalElements: response.data.totalElements,
      totalPages: response.data.totalPages,
      currentPage: response.data.number,
      hasNext: !response.data.last,
      hasPrevious: !response.data.first,
    };
  }

  // ==================== ANALYTICS OPERATIONS ====================

  /**
   * Get post analytics for a specific post
   */
  static async getPostAnalytics(postId: string): Promise<{
    views: number;
    likes: number;
    resonance: number;
    collects: number;
    comments: number;
    shares: number;
    engagementRate: number;
    viewsOverTime: { date: string; views: number }[];
  }> {
    const response = await api.get<{
      analytics: {
        views: number;
        likes: number;
        resonance: number;
        collects: number;
        comments: number;
        shares: number;
        engagementRate: number;
        viewsOverTime: { date: string; views: number }[];
      };
    }>(`/api/posts/${postId}/analytics`);

    return response.data.analytics;
  }

  /**
   * Get user's post performance analytics
   */
  static async getUserPostAnalytics(
    userId: string,
    timeframe: 'week' | 'month' | 'year' = 'month'
  ): Promise<{
    totalPosts: number;
    totalViews: number;
    totalEngagements: number;
    averageEngagementRate: number;
    topPosts: Post[];
    performanceOverTime: { date: string; posts: number; engagements: number }[];
  }> {
    const response = await api.get<{
      analytics: {
        totalPosts: number;
        totalViews: number;
        totalEngagements: number;
        averageEngagementRate: number;
        topPosts: Post[];
        performanceOverTime: { date: string; posts: number; engagements: number }[];
      };
    }>(`/api/users/${userId}/post-analytics`, { timeframe });

    return response.data.analytics;
  }

  // ==================== UTILITY METHODS ====================

  /**
   * Check if user has engaged with a post
   */
  static async checkUserEngagement(
    postId: string,
    userId: string
  ): Promise<{
    isLiked: boolean;
    isResonated: boolean;
    isCollected: boolean;
    isViewed: boolean;
  }> {
    const response = await api.get<{
      engagements: {
        isLiked: boolean;
        isResonated: boolean;
        isCollected: boolean;
        isViewed: boolean;
      };
    }>(`/api/posts/${postId}/user-engagement`, { userId });

    return response.data.engagements;
  }

  /**
   * Get post engagement summary
   */
  static async getPostEngagementSummary(postId: string): Promise<{
    totalEngagements: number;
    resonanceUsers: string[];
    collectUsers: string[];
    recentEngagements: {
      userId: string;
      userName: string;
      type: EngagementType;
      timestamp: string;
    }[];
  }> {
    const response = await api.get<{
      summary: {
        totalEngagements: number;
        resonanceUsers: string[];
        collectUsers: string[];
        recentEngagements: {
          userId: string;
          userName: string;
          type: EngagementType;
          timestamp: string;
        }[];
      };
    }>(`/api/posts/${postId}/engagement-summary`);

    return response.data.summary;
  }

  /**
   * Get suggested posts based on user's interests and engagement history
   */
  static async getSuggestedPosts(
    userId: string,
    params: { page?: number; size?: number } = {}
  ): Promise<FeedResponse> {
    const { page = 0, size = 20 } = params;

    const response = await api.get<PaginatedResponse<Post>>(
      `/api/users/${userId}/suggested-posts`,
      { page, size }
    );

    return {
      posts: response.data.content,
      totalElements: response.data.totalElements,
      totalPages: response.data.totalPages,
      currentPage: response.data.number,
      hasNext: !response.data.last,
      hasPrevious: !response.data.first,
    };
  }
}

export default PostsAPI;
