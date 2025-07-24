import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import { authApi } from '../services/api';

interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt?: string;
  updatedAt?: string;
}

interface JWTPayload {
  sub: string; // user's email stored in subject claim
  exp: number; // expiration timestamp
  iat: number; // issued at timestamp
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string, recaptchaToken: string) => Promise<boolean>;
  logout: () => void;
  checkAuthStatus: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  // Check authentication status on app startup
  useEffect(() => {
    checkAuthStatus();
  }, []);

  const validateToken = (token: string): boolean => {
    try {
      const decoded = jwtDecode<JWTPayload>(token);
      const currentTime = Date.now() / 1000; // Convert to seconds
      
      // Check if token is expired
      if (decoded.exp < currentTime) {
        console.log('Token expired');
        return false;
      }
      
      return true;
    } catch (error) {
      console.error('Invalid token:', error);
      return false;
    }
  };

  const decodeToken = (token: string): User | null => {
    try {
      const decoded = jwtDecode<JWTPayload>(token);

      return {
        id: '', // ID not present in token; you can fetch by email if needed
        email: decoded.sub,
        firstName: 'Admin',
        lastName: 'User'
      };
    } catch (error) {
      console.error('Failed to decode token:', error);
      return null;
    }
  };

  const checkAuthStatus = async () => {
    const token = localStorage.getItem('admin_token');
    
    if (token) {
      // Validate the token
      if (validateToken(token)) {
        // Token is valid, decode user info
        const userData = decodeToken(token);
        if (userData) {
          setUser(userData);
          setIsAuthenticated(true);
        } else {
          // Token is invalid, clear it
          localStorage.removeItem('admin_token');
          setUser(null);
          setIsAuthenticated(false);
        }
      } else {
        // Token is expired or invalid, clear it
        localStorage.removeItem('admin_token');
        setUser(null);
        setIsAuthenticated(false);
      }
    }
    
    setLoading(false);
  };

  const login = async (email: string, password: string, recaptchaToken: string): Promise<boolean> => {
    try {
      setLoading(true);
      setIsAuthenticated(false);
      setUser(null);
      // Call backend login API
      const response = await authApi.adminLogin(email, password);
      const data = response.data;
      if (data && data.accessToken) {
        localStorage.setItem('admin_token', data.accessToken);
        // Use user info from backend response
        setUser({
          id: data.user.id,
          email: data.user.email,
          firstName: data.user.firstName,
          lastName: data.user.lastName,
          createdAt: data.user.createdAt,
          updatedAt: data.user.updatedAt,
        });
        setIsAuthenticated(true);
        return true;
      } else {
        return false;
      }
    } catch (error: any) {
      console.error('Login failed:', error);
      return false;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('admin_token');
    setUser(null);
    setIsAuthenticated(false);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated,
    loading,
    login,
    logout,
    checkAuthStatus
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}; 