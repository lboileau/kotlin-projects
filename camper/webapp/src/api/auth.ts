import { request } from './http';

export interface AuthUser {
  id: string;
  email: string;
  username: string | null;
}

/** POST /api/auth — sign in by email. 404/REGISTRATION_REQUIRED means the caller should register instead. */
export function signIn(email: string): Promise<AuthUser> {
  return request('/api/auth', { method: 'POST', body: { email } });
}

/** POST /api/users — register (idempotent on the backend). */
export function register(email: string, username: string): Promise<AuthUser> {
  return request('/api/users', { method: 'POST', body: { email, username } });
}

/** GET /api/users/{id} */
export function getUser(userId: string): Promise<AuthUser> {
  return request(`/api/users/${userId}`);
}

/** PUT /api/users/{id} — the backend's UpdateUserRequest requires `username`. */
export function updateUsername(userId: string, username: string): Promise<AuthUser> {
  return request(`/api/users/${userId}`, { method: 'PUT', body: { username } });
}
