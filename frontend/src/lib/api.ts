import { User, Room, CodeFile } from '../types';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || '/api';

async function fetchJson<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    credentials: 'include',
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.message || `Request failed with status ${res.status}`);
  }
  return data as T;
}

export const api = {
  // Auth
  register: (data: { name: string; email: string; password: string }) =>
    fetchJson<{ user: User; token: string }>(`${API_BASE}/auth/register`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  login: (data: { email: string; password: string }) =>
    fetchJson<{ user: User; token: string }>(`${API_BASE}/auth/login`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  logout: () =>
    fetchJson<{ message: string }>(`${API_BASE}/auth/logout`, {
      method: 'POST',
    }),

  me: () => fetchJson<User>(`${API_BASE}/auth/me`),

  // Rooms
  getRooms: () => fetchJson<Room[]>(`${API_BASE}/rooms`),

  createRoom: () =>
    fetchJson<Room>(`${API_BASE}/rooms`, {
      method: 'POST',
    }),

  joinRoom: (roomCode: string) =>
    fetchJson<Room>(`${API_BASE}/rooms/join`, {
      method: 'POST',
      body: JSON.stringify({ roomCode }),
    }),

  getRoom: (roomId: string) => fetchJson<Room>(`${API_BASE}/rooms/${roomId}`),

  // File
  getFile: (roomId: string) => fetchJson<CodeFile>(`${API_BASE}/rooms/${roomId}/file`),

  saveFile: (roomId: string, content: string) =>
    fetchJson<CodeFile>(`${API_BASE}/rooms/${roomId}/file`, {
      method: 'PUT',
      body: JSON.stringify({ content }),
    }),

  // Execution
  executeCode: (roomId: string, code?: string) =>
    fetchJson<{ executionId: string; status: string; message: string }>(
      `${API_BASE}/rooms/${roomId}/execute`,
      {
        method: 'POST',
        body: JSON.stringify({ code }),
      }
    ),
};
