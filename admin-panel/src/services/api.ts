import axios from 'axios';

// Create axios instance with base configuration
const api = axios.create({
  // Always use backend on port 8080 unless overridden by environment variable
  baseURL: process.env.REACT_APP_API_URL || '/',
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
    api.post('/v1/auth/login', { email, password }),
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

// Posts API methods - adapting to available backend endpoints
export const postsApi = {
  // Note: Backend doesn't have GET posts endpoint, only POST create
  // Uncomment when backend implements these endpoints:
    
  // deletePost: (id: string) => api.delete(`/v1/admin/posts/${id}`),
  
  // flagPost: (id: string) => api.post(`/v1/admin/posts/${id}/flag`),
  
  // Create post (maps to existing backend endpoint)
  createPost: (data: {
    content: string;
    emotionTag?: string;
    aiMetadata?: string;
  }) => api.post('/v1/posts/create', data),

  // Get paginated feed of posts with optional server-side filters (backend /v1/posts/feed)
  getFeed: (
    page: number = 0,
    size: number = 20,
    sortBy: string = 'newest',
    filters: {
      emotionTag?: string;
      startDate?: string; // ISO string
      endDate?: string;   // ISO string
    } = {}
  ) => {
    const params: any = { page, size, sortBy };
    if (filters.emotionTag) params.emotionTag = filters.emotionTag;
    if (filters.startDate) params.startDate = filters.startDate;
    if (filters.endDate) params.endDate = filters.endDate;
    return api.get('/v1/posts/feed', { params });
  },

  // Get flagged posts for moderation queue
  getFlaggedPosts: () => api.get('/admin/v1/posts/flagged'),
};

// System health and monitoring
export const systemApi = {
  // Health check using test endpoint
  getHealth: () => api.get('/v1/auth/test'),
};

export default api; 