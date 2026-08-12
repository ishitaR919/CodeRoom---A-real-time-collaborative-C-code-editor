export interface User {
  id: string;
  name: string;
  email: string;
  createdAt: string;
}

export interface Room {
  id: string;
  roomCode: string;
  createdBy: User;
  createdAt: string;
}

export interface CodeFile {
  id: string;
  roomId: string;
  filename: string;
  content: string;
  updatedAt: string;
}

export interface ExecutionResult {
  executionId: string;
  roomId: string;
  userId: string;
  status: 'PENDING' | 'SUCCESS' | 'COMPILATION_ERROR' | 'RUNTIME_ERROR' | 'TIMEOUT' | 'SYSTEM_ERROR';
  output: string;
  errorOutput: string;
  executionTimeMs: number;
}

export interface PresenceUser {
  userId: string;
  name: string;
}
