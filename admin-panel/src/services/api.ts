import axios from 'axios';

// Create axios instance with base configuration
const api = axios.create({
  // Use proxy configuration from package.json when in development
  // In production, set REACT_APP_API_URL environment variable
  baseURL: process.env.REACT_APP_API_URL || '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to inject Authorization header
api.interceptors.request.use(
  (config) => {
    // Do not attach token for auth endpoints
    if (config.url && !config.url.startsWith('/v1/auth')) {
      const token = localStorage.getItem('admin_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    
    // Handle FormData - remove default Content-Type to let browser set multipart boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token expiry
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - redirect to login
      localStorage.removeItem('admin_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API methods for admin login using backend auth system
export const authApi = {
  adminLogin: (email: string, password: string) => 
    api.post('/v1/auth/admin/login', { email, password }),
  // Test auth endpoint for health checks
  testAuth: () => api.get('/v1/auth/test'),
};

// Users API methods - maps to backend user management endpoints  
export const usersApi = {
  // Get all users (admin functionality using existing endpoint)
  getAllUsers: () => api.get('/api/users'),
  
  // Get user by ID
  getUserById: (id: string) => api.get(`/api/users/${id}`),
  
  // Get user by email  
  getUserByEmail: (email: string) => api.get(`/api/users/email/${email}`),
  
  // Delete user (admin functionality)
  deleteUser: (id: string) => api.delete(`/api/users/${id}`),
};

// Posts API methods - using actual backend endpoints
export const postsApi = {
  // Get posts with pagination and filters (backend endpoint: GET /v1/posts)
  getPosts: (
    page: number = 1,
    limit: number = 20,
    emotion?: string,
    sort: string = 'newest'
  ) => {
    // Backend uses 0-based pagination and 'size' parameter
    const params: any = { 
      page: page - 1, // Convert to 0-based
      size: limit     // Use 'size' instead of 'limit'
    };
    if (emotion) params.emotion = emotion;
    if (sort) params.sort = sort;
    return api.get('/v1/posts', { params });
  },

  // Get single post by ID
  getPost: (postId: string) => api.get(`/v1/posts/${postId}`),
  
  // Create post (maps to existing backend endpoint)
  createPost: (data: {
    title: string;
    content: string;
    emotion?: string;
    preImg?: string;
  }) => api.post('/v1/posts/create', data),

  // Delete post (admin functionality) - uses admin endpoint that bypasses ownership checks
  deletePost: (postId: string) => api.delete(`/admin/v1/posts/delete/${postId}`),

  // Get flagged posts for moderation queue
  getFlaggedPosts: () => api.get('/admin/v1/posts/flagged'),

  // Flag a post for moderation
  flagPost: (postId: string, reason?: string) => 
    api.post(`/admin/v1/posts/flag/${postId}`, reason ? { reason } : {}),

  // Unflag a post
  unflagPost: (postId: string) => 
    api.post(`/admin/v1/posts/unflag/${postId}`),
};

// Articles API methods - using actual backend endpoints
export const articlesApi = {
  // Get all published articles (for website)
  getPublishedArticles: () => api.get('/v1/articles'),
  
  // Get articles with pagination (admin panel)
  getArticles: (page: number = 0, limit: number = 20, publishedOnly: boolean = false) => {
    const params = { page, limit, publishedOnly };
    return api.get('/v1/articles/admin', { params });
  },
  
  // Get single article by ID
  getArticle: (id: string) => api.get(`/v1/articles/view/${id}`),
  
  // Create new article (admin functionality)
  createArticle: (data: {
    title: string;
    content: string;
    intro?: string;
    coverImage?: string;
    published?: boolean;
  }) => api.post('/v1/articles/create', data),
  
  // Update article (admin functionality)
  updateArticle: (id: string, data: {
    title: string;
    content: string;
    intro?: string;
    coverImage?: string;
    published?: boolean;
  }) => api.put(`/v1/articles/edit/${id}`, data),
  
  // Delete article (admin functionality)
  deleteArticle: (id: string) => api.delete(`/v1/articles/delete/${id}`),
  
  // Toggle publish status
  togglePublishStatus: (id: string) => api.post(`/v1/articles/manage/${id}/toggle-publish`),
  
  // Search articles
  searchArticles: (query: string, page: number = 0, limit: number = 20, publishedOnly: boolean = true) => {
    const params = { query, page, limit, publishedOnly };
    return api.get('/v1/articles/search', { params });
  },

  // Upload .docx file for parsing
  uploadDocx: (formData: FormData) => {
    return api.post('/v1/articles/upload-docx', formData);
  },
};

// Psychological Analysis API methods (Youseef's Integration)
export const psychologicalApi = {
  // Get psychological state for a user
  getPsychologicalState: (userId: string) => 
    api.get(`/api/ai/psychology/state/${userId}`),
  
  // Generate psychological state-aware response
  generatePsychologicalResponse: (userId: string, message: string) =>
    api.post(`/api/ai/psychology/chat/${userId}`, { message }),
  
  // Analyze message for psychological insights
  analyzeMessage: (userId: string, message: string) =>
    api.post(`/api/ai/psychology/analyze/${userId}`, { message }),
  
  // Get psychological analysis health status
  getHealth: () => api.get('/api/ai/psychology/health'),
  
  // Get batch processing statistics
  getBatchStatistics: () => api.get('/api/ai/psychology/batch/statistics'),
  
  // Get cache statistics
  getCacheStatistics: () => api.get('/api/ai/psychology/cache/statistics'),
  
  // Invalidate user's psychological state cache
  invalidateUserCache: (userId: string) =>
    api.delete(`/api/ai/psychology/cache/${userId}`),
  
  // Warm up cache for a user
  warmUpCache: (userId: string) =>
    api.post(`/api/ai/psychology/cache/warmup/${userId}`),
};

// System health and monitoring
export const systemApi = {
  /**
   * Retrieve system health information from Spring Boot Actuator endpoint.
   * This endpoint is publicly accessible as per backend SecurityConfig.
   */
  getHealth: () => api.get('/actuator/health'),
  // Direct backend connection test (bypasses proxy)
  testBackendDirect: () => axios.get('http://localhost:8080/actuator/health'),
  
  // Individual component health checks
  getDatabaseHealth: () => api.get('/actuator/health/database'),
  getRedisHealth: () => api.get('/actuator/health/redis'),
  
  // Health groups for Kubernetes probes
  getLivenessHealth: () => api.get('/actuator/health/liveness'),
  getReadinessHealth: () => api.get('/actuator/health/readiness'),
  
  // Test backend connectivity with timeout
  testConnectivity: () => axios.get('http://localhost:8080/actuator/health', { timeout: 5000 }),
};

export default api; 