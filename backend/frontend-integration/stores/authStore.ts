// Authentication Store for YouthMind Frontend using Zustand
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { AuthAPI } from '../api/auth';
import { TokenManager } from '../api/client';
import type {
  User,
  UserPreferences,
  LoginRequest,
  RegisterRequest,
  UpdateUserProfileRequest,
  UpdateUserPreferencesRequest,
  AuthResponse,
} from '../api/types';

// ==================== AUTH STATE INTERFACE ====================
interface AuthState {
  // State
  user: User | null;
  preferences: UserPreferences | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  
  // Actions
  login: (credentials: LoginRequest) => Promise<void>;
  register: (userData: RegisterRequest) => Promise<void>;
  logout: () => void;
  refreshAuth: () => Promise<void>;
  updateProfile: (data: UpdateUserProfileRequest) => Promise<void>;
  updatePreferences: (data: UpdateUserPreferencesRequest) => Promise<void>;
  clearError: () => void;
  setLoading: (loading: boolean) => void;
  
  // Utility methods
  isTokenExpired: () => boolean;
  getUser: () => User | null;
  getUserId: () => string | null;
  hasRole: (role: string) => boolean;
}

// ==================== TOKEN UTILITIES ====================
const decodeToken = (token: string): any => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    return null;
  }
};

const isTokenExpired = (token: string): boolean => {
  try {
    const decoded = decodeToken(token);
    if (!decoded || !decoded.exp) return true;
    
    const currentTime = Date.now() / 1000;
    return decoded.exp < currentTime;
  } catch (error) {
    return true;
  }
};

// ==================== AUTH STORE ====================
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      // Initial state
      user: null,
      preferences: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      // ==================== AUTHENTICATION ACTIONS ====================
      
      login: async (credentials: LoginRequest) => {
        set({ isLoading: true, error: null });
        
        try {
          const response = await AuthAPI.login(credentials);
          const { user, accessToken, refreshToken } = response;

          // Store tokens in TokenManager
          TokenManager.setTokens(accessToken, refreshToken);

          // Update store state
          set({
            user,
            accessToken,
            refreshToken,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });

          // Load user preferences
          try {
            const preferencesResponse = await AuthAPI.getUserPreferences();
            set({ preferences: preferencesResponse });
          } catch (prefError) {
            console.warn('Failed to load user preferences:', prefError);
          }

        } catch (error: any) {
          const errorMessage = error.message || 'Login failed';
          set({
            user: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
            isLoading: false,
            error: errorMessage,
          });
          throw error;
        }
      },

      register: async (userData: RegisterRequest) => {
        set({ isLoading: true, error: null });
        
        try {
          const response = await AuthAPI.register(userData);
          const { user, accessToken, refreshToken } = response;

          // Store tokens
          TokenManager.setTokens(accessToken, refreshToken);

          // Update store state
          set({
            user,
            accessToken,
            refreshToken,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });

          // Create default preferences
          try {
            const defaultPreferences = {
              theme: 'SYSTEM' as const,
              language: 'en',
              notificationsEnabled: true,
              emailNotifications: true,
              pushNotifications: true,
              privacyLevel: 'PUBLIC' as const,
              aiSuggestionsEnabled: true,
              anonymousPostingAllowed: false,
            };
            
            const preferencesResponse = await AuthAPI.updateUserPreferences(defaultPreferences);
            set({ preferences: preferencesResponse });
          } catch (prefError) {
            console.warn('Failed to create default preferences:', prefError);
          }

        } catch (error: any) {
          const errorMessage = error.message || 'Registration failed';
          set({
            user: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
            isLoading: false,
            error: errorMessage,
          });
          throw error;
        }
      },

      logout: () => {
        // Call logout API (fire and forget)
        AuthAPI.logout().catch(console.error);

        // Clear tokens
        TokenManager.clearTokens();

        // Reset store state
        set({
          user: null,
          preferences: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
          isLoading: false,
          error: null,
        });

        // Clear any cached data
        if (typeof window !== 'undefined') {
          // Clear other localStorage items if needed
          localStorage.removeItem('feed-cache');
          localStorage.removeItem('user-cache');
        }
      },

      refreshAuth: async () => {
        const { refreshToken } = get();
        
        if (!refreshToken) {
          get().logout();
          throw new Error('No refresh token available');
        }

        if (isTokenExpired(refreshToken)) {
          get().logout();
          throw new Error('Refresh token expired');
        }

        try {
          const newAccessToken = await TokenManager.refreshAccessToken();
          
          set({
            accessToken: newAccessToken,
            error: null,
          });

          return newAccessToken;
        } catch (error) {
          get().logout();
          throw error;
        }
      },

      // ==================== PROFILE MANAGEMENT ====================

      updateProfile: async (data: UpdateUserProfileRequest) => {
        const { user } = get();
        if (!user) throw new Error('User not authenticated');

        set({ isLoading: true, error: null });

        try {
          const updatedUser = await AuthAPI.updateUserProfile(data);
          
          set({
            user: updatedUser,
            isLoading: false,
            error: null,
          });
        } catch (error: any) {
          const errorMessage = error.message || 'Failed to update profile';
          set({
            isLoading: false,
            error: errorMessage,
          });
          throw error;
        }
      },

      updatePreferences: async (data: UpdateUserPreferencesRequest) => {
        const { user } = get();
        if (!user) throw new Error('User not authenticated');

        set({ isLoading: true, error: null });

        try {
          const updatedPreferences = await AuthAPI.updateUserPreferences(data);
          
          set({
            preferences: updatedPreferences,
            isLoading: false,
            error: null,
          });
        } catch (error: any) {
          const errorMessage = error.message || 'Failed to update preferences';
          set({
            isLoading: false,
            error: errorMessage,
          });
          throw error;
        }
      },

      // ==================== UTILITY METHODS ====================

      clearError: () => set({ error: null }),

      setLoading: (loading: boolean) => set({ isLoading: loading }),

      isTokenExpired: () => {
        const { accessToken } = get();
        return !accessToken || isTokenExpired(accessToken);
      },

      getUser: () => get().user,

      getUserId: () => get().user?.id || null,

      hasRole: (role: string) => {
        const { accessToken } = get();
        if (!accessToken) return false;

        const decoded = decodeToken(accessToken);
        return decoded?.roles?.includes(role) || false;
      },
    }),
    {
      name: 'youthmind-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        preferences: state.preferences,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        // Validate tokens on rehydration
        if (state?.accessToken && isTokenExpired(state.accessToken)) {
          // Try to refresh if refresh token is valid
          if (state.refreshToken && !isTokenExpired(state.refreshToken)) {
            state.refreshAuth().catch(() => {
              state.logout();
            });
          } else {
            state.logout();
          }
        }
      },
    }
  )
);

