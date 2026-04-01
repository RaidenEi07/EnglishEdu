/**
 * Auth module - Core auth logic (token & user management)
 */
import { apiPost } from './api.ts';
import type { AuthResponse, UserResponse } from './types.ts';

const TOKEN_KEY = 'sso_token';
const USER_KEY  = 'sso_user';

// Re-use API_BASE from api.ts so logout URL follows the same base as all other calls
const API_BASE: string = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export function getToken(): string | null {
  try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function setToken(token: string): void {
  try { localStorage.setItem(TOKEN_KEY, token); } catch { /* noop */ }
}

function clearToken(): void {
  try {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  } catch { /* noop */ }
}

function saveUser(user: UserResponse | null): void {
  if (!user) return;
  try {
    localStorage.setItem(USER_KEY, JSON.stringify({
      id: user.id, username: user.username,
      firstName: user.firstName, lastName: user.lastName,
      fullName: user.fullName || [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username,
      email: user.email, avatarUrl: user.avatarUrl, role: user.role,
      guest: user.guest,
    }));
  } catch { /* noop */ }
}

export function isLoggedIn(): boolean { return !!getToken(); }

export async function login(username: string, password: string): Promise<AuthResponse> {
  const data = await apiPost<AuthResponse>('/auth/login', { username, password });
  setToken(data.token);
  saveUser(data.user);
  return data;
}

export async function guestLogin(): Promise<AuthResponse> {
  const data = await apiPost<AuthResponse>('/auth/guest', {});
  setToken(data.token);
  saveUser(data.user);
  return data;
}

export function logout(): void {
  const token = getToken();
  if (token) {
    // Use API_BASE so the URL stays correct in both dev (/api/v1) and prod (same-domain)
    fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {});
  }
  clearToken();
  window.location.href = '/';
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  const data = await apiPost<AuthResponse>('/auth/register', payload);
  setToken(data.token);
  saveUser(data.user);
  return data;
}

export async function requestPasswordReset(searchValue: string): Promise<unknown> {
  return apiPost('/auth/forgot-password', { search: searchValue });
}

export async function resetPassword(token: string, newPassword: string): Promise<unknown> {
  return apiPost('/auth/reset-password', { token, newPassword });
}
