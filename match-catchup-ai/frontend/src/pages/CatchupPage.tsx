import React, { useEffect, useState } from 'react';
import { CatchupSession, TimelineData, LiveScore } from '../types';
import {
  fetchCatchupSession,
  fetchTimeline,
  fetchSummary,
  fetchLiveScore,
  completeCatchup,
} from '../api';
import { AiSummaryCard } from '../components/AiSummaryCard';
import { TimelineSection } from '../components/TimelineSection';
import { FloatingChatbot } from '../components/FloatingChatbot';
import { ScoreCard } from '../components/ScoreCard';
import { ArrowLeft, CheckCircle, Clock, Sparkles, RefreshCw, Trophy, Flame } from 'lucide-react';

interface CatchupPageProps {
  sessionId: string;
  onBackToLive: () => void;
}

export const CatchupPage: React.FC<CatchupPageProps> = ({
  sessionId,
  onBackToLive,
}) => {
  const [session, setSession] = useState<CatchupSession | null>(null);
  const [timeline, setTimeline] = useState<TimelineData | null>(null);
  const [summary, setSummary] = useState<string>('');
  const [liveScore, setLiveScore] = useState<LiveScore | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [completing, setCompleting] = useState(false);

  useEffect(() => {
    async function loadCatchupData() {
      try {
        setLoading(true);
        setError(null);
        const sessData = await fetchCatchupSession(sessionId);
        const [timelineData, summaryText, currentScore] = await Promise.all([
          fetchTimeline(sessionId),
          fetchSummary(sessionId).catch(() => 'AI summary is currently preparing factual highlights...'),
          fetchLiveScore(sessData.matchId).catch(() => null),
        ]);

        setSession(sessData);
        setTimeline(timelineData);
        setSummary(summaryText);
        setLiveScore(currentScore);
      } catch (err: any) {
        console.error('Failed to load catch-up data:', err);
        setError(err.message || 'Unable to load catch-up data.');
      } finally {
        setLoading(false);
      }
    }

    loadCatchupData();
  }, [sessionId]);

  const handleFinishCatchup = async () => {
    try {
      setCompleting(true);
      await completeCatchup(sessionId);
      onBackToLive();
    } catch (err) {
      console.error('Failed to complete catch-up session:', err);
      onBackToLive();
    } finally {
      setCompleting(false);
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

  if (error) {
    return (
      <div className="max-w-xl mx-auto px-4 py-20 text-center text-slate-300 space-y-4">
        <p className="text-rose-400 font-bold text-sm">{error}</p>
        <button
          onClick={onBackToLive}
          className="px-6 py-3 rounded-2xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs transition-colors cursor-pointer"
        >
          Return to Live Match
        </button>
      </div>
    );
  }

  if (loading || !session || !timeline) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-24 text-center text-slate-400">
        <RefreshCw className="w-12 h-12 animate-spin mx-auto mb-4 text-rose-500" />
        <h3 className="text-xl font-black text-white mb-2">
          Compiling your missed cricket timeline...
        </h3>
        <p className="text-xs text-slate-400 max-w-md mx-auto">
          Retrieving ball-by-ball deliveries from database, generating ground-truth AI summary, and arming RAG chat vectors
        </p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Navigation & Finish Button Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <button
          onClick={onBackToLive}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 hover:text-white text-xs font-bold text-slate-300 transition-all cursor-pointer shadow-sm w-fit"
        >
          <ArrowLeft className="w-4 h-4 text-rose-500" />
          <span>Back to Live Match</span>
        </button>

        <div className="flex items-center gap-3">
          <button
            onClick={handleFinishCatchup}
            disabled={completing}
            className="py-3 px-6 rounded-2xl bg-gradient-to-r from-emerald-600 via-emerald-500 to-teal-500 hover:from-emerald-500 hover:to-teal-400 text-white font-black text-xs tracking-wider uppercase flex items-center gap-2.5 shadow-xl shadow-emerald-950/60 transition-all cursor-pointer disabled:opacity-50 hover:scale-105"
          >
            <CheckCircle className="w-4 h-4 text-emerald-100" />
            <span>
              {completing ? 'Completing...' : 'Finish Catch-Up & Return to Live'}
            </span>
          </button>
        </div>
      </div>

      {/* Missed Overview Hero Banner */}
      <div className="sports-card rounded-3xl p-6 sm:p-8 border border-rose-500/40 relative overflow-hidden shadow-2xl">
        <div className="absolute top-0 right-0 w-96 h-48 bg-rose-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs font-black uppercase tracking-wider mb-3">
              <Sparkles className="w-3.5 h-3.5 text-amber-300" />
              <span>MISSED MATCH INTERVAL REVIEW</span>
            </div>

            <h1 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
              India vs Australia <span className="text-slate-400 font-light text-2xl">T20</span>
            </h1>

            <div className="mt-3 flex flex-wrap items-center gap-3 text-xs sm:text-sm text-slate-300">
              <span className="bg-slate-900/90 px-3 py-1 rounded-xl border border-slate-800">
                Missed Deliveries:{' '}
                <strong className="text-rose-400 font-mono font-black text-sm">
                  {timeline.totalDeliveries} balls
                </strong>
              </span>

              <span className="bg-slate-900/90 px-3 py-1 rounded-xl border border-slate-800">
                Overs:{' '}
                <strong className="text-white font-mono font-bold">
                  {session.fromOver} to {session.toOver}
                </strong>
              </span>

              {session.inactiveFrom && (
                <span className="flex items-center gap-1.5 bg-slate-900/90 px-3 py-1 rounded-xl border border-slate-800 text-rose-300 font-medium">
                  <Clock className="w-3.5 h-3.5 text-rose-400" />
                  Away: <strong className="text-white">{formatTime(session.inactiveFrom)}</strong>
                  {session.returnedAt && <> → <strong className="text-white">{formatTime(session.returnedAt)}</strong></>}
                </span>
              )}
            </div>
          </div>

          {/* Quick Metrics Cards */}
          <div className="grid grid-cols-4 gap-3 bg-slate-900/90 p-4 rounded-2xl border border-slate-800 text-center shadow-inner">
            <div className="p-2">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">
                Runs
              </span>
              <span className="text-2xl font-black text-white font-mono">
                {timeline.totalRuns}
              </span>
            </div>

            <div className="p-2">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">
                Wickets
              </span>
              <span className="text-2xl font-black text-rose-400 font-mono">
                {timeline.totalWickets}
              </span>
            </div>

            <div className="p-2">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">
                Fours
              </span>
              <span className="text-2xl font-black text-amber-400 font-mono">
                {timeline.totalFours}
              </span>
            </div>

            <div className="p-2">
              <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">
                Sixes
              </span>
              <span className="text-2xl font-black text-purple-400 font-mono">
                {timeline.totalSixes}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Current Match Context Scorecard */}
      {liveScore && <ScoreCard score={liveScore} />}

      {/* AI Ground-Truth Summary Card */}
      <AiSummaryCard
        summary={summary}
        totalDeliveries={timeline.totalDeliveries}
        fromOver={session.fromOver}
        toOver={session.toOver}
      />

      {/* Missed Ball-by-Ball Timeline */}
      <TimelineSection overs={timeline.overs} />

      {/* Floating Chatbot */}
      <FloatingChatbot matchId={session.matchId} sessionId={sessionId} />
    </div>
  );
};
