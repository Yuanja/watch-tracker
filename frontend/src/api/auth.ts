import { apiClient } from './client';
import type { User } from '../types/user';

/**
 * Fetch the currently authenticated user's profile.
 */
export async function getMe(): Promise<User> {
  const response = await apiClient.get<User>('/auth/me');
  return response.data;
}

/**
 * Initiate Google OAuth2 login by redirecting the browser to the backend
 * OAuth2 authorization endpoint. The backend handles the OAuth flow and
 * redirects back with a JWT token.
 */
export function loginWithGoogle(): void {
  window.location.href = '/api/oauth2/authorization/google';
}

/**
 * Logout — no backend call needed for stateless JWT; just clear local storage.
 */
export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout').catch(() => {
    // Best-effort; ignore errors
  });
}
