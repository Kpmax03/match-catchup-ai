import React from 'react';
import { LiveScore } from '../types';
import { Shield, Target, TrendingUp, Radio, Zap } from 'lucide-react';

interface ScoreCardProps {
  score: LiveScore;
}

export const ScoreCard: React.FC<ScoreCardProps> = ({ score }) => {
  const isChasing = !!score.target && score.target > 0;
  const ballsBowled = score.overs * 6 + score.balls;
  const ballsRemaining = Math.max(0, 120 - ballsBowled);
  const runsNeeded = isChasing ? score.target! - score.totalScore : 0;
  const rrr = isChasing && ballsRemaining > 0 ? ((Math.max(0, runsNeeded) / ballsRemaining) * 6).toFixed(2) : '0.00';

  return (
    <div className="sports-card rounded-3xl p-6 sm:p-7 shadow-2xl border border-slate-800 relative overflow-hidden">
      {/* Background stadium glow */}
      <div className="absolute top-0 right-0 w-80 h-40 bg-rose-600/10 rounded-full blur-3xl pointer-events-none" />

      {/* Top Bar: Match details and status */}
      <div className="flex items-center justify-between border-b border-slate-800/80 pb-4 mb-5">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-600 text-white text-xs font-black uppercase">
            <Radio className="w-3.5 h-3.5 animate-pulse text-white" />
            <span>{score.status}</span>
          </div>
          <span className="text-xs font-black text-slate-400 tracking-wider uppercase">
            {score.format} Series · Innings {score.innings}
          </span>
        </div>

        <div className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
          <span>Feed:</span>
          <span className="px-2.5 py-0.5 rounded-full bg-slate-800 text-emerald-400 font-mono text-[11px] font-bold border border-slate-700">
            {score.source}
          </span>
        </div>
      </div>

      {/* Team names and primary score */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-700 to-slate-900 border border-amber-400/50 flex items-center justify-center font-black text-sm text-white shadow-md">
              {score.battingTeam.slice(0, 3).toUpperCase()}
            </div>
            <div>
              <h2 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
                {score.battingTeam} <span className="text-slate-500 font-light text-xl">vs</span> {score.bowlingTeam}
              </h2>
              <p className="text-xs text-slate-400">
                Currently Batting: <strong className="text-rose-400">{score.battingTeam}</strong>
              </p>
            </div>
          </div>
        </div>

        {/* Big Score Block */}
        <div className="flex items-baseline gap-4 bg-slate-900/90 px-6 py-4 rounded-2xl border border-slate-800 shadow-inner">
          <div>
            <div className="text-4xl sm:text-5xl font-black tracking-tight text-white font-mono">
              {score.totalScore}
              <span className="text-rose-500 font-light mx-0.5">/</span>
              <span className="text-amber-400">{score.wickets}</span>
            </div>
            <div className="text-xs text-slate-400 mt-1 font-semibold">
              Overs: <span className="text-white font-bold font-mono">{score.overs}.{score.balls}</span> (20 ov)
            </div>
          </div>
        </div>
      </div>

      {/* Dynamic Match Equation Bar */}
      {isChasing && (
        <div className="mb-5 p-3 rounded-xl bg-slate-900/90 border border-slate-800 flex items-center gap-2">
          <Zap className="w-4 h-4 text-amber-400 shrink-0" />
          <p className="text-xs font-bold text-slate-200">
            {runsNeeded <= 0 ? (
              <span className="text-emerald-400">Target reached! {score.battingTeam} won!</span>
            ) : (
              <span>
                Need <strong className="text-amber-400 font-mono">{runsNeeded} runs</strong> in{' '}
                <strong className="text-white font-mono">{ballsRemaining} balls</strong> • Required RR:{' '}
                <strong className="text-rose-400 font-mono">{rrr}</strong>
              </span>
            )}
          </p>
        </div>
      )}

      {/* Secondary Metrics: Run rate & Target */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-4 border-t border-slate-800/80">
        <div className="flex items-center gap-3 bg-slate-900/50 p-3 rounded-xl border border-slate-800/60">
          <div className="p-2 rounded-lg bg-emerald-500/15 text-emerald-400">
            <TrendingUp className="w-4 h-4" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-bold text-slate-400 block">
              Current Run Rate
            </span>
            <span className="text-base font-black text-emerald-400 font-mono">
              {score.runRate.toFixed(2)}
            </span>
          </div>
        </div>

        {score.target && (
          <div className="flex items-center gap-3 bg-slate-900/50 p-3 rounded-xl border border-slate-800/60">
            <div className="p-2 rounded-lg bg-amber-500/15 text-amber-400">
              <Target className="w-4 h-4" />
            </div>
            <div>
              <span className="text-[10px] uppercase font-bold text-slate-400 block">
                Target Score
              </span>
              <span className="text-base font-black text-amber-400 font-mono">
                {score.target}
              </span>
            </div>
          </div>
        )}

        <div className="flex items-center gap-3 bg-slate-900/50 p-3 rounded-xl border border-slate-800/60">
          <div className="p-2 rounded-lg bg-blue-500/15 text-blue-400">
            <Shield className="w-4 h-4" />
          </div>
          <div>
            <span className="text-[10px] uppercase font-bold text-slate-400 block">
              Balls Recorded
            </span>
            <span className="text-base font-black text-slate-200 font-mono">
              {score.latestDeliveryId}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
