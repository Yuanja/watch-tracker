import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
} from 'react';
import type { User } from '../types/user';
import { getMe, loginWithGoogle, logout as apiLogout } from '../api/auth';
import { clearAuthToken, getAuthToken, setAuthToken } from '../api/client';

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: () => void;
  logout: () => Promise<void>;
  setToken: (token: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface AuthProviderProps {
  children: React.ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const loadUser = useCallback(async () => {
    const token = getAuthToken();
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      const currentUser = await getMe();
      setUser(currentUser);
    } catch {
      // Token is invalid or expired
      clearAuthToken();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const login = useCallback(() => {
    loginWithGoogle();
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } finally {
      clearAuthToken();
      setUser(null);
    }
  }, []);

  /**
   * Called after OAuth2 callback returns a token (e.g. via URL param).
   * Stores the token and loads the user profile.
   */
  const setToken = useCallback(async (token: string) => {
    setAuthToken(token);
    setIsLoading(true);
    try {
      const currentUser = await getMe();
      setUser(currentUser);
    } catch {
      clearAuthToken();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const value: AuthContextValue = {
    user,
    isLoading,
    isAuthenticated: user !== null,
    login,
    logout,
    setToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return ctx;
}
