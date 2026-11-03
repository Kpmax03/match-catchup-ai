import React, { useCallback, useEffect, useRef, useState } from 'react';
import { CatchupSession, LiveScore } from '../types';
import { fetchLiveScore, sendHeartbeat, sendViewedDelivery, fetchCatchupAvailable, resetMatchSimulation } from '../api';
import { BroadcastScorecard } from '../components/BroadcastScorecard';
import { FloatingChatbot } from '../components/FloatingChatbot';
import { Sparkles, ArrowRight, Clock, RefreshCw, ArrowLeft, RotateCcw, ShieldCheck, Flame } from 'lucide-react';

interface LiveMatchPageProps {
  matchId: string;
  onOpenCatchup: (sessionId: string) => void;
  onHeartbeatStateChange?: (active: boolean) => void;
  onBackToMatches?: () => void;
}

export const LiveMatchPage: React.FC<LiveMatchPageProps> = ({
  matchId,
  onOpenCatchup,
  onHeartbeatStateChange,
  onBackToMatches,
}) => {
  const [score, setScore] = useState<LiveScore | null>(null);
  const [session, setSession] = useState<CatchupSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [resetting, setResetting] = useState(false);

  const sessionRef = useRef<CatchupSession | null>(null);
  sessionRef.current = session;

  // Poll live scoreboard
  const loadMatchData = useCallback(async () => {
    try {
      const currentScore = await fetchLiveScore(matchId);
      setScore(currentScore);
      setError('');

      // Record viewing progress for the user ONLY IF caught up (no pending catch-up session)
      if (currentScore && currentScore.latestDeliveryId > 0 && !sessionRef.current) {
        await sendViewedDelivery(matchId, currentScore.latestDeliveryId).catch(() => {});
      }
    } catch (e: any) {
      setError(e instanceof Error ? e.message : 'Unable to load the live scoreboard.');
    } finally {
      setLoading(false);
    }
  }, [matchId]);

  // Automatic heartbeat every 15s (detects return from inactivity & missed deliveries)
  const heartbeat = useCallback(async () => {
    try {
      const activity = await sendHeartbeat(matchId);
      if (activity.catchupAvailable && activity.catchupSession) {
        sessionRef.current = activity.catchupSession;
        setSession(activity.catchupSession);
      }
    } catch (e) {
      console.error('Match heartbeat failed:', e);
    }
  }, [matchId]);

  // Lifecycle: Starts heartbeats and polling on mount; cleans up completely on unmount
  useEffect(() => {
    onHeartbeatStateChange?.(true);

    // Initial sequence: First check available catchup session and fire heartbeat
    async function init() {
      try {
        const existingSession = await fetchCatchupAvailable(matchId).catch(() => null);
        if (existingSession && existingSession.status === 'PENDING') {
          sessionRef.current = existingSession;
          setSession(existingSession);
        } else {
          sessionRef.current = null;
          setSession(null);
          await heartbeat();
        }
      } catch (err) {
        console.error('Initial catchup/heartbeat check error:', err);
      } finally {
        await loadMatchData();
      }
    }

    void init();

    const heartInterval = setInterval(() => void heartbeat(), 15000);
    const scoreInterval = setInterval(() => void loadMatchData(), 10000);

    // Refresh immediately when returning from another tab
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void heartbeat();
        void loadMatchData();
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      clearInterval(heartInterval);
      clearInterval(scoreInterval);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      onHeartbeatStateChange?.(false);
    };
  }, [matchId, heartbeat, loadMatchData, onHeartbeatStateChange]);

  const handleResetMatch = async () => {
    try {
      setResetting(true);
      await resetMatchSimulation(matchId);
      setSession(null);
      await loadMatchData();
    } catch (err) {
      console.error('Failed to reset match simulation:', err);
    } finally {
      setResetting(false);
    }
  };

  const formatTime = (isoString?: string) => {
    if (!isoString) return '';
    try {
      const d = new Date(isoString);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return '';
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Navigation Row */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div className="flex items-center gap-3">
          {onBackToMatches && (
            <button
              onClick={onBackToMatches}
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 hover:text-white text-xs font-bold text-slate-300 transition-all cursor-pointer shadow-sm"
            >
              <ArrowLeft className="w-4 h-4 text-rose-500" />
              <span>Back to Fixtures</span>
            </button>
          )}

          <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/60 border border-slate-800 text-xs font-mono text-slate-400">
            <span>Match:</span>
            <strong className="text-white">{matchId}</strong>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleResetMatch}
            disabled={resetting}
            className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white text-xs font-bold border border-slate-700/80 transition-all cursor-pointer disabled:opacity-50 shadow-sm"
            title="Restart simulation from delivery 1 (over 0.1)"
          >
            <RotateCcw className={`w-3.5 h-3.5 text-amber-400 ${resetting ? 'animate-spin' : ''}`} />
            <span>{resetting ? 'Restarting...' : 'Restart from Ball 1'}</span>
          </button>
        </div>
      </div>

      {/* Prominent Match Catch-Up Alert Banner (Shown when user returns from away) */}
      {session && (
        <div className="relative rounded-3xl border-2 border-rose-500/80 bg-gradient-to-r from-rose-950/90 via-slate-900 to-slate-900 p-6 sm:p-8 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-2xl shadow-rose-950/70 overflow-hidden animate-in fade-in duration-300">
          {/* Ambient Corner Flare */}
          <div className="absolute top-0 right-0 w-80 h-40 bg-gradient-to-l from-rose-600/20 via-amber-500/10 to-transparent pointer-events-none" />

          <div className="flex items-start gap-5 relative z-10">
            <div className="p-4 rounded-2xl bg-gradient-to-tr from-rose-600 to-amber-500 text-white shadow-lg shadow-rose-950/80 shrink-0">
              <Sparkles className="w-7 h-7 text-yellow-100 animate-pulse" />
            </div>

            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-rose-500/20 text-rose-300 text-xs font-black uppercase tracking-wider mb-2 border border-rose-500/30">
                <Flame className="w-3.5 h-3.5 text-amber-300" />
                <span>You were away from the match</span>
              </div>

              <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
                Catch Up Ready: {session.missedDeliveriesCount} Deliveries Missed
              </h2>

              <div className="mt-2 flex flex-wrap items-center gap-3 text-xs sm:text-sm text-slate-300">
                <span className="bg-slate-900/80 px-2.5 py-1 rounded-lg border border-slate-800">
                  Overs missed: <strong className="text-amber-300 font-mono">{session.fromOver} to {session.toOver}</strong>
                </span>

                {session.inactiveFrom && (
                  <span className="flex items-center gap-1.5 bg-slate-900/80 px-2.5 py-1 rounded-lg border border-slate-800 text-rose-300 font-medium">
                    <Clock size={13} className="text-rose-400" />
                    Away: <strong className="text-white">{formatTime(session.inactiveFrom)}</strong>
                    {session.returnedAt && <> → <strong className="text-white">{formatTime(session.returnedAt)}</strong></>}
                  </span>
                )}

                <span className="hidden lg:inline text-slate-400 text-xs">
                  Grounded AI summary & ball-by-ball timeline ready
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={() => onOpenCatchup(session.id)}
            className="relative z-10 py-4 px-8 rounded-2xl bg-gradient-to-r from-rose-600 via-red-500 to-amber-500 hover:from-rose-500 hover:to-amber-400 text-white font-black text-sm tracking-wide shadow-xl shadow-rose-950/60 flex items-center justify-center gap-3 transition-all hover:scale-105 cursor-pointer shrink-0 animate-shimmer"
          >
            <span>START CATCH-UP</span>
            <ArrowRight className="w-4 h-4 text-yellow-100" />
          </button>
        </div>
      )}

      {/* Error notification */}
      {error && (
        <div role="alert" className="rounded-2xl border border-rose-800/80 bg-rose-950/40 p-4 text-sm text-rose-200">
          {error}
        </div>
      )}

      {/* Broadcast Scoreboard (Clean live scoreboard with batting and bowling figures) */}
      {loading || !score ? (
        <div className="rounded-3xl border border-slate-800 bg-[#0f172a] p-20 text-center text-slate-400 shadow-2xl">
          <RefreshCw className="w-10 h-10 animate-spin mx-auto mb-4 text-rose-500" />
          <p className="text-base font-extrabold text-white">Connecting to live match feed...</p>
          <p className="text-xs text-slate-400 mt-1">Synchronizing deliveries with Redis cache</p>
        </div>
      ) : (
        <BroadcastScorecard
          score={score}
          session={session}
          onOpenCatchup={onOpenCatchup}
        />
      )}

      {/* Floating Bottom-Right Chatbot */}
      <FloatingChatbot matchId={matchId} sessionId={session?.id ?? null} />
    </div>
  );
};