// ==================== SELECTORS ====================

// Selector hooks for better performance
export const useUser = () => useAuthStore((state) => state.user);
export const useIsAuthenticated = () => useAuthStore((state) => state.isAuthenticated);
export const useAuthLoading = () => useAuthStore((state) => state.isLoading);
export const useAuthError = () => useAuthStore((state) => state.error);
export const useUserPreferences = () => useAuthStore((state) => state.preferences);

// ==================== AUTH UTILITIES ====================

/**
 * Hook for checking if user has specific permissions
 */
export const usePermissions = () => {
  const { hasRole, user } = useAuthStore();
  
  return {
    isAdmin: hasRole('ADMIN'),
    isModerator: hasRole('MODERATOR'),
    isUser: hasRole('USER'),
    canModerate: hasRole('ADMIN') || hasRole('MODERATOR'),
    canCreatePost: !!user,
    canDeletePost: (postUserId: string) => 
      hasRole('ADMIN') || hasRole('MODERATOR') || user?.id === postUserId,
    canEditPost: (postUserId: string) => user?.id === postUserId,
  };
};

/**
 * Hook for automatic token refresh
 */
export const useTokenRefresh = () => {
  const { refreshAuth, isTokenExpired, logout } = useAuthStore();

  const checkAndRefreshToken = async () => {
    if (isTokenExpired()) {
      try {
        await refreshAuth();
        return true;
      } catch (error) {
        logout();
        return false;
      }
    }
    return true;
  };

  return { checkAndRefreshToken };
};

/**
 * Hook for auth status with automatic refresh
 */
export const useAuthStatus = () => {
  const { user, isAuthenticated, isLoading, error } = useAuthStore();
  const { checkAndRefreshToken } = useTokenRefresh();

  React.useEffect(() => {
    if (isAuthenticated && !isLoading) {
      checkAndRefreshToken();
    }
  }, [isAuthenticated, isLoading, checkAndRefreshToken]);

  return {
    user,
    isAuthenticated,
    isLoading,
    error,
    isGuest: !isAuthenticated,
  };
};

export default useAuthStore;
