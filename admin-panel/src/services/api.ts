import axios from 'axios';

// Create axios instance with base configuration
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to inject Authorization header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
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

// Posts API methods (client-side pagination approach)
export const postsApi = {
  // Client-side pagination: fetch all posts
  getPosts: () => api.get('/v1/admin/posts'), // Returns all posts as array
  
  // Server-side pagination (commented for future use):
  // getPosts: (params: {
  //   page?: number;
  //   size?: number;
  //   emotionTag?: string;
  //   startDate?: string;
  //   endDate?: string;
  // }) => api.get('/v1/admin/posts', { params }),
  
  deletePost: (id: string) => api.delete(`/v1/admin/posts/${id}`),
  
  flagPost: (id: string) => api.post(`/v1/admin/posts/${id}/flag`),
};

export default api; 