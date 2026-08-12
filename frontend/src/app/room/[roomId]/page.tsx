'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useAuth } from '../../../hooks/useAuth';
import { useCollaboration } from '../../../hooks/useCollaboration';
import { api } from '../../../lib/api';
import { Room, CodeFile } from '../../../types';
import dynamic from 'next/dynamic';
import { Navbar } from '../../../components/Navbar';
import { PresenceList } from '../../../components/PresenceList';
import { OutputPanel } from '../../../components/OutputPanel';
import { Play, Copy, Check, FileCode, Save, Loader2 } from 'lucide-react';

const MonacoEditorComponent = dynamic(
  () => import('../../../components/MonacoEditorComponent').then((m) => m.MonacoEditorComponent),
  {
    ssr: false,
    loading: () => (
      <div className="h-full w-full bg-dark-900 flex items-center justify-center text-gray-500 font-mono text-sm">
        <Loader2 className="w-5 h-5 animate-spin text-blue-400 mr-2" />
        Loading Editor...
      </div>
    ),
  }
);

export default function RoomPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const { user, loading: authLoading, logout } = useAuth();
  const router = useRouter();

  const [room, setRoom] = useState<Room | null>(null);
  const [initialFile, setInitialFile] = useState<CodeFile | null>(null);
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) {
      router.replace('/login');
    }
  }, [user, authLoading, router]);

  useEffect(() => {
    if (user && roomId) {
      Promise.all([api.getRoom(roomId), api.getFile(roomId)])
        .then(([roomData, fileData]) => {
          setRoom(roomData);
          setInitialFile(fileData);
        })
        .catch((err) => {
          console.error('Failed to load room or file:', err);
        })
        .finally(() => setLoading(false));
    }
  }, [user, roomId]);

  const {
    yText,
    onlineUsers,
    executionResult,
    isExecuting,
    isSaving,
    lastSavedAt,
    executeCode,
    manualSave,
  } = useCollaboration(roomId || '', user, initialFile?.content || '');

  const handleCopyCode = () => {
    if (room?.roomCode) {
      navigator.clipboard.writeText(room.roomCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-dark-900 text-gray-400">
        <div className="flex items-center space-x-3">
          <Loader2 className="w-5 h-5 animate-spin text-blue-400" />
          <span>Loading Coding Room...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen flex flex-col bg-dark-900 text-gray-100 overflow-hidden">
      <Navbar user={user} onLogout={logout} />

      {/* Room Toolbar */}
      <div className="bg-dark-800 border-b border-dark-700 px-6 py-2.5 flex items-center justify-between shadow-md">
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2 bg-dark-900 border border-dark-600 px-3 py-1.5 rounded-lg">
            <span className="text-xs text-gray-400 font-semibold uppercase">Room Code:</span>
            <span className="font-mono text-sm font-bold text-blue-400 tracking-wider">{room?.roomCode}</span>
            <button
              onClick={handleCopyCode}
              title="Copy Room Code"
              className="text-gray-400 hover:text-white transition ml-1"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            </button>
          </div>

          <div className="text-xs text-gray-400 flex items-center space-x-2">
            {isSaving ? (
              <span className="text-amber-400 flex items-center space-x-1">
                <Loader2 className="w-3 h-3 animate-spin" />
                <span>Saving...</span>
              </span>
            ) : lastSavedAt ? (
              <span className="text-emerald-400 flex items-center space-x-1">
                <Save className="w-3 h-3" />
                <span>Saved at {lastSavedAt.toLocaleTimeString()}</span>
              </span>
            ) : (
              <span>Code synchronized</span>
            )}
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={manualSave}
            title="Save code to database"
            className="bg-dark-700 hover:bg-dark-600 text-gray-300 px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 border border-dark-600 transition"
          >
            <Save className="w-3.5 h-3.5 text-blue-400" />
            <span>Save</span>
          </button>

          <button
            onClick={executeCode}
            disabled={isExecuting}
            className="bg-emerald-600 hover:bg-emerald-500 text-white font-bold px-5 py-1.5 rounded-lg text-sm flex items-center space-x-2 shadow-lg shadow-emerald-600/20 transition disabled:opacity-50"
          >
            {isExecuting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Running...</span>
              </>
            ) : (
              <>
                <Play className="w-4 h-4 fill-current" />
                <span>RUN</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Main Workspace */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Sidebar (Files & Online Users) */}
        <aside className="w-64 bg-dark-800 border-r border-dark-700 flex flex-col justify-between shrink-0">
          <div>
            <div className="p-4 border-b border-dark-700">
              <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Files</div>
              <div className="flex items-center space-x-2 bg-dark-700 text-blue-300 px-3 py-2 rounded-lg border border-blue-500/30 text-sm font-mono">
                <FileCode className="w-4 h-4 text-blue-400" />
                <span>main.cpp</span>
              </div>
            </div>
          </div>

          <PresenceList onlineUsers={onlineUsers} currentUserId={user?.id} />
        </aside>

        {/* Center & Bottom: Monaco Editor & Output Panel */}
        <main className="flex-1 flex flex-col min-w-0">
          <div className="flex-1 min-h-0 relative">
            <MonacoEditorComponent yText={yText} defaultValue={initialFile?.content} />
          </div>

          <div className="h-48 shrink-0">
            <OutputPanel result={executionResult} isExecuting={isExecuting} />
          </div>
        </main>
      </div>
    </div>
  );
}
