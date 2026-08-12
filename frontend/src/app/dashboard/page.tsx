'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../../hooks/useAuth';
import { api } from '../../lib/api';
import { Room } from '../../types';
import { Navbar } from '../../components/Navbar';
import { Plus, LogIn, ArrowRight, Code, Sparkles, Loader2, AlertCircle } from 'lucide-react';

export default function DashboardPage() {
  const { user, loading, logout } = useAuth();
  const [rooms, setRooms] = useState<Room[]>([]);
  const [fetchingRooms, setFetchingRooms] = useState(true);
  const [joinCode, setJoinCode] = useState('');
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);
  const [joining, setJoining] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.replace('/login');
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (user) {
      api
        .getRooms()
        .then((data) => setRooms(data))
        .catch((err) => console.error('Failed to load rooms:', err))
        .finally(() => setFetchingRooms(false));
    }
  }, [user]);

  const handleCreateRoom = async () => {
    setError('');
    setCreating(true);
    try {
      const room = await api.createRoom();
      router.push(`/room/${room.id}`);
    } catch (err: any) {
      setError(err.message || 'Failed to create room.');
      setCreating(false);
    }
  };

  const handleJoinRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinCode.trim()) return;
    setError('');
    setJoining(true);
    try {
      const room = await api.joinRoom(joinCode.trim());
      router.push(`/room/${room.id}`);
    } catch (err: any) {
      setError(err.message || 'Invalid room code.');
      setJoining(false);
    }
  };

  if (loading || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-dark-900 text-gray-400">
        <div className="flex items-center space-x-3">
          <Loader2 className="w-5 h-5 animate-spin text-blue-400" />
          <span>Loading Dashboard...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-dark-900 flex flex-col text-gray-100">
      <Navbar user={user} onLogout={logout} />

      <main className="flex-1 max-w-5xl w-full mx-auto px-6 py-10 space-y-10">
        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-blue-900/30 via-dark-800 to-dark-800 border border-blue-500/20 rounded-2xl p-8 flex flex-col md:flex-row items-start md:items-center justify-between shadow-xl">
          <div>
            <div className="flex items-center space-x-2 text-blue-400 text-sm font-semibold mb-2">
              <Sparkles className="w-4 h-4" />
              <span>Real-Time C++ Environment</span>
            </div>
            <h1 className="text-3xl font-bold text-white">Welcome back, {user.name}</h1>
            <p className="text-gray-400 text-sm mt-1 max-w-xl">
              Create a new coding room or join your peers using a room code to edit C++ code together live and run it inside Docker.
            </p>
          </div>

          <button
            onClick={handleCreateRoom}
            disabled={creating}
            className="mt-6 md:mt-0 bg-blue-600 hover:bg-blue-500 text-white font-semibold px-6 py-3 rounded-xl flex items-center space-x-2 shadow-lg shadow-blue-600/25 transition disabled:opacity-50"
          >
            {creating ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <>
                <Plus className="w-5 h-5" />
                <span>Create Room</span>
              </>
            )}
          </button>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-4 rounded-xl flex items-center space-x-3 text-sm">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Join Room Form */}
        <div className="bg-dark-800 border border-dark-700 rounded-2xl p-6 shadow-md">
          <h2 className="text-lg font-bold text-white mb-2">Join an Existing Room</h2>
          <p className="text-xs text-gray-400 mb-4">Enter a 6-character room code shared by your peer (e.g. ABC123)</p>

          <form onSubmit={handleJoinRoom} className="flex flex-col sm:flex-row gap-3">
            <input
              type="text"
              required
              value={joinCode}
              onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
              placeholder="Enter Room Code (e.g. ABC123)"
              maxLength={10}
              className="flex-1 bg-dark-900 border border-dark-600 rounded-xl px-4 py-2.5 text-white placeholder-gray-500 focus:outline-none focus:border-blue-500 uppercase tracking-widest font-mono text-sm"
            />
            <button
              type="submit"
              disabled={joining}
              className="bg-dark-700 hover:bg-dark-600 text-white font-medium px-6 py-2.5 rounded-xl flex items-center justify-center space-x-2 border border-dark-600 transition disabled:opacity-50"
            >
              {joining ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  <LogIn className="w-4 h-4 text-blue-400" />
                  <span>Join Room</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* My Rooms List */}
        <div className="space-y-4">
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <Code className="w-5 h-5 text-blue-400" />
            <span>My Rooms</span>
          </h2>

          {fetchingRooms ? (
            <div className="py-12 text-center text-gray-500 flex items-center justify-center space-x-2">
              <Loader2 className="w-5 h-5 animate-spin" />
              <span>Loading your rooms...</span>
            </div>
          ) : rooms.length === 0 ? (
            <div className="bg-dark-800/50 border border-dark-700/50 rounded-2xl p-10 text-center text-gray-400 space-y-3">
              <Code className="w-10 h-10 mx-auto text-gray-600" />
              <p className="font-medium text-gray-300">No coding rooms created yet.</p>
              <p className="text-xs text-gray-500 max-w-sm mx-auto">
                Click "Create Room" above to start your first real-time C++ collaboration session.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {rooms.map((r, index) => (
                <div
                  key={r.id}
                  className="bg-dark-800 border border-dark-700 hover:border-dark-600 rounded-xl p-5 flex flex-col justify-between transition group shadow-md"
                >
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                        Room #{index + 1}
                      </span>
                      <span className="bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-mono font-bold px-2.5 py-1 rounded-md tracking-wider">
                        {r.roomCode}
                      </span>
                    </div>
                    <p className="text-xs text-gray-500">
                      Created: {new Date(r.createdAt).toLocaleDateString()}
                    </p>
                  </div>

                  <button
                    onClick={() => router.push(`/room/${r.id}`)}
                    className="mt-5 w-full bg-dark-700 hover:bg-blue-600 text-gray-200 hover:text-white font-medium py-2 rounded-lg flex items-center justify-center space-x-2 transition border border-dark-600 hover:border-blue-500"
                  >
                    <span>Open Room</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
