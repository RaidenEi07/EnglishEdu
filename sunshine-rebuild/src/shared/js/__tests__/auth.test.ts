import { describe, it, expect, beforeEach, vi } from 'vitest';

// auth.ts uses localStorage and imports apiPost from api.ts.
// We mock api.ts before importing auth so the module graph is clean.
vi.mock('../api.ts', () => ({
  apiPost: vi.fn(),
}));

import { getToken, isLoggedIn } from '../auth.ts';

const TOKEN_KEY = 'sso_token';

describe('getToken', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('returns null when token is not set', () => {
    expect(getToken()).toBeNull();
  });

  it('returns the stored token string', () => {
    localStorage.setItem(TOKEN_KEY, 'my.jwt.token');
    expect(getToken()).toBe('my.jwt.token');
  });

  it('returns updated value after overwrite', () => {
    localStorage.setItem(TOKEN_KEY, 'first');
    localStorage.setItem(TOKEN_KEY, 'second');
    expect(getToken()).toBe('second');
  });
});

describe('isLoggedIn', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('returns false when no token exists', () => {
    expect(isLoggedIn()).toBe(false);
  });

  it('returns true when a token is present', () => {
    localStorage.setItem(TOKEN_KEY, 'any.valid.token');
    expect(isLoggedIn()).toBe(true);
  });

  it('returns false after token is removed', () => {
    localStorage.setItem(TOKEN_KEY, 'token');
    localStorage.removeItem(TOKEN_KEY);
    expect(isLoggedIn()).toBe(false);
  });
});
