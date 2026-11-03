import React, { useEffect, useState } from 'react';
import { Match } from '../types';
import { fetchMatches } from '../api';
import { Radio, ArrowRight, ShieldCheck, Zap, Bot, Trophy, Sparkles, Activity } from 'lucide-react';

interface HomePageProps {
  onSelectMatch: (matchId: string) => void;
}

export const HomePage: React.FC<HomePageProps> = ({ onSelectMatch }) => {
  const [matches, setMatches] = useState<Match[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMatches()
      .then((data) => setMatches(data))
      .catch((err) => console.error('Failed to load matches:', err))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-12">
      {/* Hero Section */}
      <div className="relative rounded-3xl overflow-hidden p-8 sm:p-14 bg-gradient-to-r from-[#0e1628] via-[#11192e] to-[#1c0f1e] border border-slate-800 shadow-2xl">
        {/* Ambient Stadium Lighting */}
        <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-gradient-to-br from-rose-600/15 via-amber-500/10 to-transparent rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />

        <div className="max-w-3xl relative z-10">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs font-black uppercase tracking-wider mb-6">
            <Zap className="w-4 h-4 text-amber-300" />
            <span>Dream11 MatchCatchup AI Engine</span>
          </div>

          <h1 className="text-4xl sm:text-6xl font-black text-white tracking-tight leading-[1.1] mb-5">
            Never miss a crucial ball when life pulls you away.
          </h1>

          <p className="text-base sm:text-lg text-slate-300 mb-8 leading-relaxed max-w-2xl">
            Real-time cricket tracking with automatic departure detection. Step away, close the tab, or minimize your window—upon return, get an instantaneous factual AI summary, ball-by-ball missed delivery timeline, and a grounded RAG chatbot.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="flex items-start gap-3 bg-slate-900/80 border border-slate-800 p-4 rounded-2xl">
              <div className="p-2 rounded-xl bg-emerald-500/15 text-emerald-400 shrink-0">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <span className="text-xs font-extrabold text-white block">
                  Automatic Detection
                </span>
                <span className="text-[11px] text-slate-400 mt-0.5 block">
                  Tracks exact departure & return timestamps
                </span>
              </div>
            </div>

            <div className="flex items-start gap-3 bg-slate-900/80 border border-slate-800 p-4 rounded-2xl">
              <div className="p-2 rounded-xl bg-amber-500/15 text-amber-400 shrink-0">
                <Sparkles className="w-5 h-5" />
              </div>
              <div>
                <span className="text-xs font-extrabold text-white block">
                  Zero Hallucinations
                </span>
                <span className="text-[11px] text-slate-400 mt-0.5 block">
                  Ground-truth facts verified against ball events
                </span>
              </div>
            </div>

            <div className="flex items-start gap-3 bg-slate-900/80 border border-slate-800 p-4 rounded-2xl">
              <div className="p-2 rounded-xl bg-purple-500/15 text-purple-400 shrink-0">
                <Bot className="w-5 h-5" />
              </div>
              <div>
                <span className="text-xs font-extrabold text-white block">
                  Session-Isolated RAG
                </span>
                <span className="text-[11px] text-slate-400 mt-0.5 block">
                  Chatbot bounded strictly to your missed overs
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Available Fixtures Section */}
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <Activity className="w-5 h-5 text-rose-500" />
              <h2 className="text-2xl font-black text-white tracking-tight">
                ACTIVE FIXTURES
              </h2>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Select a live match to view live broadcast scores and test the catch-up experience
            </p>
          </div>
        </div>

        {loading ? (
          <div className="sports-card rounded-3xl p-16 text-center text-slate-400 border border-slate-800">
            <div className="w-10 h-10 border-2 border-rose-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-sm font-bold text-white">Loading live fixtures from microservices...</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {matches.map((match) => (
              <div
                key={match.id}
                className="sports-card rounded-3xl p-6 sm:p-7 border border-slate-800 hover:border-rose-500/50 transition-all shadow-xl group relative overflow-hidden flex flex-col justify-between"
              >
                {/* Top Bar: Live Status & Format */}
                <div>
                  <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-2">
                      <span className="px-3 py-1 rounded-full bg-rose-600 text-white text-xs font-black flex items-center gap-1.5 shadow-sm shadow-rose-950/60 uppercase">
                        <Radio className="w-3 h-3 animate-pulse text-white" />
                        {match.status}
                      </span>
                      <span className="text-xs font-extrabold text-slate-400 uppercase tracking-wider">
                        {match.format} Series
                      </span>
                    </div>

                    <span className="text-xs font-mono text-slate-500 bg-slate-900 px-2 py-0.5 rounded border border-slate-800">
                      ID: {match.id}
                    </span>
                  </div>

                  {/* Team Crests & Score Banner */}
                  <div className="mb-6 space-y-4">
                    <div className="flex items-center gap-4">
                      {/* Team Crest */}
                      <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-700 to-slate-900 border border-amber-400/50 flex items-center justify-center font-black text-base text-white shadow-md">
                        {match.battingTeam.slice(0, 3).toUpperCase()}
                      </div>
                      <div>
                        <h3 className="text-xl sm:text-2xl font-black text-white group-hover:text-rose-400 transition-colors">
                          {match.matchName}
                        </h3>
                        <p className="text-xs text-slate-400">
                          Batting: <strong className="text-slate-200">{match.battingTeam}</strong> vs{' '}
                          <strong className="text-slate-200">{match.bowlingTeam}</strong>
                        </p>
                      </div>
                    </div>

                    {/* Big Score Display */}
                    <div className="bg-slate-900/90 rounded-2xl p-4 border border-slate-800/80 flex items-center justify-between">
                      <div>
                        <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">
                          Innings {match.innings} Score
                        </span>
                        <div className="flex items-baseline gap-2">
                          <span className="text-3xl font-black text-white font-mono">
                            {match.totalScore}
                            <span className="text-rose-500 font-light mx-0.5">/</span>
                            {match.wickets}
                          </span>
                          <span className="text-xs font-bold text-slate-400 font-mono">
                            ({match.overs}.{match.balls} ov)
                          </span>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="text-[10px] uppercase font-bold text-slate-400 block mb-0.5">
                          Run Rate
                        </span>
                        <span className="text-xl font-black text-emerald-400 font-mono">
                          {match.runRate.toFixed(2)}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Primary CTA */}
                <button
                  onClick={() => onSelectMatch(match.id)}
                  className="w-full py-4 px-5 rounded-2xl bg-gradient-to-r from-rose-600 to-rose-500 hover:from-rose-500 hover:to-rose-400 text-white font-black text-sm shadow-xl shadow-rose-950/50 flex items-center justify-center gap-2.5 transition-all group-hover:scale-[1.01] cursor-pointer"
                >
                  <span>Open Live Scoreboard</span>
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
