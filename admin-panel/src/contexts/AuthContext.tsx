import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface User {
  id: string;
  username: string;
  email: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<boolean>;
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

  const checkAuthStatus = () => {
    const token = localStorage.getItem('admin_token');
    if (token) {
      // You can add token validation here
      // For now, we'll assume token exists means user is authenticated
      setIsAuthenticated(true);
      // You could decode JWT or fetch user info here
      setUser({
        id: '1',
        username: 'admin',
        email: 'admin@youthmind.com',
        role: 'admin'
      });
    }
    setLoading(false);
  };

  const login = async (email: string, password: string): Promise<boolean> => {
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
        password
      });

      const { token, user: userData } = response.data;

      if (token) {
        localStorage.setItem('admin_token', token);
        setUser(userData);
        setIsAuthenticated(true);
        return true;
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