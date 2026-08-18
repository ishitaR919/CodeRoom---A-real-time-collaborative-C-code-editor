import { useEffect, useState, useRef, useCallback } from 'react';
import * as Y from 'yjs';
import { User, PresenceUser, ExecutionResult } from '../types';
import { api } from '../lib/api';

function uint8ArrayToBase64(bytes: Uint8Array): string {
  let binary = '';
  const len = bytes.byteLength;
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

function base64ToUint8Array(base64: string): Uint8Array {
  const binaryString = atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

export function useCollaboration(roomId: string, user: User | null, initialCode: string = '') {
  const [ydoc] = useState(() => new Y.Doc());
  const [yText] = useState(() => ydoc.getText('monaco'));
  const [onlineUsers, setOnlineUsers] = useState<PresenceUser[]>([]);
  const [executionResult, setExecutionResult] = useState<ExecutionResult | null>(null);
  const [isExecuting, setIsExecuting] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);

  const socketRef = useRef<WebSocket | null>(null);
  const saveTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const isInitialSetRef = useRef(false);

  // Auto save to PostgreSQL
  const triggerSave = useCallback(
    async (codeContent: string) => {
      if (!roomId) return;
      setIsSaving(true);
      try {
        await api.saveFile(roomId, codeContent);
        setLastSavedAt(new Date());
      } catch (err) {
        console.error('Failed to auto-save file', err);
      } finally {
        setIsSaving(false);
      }
    },
    [roomId]
  );

  // Initialize Yjs document with initial code if empty
  useEffect(() => {
    if (initialCode && yText.toString() === '' && !isInitialSetRef.current) {
      isInitialSetRef.current = true;
      yText.insert(0, initialCode);
    }
  }, [initialCode, yText]);

  // Connect WebSocket & Yjs sync
  useEffect(() => {
    if (!roomId || !user) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = window.location.port === '3000'
      ? `${protocol}//${window.location.hostname}:8080/ws`
      : `${protocol}//${window.location.host}/ws`;

    const ws = new WebSocket(wsUrl);
    socketRef.current = ws;

    ws.onopen = () => {
      // Send JOIN payload
      ws.send(
        JSON.stringify({
          type: 'JOIN',
          roomId,
          userId: user.id,
          userName: user.name,
        })
      );

      // Send current state vector if we have local changes
      const stateUpdate = Y.encodeStateAsUpdate(ydoc);
      if (stateUpdate.length > 0) {
        const base64Update = uint8ArrayToBase64(stateUpdate);
        ws.send(
          JSON.stringify({
            type: 'YJS_UPDATE',
            roomId,
            update: base64Update,
          })
        );
      }
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'PRESENCE') {
          setOnlineUsers(data.users || []);
        } else if (data.type === 'YJS_UPDATE') {
          if (data.update) {
            const binaryUpdate = base64ToUint8Array(data.update);
            Y.applyUpdate(ydoc, binaryUpdate, 'websocket');
          }
        } else if (data.type === 'EXECUTION_RESULT') {
          setExecutionResult({
            executionId: data.executionId,
            roomId: data.roomId,
            userId: data.userId,
            status: data.status,
            output: data.output,
            errorOutput: data.errorOutput,
            executionTimeMs: data.executionTimeMs,
          });
          setIsExecuting(false);
        }
      } catch (err) {
        console.error('Error handling WS message', err);
      }
    };

    ws.onerror = (err) => {
      console.error('WebSocket error:', err);
    };

    // Listen to local Yjs changes and broadcast
    const handleYjsUpdate = (update: Uint8Array, origin: any) => {
      if (origin !== 'websocket' && ws.readyState === WebSocket.OPEN) {
        const base64Update = uint8ArrayToBase64(update);
        ws.send(
          JSON.stringify({
            type: 'YJS_UPDATE',
            roomId,
            update: base64Update,
          })
        );
      }

      // Schedule debounced save
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
      saveTimeoutRef.current = setTimeout(() => {
        triggerSave(yText.toString());
      }, 3000);
    };

    ydoc.on('update', handleYjsUpdate);

    return () => {
      ydoc.off('update', handleYjsUpdate);
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
      if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
        ws.close();
      }
    };
  }, [roomId, user, ydoc, yText, triggerSave]);

  const executeCode = async () => {
    if (!roomId) return;
    setIsExecuting(true);
    setExecutionResult({
      executionId: '',
      roomId,
      userId: user?.id || '',
      status: 'PENDING',
      output: '',
      errorOutput: '',
      executionTimeMs: 0,
    });
    try {
      const currentCode = yText.toString();
      await api.executeCode(roomId, currentCode);
    } catch (err: any) {
      setIsExecuting(false);
      setExecutionResult({
        executionId: '',
        roomId,
        userId: user?.id || '',
        status: 'SYSTEM_ERROR',
        output: '',
        errorOutput: err.message || 'Failed to submit execution request',
        executionTimeMs: 0,
      });
    }
  };

  const manualSave = () => {
    triggerSave(yText.toString());
  };

  return {
    ydoc,
    yText,
    onlineUsers,
    executionResult,
    isExecuting,
    isSaving,
    lastSavedAt,
    executeCode,
    manualSave,
  };
}
