import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';

interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt?: string;
  updatedAt?: string;
}

interface JWTPayload {
  sub: string; // user id
  email: string;
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
        id: decoded.sub,
        email: decoded.email,
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
      
      // Mock authentication for admin panel
      // In a real system, this would call a proper login API endpoint
      
      // Admin credentials check
      if (email === 'admin@youthmind.com' && password === 'admin') {
        const mockToken = 'mock-admin-token-' + Date.now();
        const mockUser = {
          id: '1',
          email: 'admin@youthmind.com',
          firstName: 'Admin',
          lastName: 'User'
        };
        localStorage.setItem('admin_token', mockToken);
        setUser(mockUser);
        setIsAuthenticated(true);
        return true;
      }
      
      // Test user credentials (for demo purposes)
      if (email === 'test@youthmind.com' && password === 'test123') {
        const mockToken = 'mock-test-token-' + Date.now();
        const mockUser = {
          id: '2',
          email: 'test@youthmind.com',
          firstName: 'Test',
          lastName: 'Admin'
        };
        localStorage.setItem('admin_token', mockToken);
        setUser(mockUser);
        setIsAuthenticated(true);
        return true;
      }
      
      // Invalid credentials
      console.log('Invalid credentials provided');
      return false;
      
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