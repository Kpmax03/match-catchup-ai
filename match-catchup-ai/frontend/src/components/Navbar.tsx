import React from 'react';
import { Flame, LogOut, Radio, Trophy, Tv } from 'lucide-react';

interface NavbarProps {
  displayName: string;
  email: string;
  currentPage: 'home' | 'live' | 'catchup';
  onNavigate: (page: 'home' | 'live' | 'catchup') => void;
  isHeartbeatActive?: boolean;
  onSignOut: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  displayName,
  email,
  currentPage,
  onNavigate,
  isHeartbeatActive = false,
  onSignOut,
}) => {
  // Extract user initials
  const initials = displayName
    ? displayName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2)
    : 'U';

  return (
    <header className="sticky top-0 z-40 bg-[#090d19]/90 backdrop-blur-xl border-b border-slate-800/80 shadow-xl shadow-black/20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-18 flex items-center justify-between gap-4">
        {/* Brand / Logo */}
        <div className="flex items-center gap-6">
          <button
            onClick={() => onNavigate('home')}
            className="flex items-center gap-3 text-left group cursor-pointer focus:outline-none"
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-rose-600 via-red-500 to-amber-500 flex items-center justify-center text-white shadow-lg shadow-rose-950/60 group-hover:scale-105 transition-transform">
              <Flame className="w-5 h-5 text-amber-200 fill-amber-200" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-xl tracking-tight text-white group-hover:text-rose-400 transition-colors">
                  MatchCatchup
                </span>
                <span className="text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-md bg-gradient-to-r from-rose-600 to-amber-500 text-white shadow-sm">
                  AI
                </span>
              </div>
              <p className="text-[11px] font-semibold text-slate-400 tracking-wide">
                Live cricket, on your time
              </p>
            </div>
          </button>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-2 pl-4 border-l border-slate-800/80">
            <button
              onClick={() => onNavigate('home')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                currentPage === 'home'
                  ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30 shadow-sm'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
              }`}
            >
              <Trophy className="w-3.5 h-3.5" />
              <span>Fixtures</span>
            </button>

            <button
              onClick={() => onNavigate('live')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                currentPage === 'live' || currentPage === 'catchup'
                  ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30 shadow-sm'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800/50'
              }`}
            >
              <Tv className="w-3.5 h-3.5" />
              <span>Live Center</span>
              {currentPage === 'live' && (
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse ml-0.5" />
              )}
            </button>
          </nav>
        </div>

        {/* Right Section: Match Pulse & User Menu */}
        <div className="flex items-center gap-4">
          {/* Active Heartbeat / On-Air Indicator */}
          {currentPage === 'live' && (
            <div
              className={`hidden sm:flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold border transition-colors ${
                isHeartbeatActive
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                  : 'bg-slate-800/60 border-slate-700/60 text-slate-400'
              }`}
            >
              <Radio
                size={13}
                className={isHeartbeatActive ? 'animate-pulse text-emerald-400' : 'text-slate-500'}
              />
              <span className="tracking-wide">
                {isHeartbeatActive ? 'SYNCED TO LIVE' : 'SYNC PAUSED'}
              </span>
            </div>
          )}

          {/* User Profile Info & Initials Circle */}
          <div className="flex items-center gap-3 pl-3 border-l border-slate-800/80">
            <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-slate-800 to-slate-700 border border-slate-600/60 flex items-center justify-center text-xs font-black text-rose-400 shadow-inner">
              {initials}
            </div>

            <div className="hidden lg:block text-left">
              <span className="block text-xs font-bold text-white leading-tight">
                {displayName}
              </span>
              <span className="text-[11px] text-slate-400 truncate max-w-[130px] block leading-tight">
                {email}
              </span>
            </div>

            {/* Sign Out Button */}
            <button
              onClick={onSignOut}
              className="flex items-center gap-1.5 rounded-lg border border-slate-800 bg-slate-900/60 px-3 py-2 text-xs font-semibold text-slate-300 hover:border-rose-500/60 hover:text-white hover:bg-rose-950/30 transition-all cursor-pointer"
              title="Sign out of MatchCatchup AI"
            >
              <LogOut size={14} className="text-slate-400 group-hover:text-rose-400" />
              <span className="hidden sm:inline">Sign out</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
