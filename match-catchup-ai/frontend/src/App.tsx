import { useEffect, useRef, useState } from 'react';
import { Navbar } from './components/Navbar';
import { HomePage } from './pages/HomePage';
import { LiveMatchPage } from './pages/LiveMatchPage';
import { CatchupPage } from './pages/CatchupPage';
import { AuthPage } from './pages/AuthPage';
import { AuthUser, fetchMe, saveToken, getToken, sendLeaveMatch } from './api';

export function App() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [checking, setChecking] = useState(true);
  const [currentPage, setCurrentPage] = useState<'home' | 'live' | 'catchup'>('home');
  const [selectedMatchId, setSelectedMatchId] = useState('IND-AUS-001');
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [isHeartbeatActive, setIsHeartbeatActive] = useState(false);

  // Remember the page user was on before auto-logout so they resume on live score
  const lastActivePageRef = useRef<'home' | 'live' | 'catchup'>('home');
  lastActivePageRef.current = currentPage;

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setChecking(false);
      return;
    }
    fetchMe()
      .then(setUser)
      .catch(() => saveToken(null))
      .finally(() => setChecking(false));
  }, []);

  const signOut = () => {
    if (selectedMatchId) {
      sendLeaveMatch(selectedMatchId);
    }
    saveToken(null);
    setUser(null);
    setActiveSessionId(null);
  };

  // Requirement: if user closes the tab, log out. If user opens/switches to another tab, do NOT log out.
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden' && user) {
        // User switched to another tab or minimized window:
        // Record match away timestamp so missed deliveries are tracked, but DO NOT log out!
        if (selectedMatchId) {
          sendLeaveMatch(selectedMatchId);
        }
      }
    };

    const handleBeforeUnload = () => {
      if (user) {
        // User is closing the tab: record departure and log out
        if (selectedMatchId) {
          sendLeaveMatch(selectedMatchId);
        }
        saveToken(null);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [user, selectedMatchId]);

  const authenticated = (profile: AuthUser) => {
    saveToken(profile.token);
    setUser(profile);
    // When logging back in after auto-logout, open live score where catch-up is available
    if (lastActivePageRef.current === 'live' || lastActivePageRef.current === 'catchup') {
      setCurrentPage('live');
    } else {
      setCurrentPage('home');
    }
  };

  if (checking) {
    return (
      <div className="min-h-screen bg-[#0b0f19] grid place-items-center text-slate-400">
        Checking your session…
      </div>
    );
  }

  if (!user) {
    return <AuthPage onAuthenticated={authenticated} />;
  }

  const openCatchup = (id: string) => {
    setActiveSessionId(id);
    setCurrentPage('catchup');
  };

  return (
    <div className="min-h-screen bg-[#0b0f19] flex flex-col selection:bg-rose-500 selection:text-white">
      <Navbar
        displayName={user.displayName}
        email={user.email}
        currentPage={currentPage}
        onNavigate={setCurrentPage}
        isHeartbeatActive={isHeartbeatActive}
        onSignOut={signOut}
      />
      <main className="flex-1">
        {currentPage === 'home' && (
          <HomePage
            onSelectMatch={(id) => {
              setSelectedMatchId(id);
              setCurrentPage('live');
            }}
          />
        )}
        {currentPage === 'live' && (
          <LiveMatchPage
            matchId={selectedMatchId}
            onOpenCatchup={openCatchup}
            onHeartbeatStateChange={setIsHeartbeatActive}
            onBackToMatches={() => setCurrentPage('home')}
          />
        )}
        {currentPage === 'catchup' && activeSessionId && (
          <CatchupPage
            sessionId={activeSessionId}
            onBackToLive={() => {
              setActiveSessionId(null);
              setCurrentPage('live');
            }}
          />
        )}
      </main>
      <footer className="border-t border-slate-800/80 bg-[#070b14] py-6 px-4">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span className="font-bold text-slate-400">MatchCatchup AI Engine v2.0</span>
            <span className="text-slate-600">•</span>
            <span>Broadcast-Grade Cricket Intelligence</span>
          </div>
          <div className="flex items-center gap-4 text-[11px]">
            <span>Tab Close Logout Security</span>
            <span className="text-slate-700">•</span>
            <span>Session-Isolated RAG</span>
            <span className="text-slate-700">•</span>
            <span>Zero Hallucinations</span>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
