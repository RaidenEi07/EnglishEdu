/**
 * Shared API helper — centralises fetch calls, auth header & error handling
 */
import { toast } from './toast.ts';

const API_BASE: string = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const TOKEN_KEY = 'sso_token';

function getToken(): string | null {
  try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function authHeaders(extra: Record<string, string> = {}): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...extra };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

/** Global error handler — shows toast and re-throws */
function handleHttpError(res: Response, body: { message?: string } | null): never {
  if (res.status === 401) {
    localStorage.removeItem(TOKEN_KEY);
    window.location.href = `/pages/login/?redirect=${encodeURIComponent(window.location.pathname)}`;
    throw new Error('Unauthorized');
  }
  if (res.status === 403) {
    toast.error('Bạn không có quyền thực hiện thao tác này.');
    throw new Error(body?.message || 'Forbidden');
  }
  if (res.status >= 500) {
    toast.error('Lỗi hệ thống, vui lòng thử lại sau.');
  }
  throw new Error(body?.message || `Request failed (${res.status})`);
}

/**
 * Generic request wrapper.
 * Returns parsed JSON body on success, throws on error.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function api<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE}${path}`;
  let res: Response;
  try {
    res = await fetch(url, {
      ...options,
      headers: authHeaders(options.headers as Record<string, string>),
    });
  } catch {
    toast.error('Không thể kết nối máy chủ. Kiểm tra kết nối mạng.');
    throw new Error('Network error');
  }

  const body = await res.json().catch(() => null);
  if (!res.ok) handleHttpError(res, body);

  const result = body?.data !== undefined ? body.data : body;

  // Spring Data 3.3+ serializes Page<T> with pagination metadata nested under a "page" key.
  // Normalise to the flat format our types expect so all consumers work unchanged.
  if (result && typeof result === 'object' && Array.isArray(result.content) &&
      result.page && typeof result.page === 'object') {
    result.number       = result.page.number       ?? result.number;
    result.totalPages   = result.page.totalPages   ?? result.totalPages;
    result.totalElements = result.page.totalElements ?? result.totalElements;
    result.size         = result.page.size         ?? result.size;
  }

  return result;
}

export function apiGet<T = any>(path: string): Promise<T>             { return api<T>(path); }
export function apiPost<T = any>(path: string, data?: unknown): Promise<T>  { return api<T>(path, { method: 'POST',   body: JSON.stringify(data) }); }
export function apiPut<T = any>(path: string, data?: unknown): Promise<T>   { return api<T>(path, { method: 'PUT',    body: JSON.stringify(data) }); }
export function apiPatch<T = any>(path: string, data?: unknown): Promise<T> { return api<T>(path, { method: 'PATCH',  body: JSON.stringify(data) }); }
export function apiDelete<T = any>(path: string): Promise<T>          { return api<T>(path, { method: 'DELETE' }); }

/**
 * Upload a file (multipart/form-data). Do NOT set Content-Type — browser does it.
 */
export async function apiUpload<T = any>(path: string, formData: FormData): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers,
      body: formData,
    });
  } catch {
    toast.error('Không thể kết nối máy chủ. Kiểm tra kết nối mạng.');
    throw new Error('Network error');
  }

  const body = await res.json().catch(() => null);
  if (!res.ok) handleHttpError(res, body);
  return body?.data !== undefined ? body.data : body;
}
