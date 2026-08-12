import React from 'react';
import { PresenceUser } from '../types';
import { Users } from 'lucide-react';

interface PresenceListProps {
  onlineUsers: PresenceUser[];
  currentUserId?: string;
}

export const PresenceList: React.FC<PresenceListProps> = ({ onlineUsers, currentUserId }) => {
  return (
    <div className="bg-dark-800 p-4 border-t border-dark-700">
      <div className="flex items-center space-x-2 text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
        <Users className="w-4 h-4 text-blue-400" />
        <span>Online Users ({onlineUsers.length})</span>
      </div>
      <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
        {onlineUsers.length === 0 ? (
          <p className="text-xs text-gray-500 italic">No users connected</p>
        ) : (
          onlineUsers.map((u) => (
            <div key={u.userId} className="flex items-center space-x-2 text-sm text-gray-300 bg-dark-700/50 px-2.5 py-1.5 rounded-md border border-dark-600/50">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span className="font-medium">{u.name}</span>
              {u.userId === currentUserId && (
                <span className="text-[10px] bg-blue-500/20 text-blue-400 border border-blue-500/30 px-1.5 py-0.5 rounded">You</span>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};
