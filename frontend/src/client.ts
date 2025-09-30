// API Client Configuration for YouthMind Frontend Integration
import axios, { AxiosInstance, AxiosResponse, AxiosError } from 'axios';

// Environment configuration
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081';
const WS_BASE_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8081';

// Create axios instance with default configuration
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000, // 15 seconds
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Token management
class TokenManager {
  private static readonly ACCESS_TOKEN_KEY = 'youthmind_access_token';
  private static readonly REFRESH_TOKEN_KEY = 'youthmind_refresh_token';
  private static refreshPromise: Promise<string> | null = null;

  static getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  static getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  static setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }

  static clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    this.refreshPromise = null;
  }

  static async refreshAccessToken(): Promise<string> {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    this.refreshPromise = this.performTokenRefresh(refreshToken);
    
    try {
      const newAccessToken = await this.refreshPromise;
      this.refreshPromise = null;
      return newAccessToken;
    } catch (error) {
      this.refreshPromise = null;
      this.clearTokens();
      throw error;
    }
  }

  private static async performTokenRefresh(refreshToken: string): Promise<string> {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/auth/refresh`, {
        refreshToken,
      });

      const { accessToken, refreshToken: newRefreshToken } = response.data.data;
      this.setTokens(accessToken, newRefreshToken);
      return accessToken;
    } catch (error) {
      throw new Error('Token refresh failed');
    }
  }
}

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = TokenManager.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Add request timestamp for debugging
    config.metadata = { startTime: new Date() };
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and token refresh
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // Log response time for performance monitoring
    if (response.config.metadata?.startTime) {
      const duration = new Date().getTime() - response.config.metadata.startTime.getTime();
      console.debug(`API Request: ${response.config.method?.toUpperCase()} ${response.config.url} - ${duration}ms`);
    }

    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized - attempt token refresh
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const newAccessToken = await TokenManager.refreshAccessToken();
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed, redirect to login
        TokenManager.clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    // Handle other error types
    if (error.response) {
      // Server responded with error status
      const errorMessage = error.response.data?.message || 'An error occurred';
      const errorCode = error.response.data?.code || 'UNKNOWN_ERROR';
      
      console.error(`API Error: ${error.response.status} - ${errorMessage}`, {
        url: originalRequest?.url,
        method: originalRequest?.method,
        code: errorCode,
        data: error.response.data,
      });

      // Create enhanced error object
      const enhancedError = new Error(errorMessage);
      enhancedError.name = errorCode;
      enhancedError.status = error.response.status;
      enhancedError.data = error.response.data;
      
      return Promise.reject(enhancedError);
    } else if (error.request) {
      // Network error
      console.error('Network Error:', error.request);
      const networkError = new Error('Network error - please check your connection');
      networkError.name = 'NETWORK_ERROR';
      return Promise.reject(networkError);
    } else {
      // Request setup error
      console.error('Request Error:', error.message);
      return Promise.reject(error);
    }
  }
);

// API Response types
export interface ApiResponse<T = any> {
  success: boolean;
  data: T;
  message?: string;
  code?: string;
  timestamp: string;
}

export interface PaginatedResponse<T = any> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ErrorResponse {
  success: false;
  message: string;
  code: string;
  timestamp: string;
  details?: any;
}

// API wrapper functions
export const api = {
  // GET request
  get: <T = any>(url: string, params?: any): Promise<ApiResponse<T>> =>
    apiClient.get(url, { params }).then(response => response.data),

  // POST request
  post: <T = any>(url: string, data?: any): Promise<ApiResponse<T>> =>
    apiClient.post(url, data).then(response => response.data),

  // PUT request
  put: <T = any>(url: string, data?: any): Promise<ApiResponse<T>> =>
    apiClient.put(url, data).then(response => response.data),

  // DELETE request
  delete: <T = any>(url: string): Promise<ApiResponse<T>> =>
    apiClient.delete(url).then(response => response.data),

  // PATCH request
  patch: <T = any>(url: string, data?: any): Promise<ApiResponse<T>> =>
    apiClient.patch(url, data).then(response => response.data),

  // File upload
  upload: <T = any>(url: string, formData: FormData): Promise<ApiResponse<T>> =>
    apiClient.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(response => response.data),
};

// Export token manager for external use
export { TokenManager };

// Export constants
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/api/auth/login',
    REGISTER: '/api/auth/register',
    LOGOUT: '/api/auth/logout',
    REFRESH: '/api/auth/refresh',
    PROFILE: '/api/auth/profile',
  },
  USERS: {
    BASE: '/api/users',
    PROFILE: (id: string) => `/api/users/${id}`,
    PREFERENCES: (id: string) => `/api/users/${id}/preferences`,
    POSTS: (id: string) => `/api/users/${id}/posts`,
  },
  POSTS: {
    BASE: '/api/posts',
    DETAIL: (id: string) => `/api/posts/${id}`,
    FEED: '/api/posts/feed',
    TRENDING: '/api/posts/trending',
    SEARCH: '/api/posts/search',
    ENGAGE: (id: string) => `/api/posts/${id}/engage`,
    COMMENTS: (id: string) => `/api/posts/${id}/comments`,
  },
  COMMENTS: {
    BASE: '/api/comments',
    DETAIL: (id: string) => `/api/comments/${id}`,
    REPLIES: (id: string) => `/api/comments/${id}/replies`,
    ENGAGE: (id: string) => `/api/comments/${id}/engage`,
  },
  CHAT: {
    CONVERSATIONS: '/api/conversations',
    CONVERSATION: (id: string) => `/api/conversations/${id}`,
    MESSAGES: (id: string) => `/api/conversations/${id}/messages`,
  },
  DRAFTS: {
    BASE: '/api/drafts',
    DETAIL: (id: string) => `/api/drafts/${id}`,
    PUBLISH: (id: string) => `/api/drafts/${id}/publish`,
  },
} as const;

// Health check function
export const checkApiHealth = async (): Promise<boolean> => {
  try {
    const response = await apiClient.get('/actuator/health', { timeout: 5000 });
    return response.data.status === 'UP';
  } catch (error) {
    console.error('API health check failed:', error);
    return false;
  }
};

// WebSocket URL for real-time features
export const WS_URL = WS_BASE_URL;

export default api;
