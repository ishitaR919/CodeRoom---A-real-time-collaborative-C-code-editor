import React from 'react';
import Link from 'next/link';
import { User } from '../types';
import { Code2, LogOut, User as UserIcon } from 'lucide-react';

interface NavbarProps {
  user: User | null;
  onLogout?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, onLogout }) => {
  return (
    <header className="bg-dark-800 border-b border-dark-700 px-6 py-3 flex items-center justify-between">
      <Link href="/dashboard" className="flex items-center space-x-3 text-blue-400 font-bold text-lg hover:text-blue-300 transition">
        <div className="bg-blue-600/20 p-2 rounded-lg border border-blue-500/30">
          <Code2 className="w-5 h-5 text-blue-400" />
        </div>
        <span className="tracking-wide text-white">Code<span className="text-blue-400">Room</span></span>
      </Link>

      {user && (
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2 bg-dark-700 px-3 py-1.5 rounded-full border border-dark-600">
            <UserIcon className="w-4 h-4 text-gray-400" />
            <span className="text-sm font-medium text-gray-200">{user.name}</span>
          </div>
          {onLogout && (
            <button
              onClick={onLogout}
              className="flex items-center space-x-1.5 text-gray-400 hover:text-red-400 text-sm font-medium transition px-3 py-1.5 rounded-lg hover:bg-dark-700"
            >
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          )}
        </div>
      )}
    </header>
  );
};
