import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';

interface User {
  id: string;
  username: string;
  email: string;
  role: string;
}

interface JWTPayload {
  sub: string; // user id
  email: string;
  username: string;
  role: string;
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
        username: decoded.username,
        email: decoded.email,
        role: decoded.role
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
      
      // DEVELOPER OPTION - Easy to remove: Allow admin/admin login
      if (email === 'admin@youthmind.com' && password === 'admin') {
        const mockToken = 'dev-token-' + Date.now();
        const mockUser = {
          id: '1',
          username: 'admin',
          email: 'admin@youthmind.com',
          role: 'admin'
        };
        localStorage.setItem('admin_token', mockToken);
        setUser(mockUser);
        setIsAuthenticated(true);
        return true;
      }
      // END DEVELOPER OPTION
      
      // Import api here to avoid circular dependency
      const { default: api } = await import('../services/api');
      
      const response = await api.post('/v1/admin/login', {
        email,
        password,
        recaptchaToken
      });

      const { token, user: userData } = response.data;

      if (token) {
        // Validate the received token
        if (validateToken(token)) {
          localStorage.setItem('admin_token', token);
          setUser(userData);
          setIsAuthenticated(true);
          return true;
        } else {
          console.error('Received invalid token from server');
          return false;
        }
      }
      return false;
    } catch (error) {
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