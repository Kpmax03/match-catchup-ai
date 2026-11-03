import React from 'react';
import { LiveScore, CatchupSession } from '../types';
import { Target, TrendingUp, Sparkles, ArrowRight, Radio, Gauge, Zap } from 'lucide-react';

interface BroadcastScorecardProps {
  score: LiveScore;
  session: CatchupSession | null;
  onOpenCatchup: (sessionId: string) => void;
}

export const BroadcastScorecard: React.FC<BroadcastScorecardProps> = ({
  score,
  session,
  onOpenCatchup,
}) => {
  // Authoritative striker and non-striker figures directly from backend live score
  const striker = score.striker || 'Batter';
  const strikerRuns = score.strikerRuns ?? 0;
  const strikerBalls = score.strikerBalls ?? 0;
  const strikerSR =
    strikerBalls > 0 ? ((strikerRuns / strikerBalls) * 100).toFixed(1) : '0.0';

  const nonStriker = score.nonStriker || 'Non-Striker';
  const nonStrikerRuns = score.nonStrikerRuns ?? 0;
  const nonStrikerBalls = score.nonStrikerBalls ?? 0;
  const nonStrikerSR =
    nonStrikerBalls > 0 ? ((nonStrikerRuns / nonStrikerBalls) * 100).toFixed(1) : '0.0';

  // Authoritative Bowler figures directly from backend live score
  const bowler = score.bowler || 'Bowler';
  const bowlerWickets = score.bowlerWickets ?? 0;
  const bowlerRuns = score.bowlerRuns ?? 0;
  const bowlerOvers =
    score.bowlerOvers != null ? score.bowlerOvers.toFixed(1) : `${score.overs}.${score.balls}`;

  const bowlerOversNumeric =
    score.bowlerOvers != null ? score.bowlerOvers : score.overs + score.balls / 6;
  const bowlerEconomy =
    bowlerOversNumeric > 0 ? (bowlerRuns / bowlerOversNumeric).toFixed(2) : '0.00';

  // Dynamic Equation Calculations for 2nd Innings Target Chase
  const isChasing = !!score.target && score.target > 0;
  const ballsBowled = score.overs * 6 + score.balls;
  const maxBalls = 120; // 20-over match standard
  const ballsRemaining = Math.max(0, maxBalls - ballsBowled);
  const runsNeeded = isChasing ? score.target! - score.totalScore : 0;
  const requiredRunRate =
    isChasing && ballsRemaining > 0
      ? ((Math.max(0, runsNeeded) / ballsRemaining) * 6).toFixed(2)
      : '0.00';

  const projectedScore = Math.round(score.runRate * 20);

  // Team Badges Helper
  const isIndia = (name: string) => name.toLowerCase().includes('ind');
  const isAustralia = (name: string) => name.toLowerCase().includes('aus');

  return (
    <div className="rounded-3xl border border-slate-800 bg-gradient-to-b from-[#0f172a] to-[#090e1c] shadow-2xl overflow-hidden relative">
      {/* Stadium Ambient Floodlight Glow */}
      <div className="absolute top-0 right-1/4 w-96 h-48 bg-rose-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-1/4 w-96 h-48 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />

      {/* Top TV Broadcast Header Bar */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-850 to-slate-900 px-6 py-3.5 border-b border-slate-800/80 flex flex-wrap items-center justify-between gap-4">
        {/* Live Broadcast Pill & Match Metadata */}
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-600 text-white text-xs font-black tracking-wider uppercase shadow-md shadow-rose-950/60">
            <Radio className="w-3.5 h-3.5 animate-pulse text-white" />
            <span>{score.status}</span>
          </span>
          <span className="text-xs font-black uppercase tracking-widest text-slate-400 flex items-center gap-2">
            <span>{score.format} BILATERAL SERIES</span>
            <span className="text-slate-600">•</span>
            <span className="text-amber-400">INNINGS {score.innings}</span>
          </span>
        </div>

        {/* Catch-Up Direct Pill if session exists */}
        {session && (
          <button
            onClick={() => onOpenCatchup(session.id)}
            className="flex items-center gap-2 px-4 py-1.5 rounded-full bg-gradient-to-r from-rose-600 via-red-500 to-amber-500 hover:from-rose-500 hover:to-amber-400 text-white font-black text-xs shadow-lg shadow-rose-950/60 transition-all hover:scale-105 cursor-pointer animate-shimmer"
          >
            <Sparkles className="w-3.5 h-3.5 text-amber-200" />
            <span>Catch Up ({session.missedDeliveriesCount} missed balls)</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Main Scoreboard Arena */}
      <div className="p-6 sm:p-8 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
        {/* Teams and Big Score Display (Cols 7) */}
        <div className="lg:col-span-7 space-y-4">
          {/* Team Crests & Match Title */}
          <div className="flex items-center gap-4">
            {/* Batting Team Emblem */}
            <div className="relative">
              {isIndia(score.battingTeam) ? (
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-700 via-blue-900 to-slate-900 border-2 border-amber-400/60 flex items-center justify-center font-black text-lg text-white shadow-xl shadow-blue-950/80">
                  <span>IND</span>
                </div>
              ) : isAustralia(score.battingTeam) ? (
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-yellow-500 via-emerald-800 to-slate-900 border-2 border-emerald-400/60 flex items-center justify-center font-black text-lg text-white shadow-xl shadow-emerald-950/80">
                  <span>AUS</span>
                </div>
              ) : (
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-rose-600 to-slate-800 border-2 border-rose-400/50 flex items-center justify-center font-black text-lg text-white shadow-xl">
                  {score.battingTeam.slice(0, 3).toUpperCase()}
                </div>
              )}
              <span className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-rose-500 border-2 border-slate-900" title="Batting Now" />
            </div>

            <div>
              <div className="flex items-baseline gap-3">
                <h1 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
                  {score.battingTeam}
                </h1>
                <span className="text-slate-500 font-bold text-lg">vs</span>
                <span className="text-xl sm:text-2xl font-bold text-slate-400">
                  {score.bowlingTeam}
                </span>
              </div>
              <p className="text-xs font-semibold text-slate-400 mt-0.5">
                {isChasing
                  ? `Target: ${score.target} runs (${ballsRemaining} balls remaining)`
                  : `1st Innings · Setting Target (Projected: ${projectedScore})`}
              </p>
            </div>
          </div>

          {/* Big Score Numerals */}
          <div className="flex items-baseline gap-5 pt-1">
            <div className="text-6xl sm:text-7xl font-black text-white tracking-tight font-mono">
              {score.totalScore}
              <span className="text-rose-500 font-light mx-1">/</span>
              <span className="text-amber-400">{score.wickets}</span>
            </div>
            <div>
              <div className="text-xl sm:text-2xl font-black text-slate-200 font-mono">
                {score.overs}.{score.balls}{' '}
                <span className="text-sm font-semibold text-slate-500">/ 20.0 ov</span>
              </div>
              <div className="text-xs font-medium text-slate-400">
                CR: <strong className="text-emerald-400">{score.runRate.toFixed(2)}</strong> RPO
              </div>
            </div>
          </div>

          {/* Match Equation Banner Bar */}
          <div className="p-3 rounded-xl bg-slate-900/90 border border-slate-800 flex items-center gap-2.5">
            <Zap className="w-4 h-4 text-amber-400 shrink-0" />
            <p className="text-xs font-bold text-slate-200">
              {isChasing ? (
                runsNeeded <= 0 ? (
                  <span className="text-emerald-400">Target Achieved! {score.battingTeam} won the match!</span>
                ) : ballsRemaining <= 0 || score.wickets >= 10 ? (
                  <span className="text-rose-400">Innings completed.</span>
                ) : (
                  <span>
                    <strong className="text-white">{score.battingTeam}</strong> need{' '}
                    <strong className="text-amber-400 font-mono text-sm">{runsNeeded} runs</strong> in{' '}
                    <strong className="text-white font-mono">{ballsRemaining} balls</strong> • Req. RR:{' '}
                    <strong className="text-rose-400 font-mono">{requiredRunRate}</strong>
                  </span>
                )
              ) : (
                <span>
                  First Innings: <strong className="text-white">{score.battingTeam}</strong> on track for{' '}
                  <strong className="text-emerald-400 font-mono">{projectedScore}</strong> runs at current pace
                </span>
              )}
            </p>
          </div>
        </div>

        {/* Match Statistics & Gauges (Cols 5) */}
        <div className="lg:col-span-5 grid grid-cols-2 gap-3.5 bg-slate-900/70 p-4 rounded-2xl border border-slate-800/80">
          {/* Current Run Rate Card */}
          <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
            <span className="text-[10px] uppercase font-extrabold tracking-wider text-slate-400 flex items-center gap-1.5 mb-1.5">
              <TrendingUp className="w-3.5 h-3.5 text-emerald-400" /> Current Run Rate
            </span>
            <div className="flex items-baseline gap-1">
              <span className="text-3xl font-black text-emerald-400 font-mono">
                {score.runRate.toFixed(2)}
              </span>
              <span className="text-[10px] text-slate-500 font-bold">RPO</span>
            </div>
          </div>

          {/* Required Run Rate / Target Card */}
          <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
            <span className="text-[10px] uppercase font-extrabold tracking-wider text-slate-400 flex items-center gap-1.5 mb-1.5">
              <Target className="w-3.5 h-3.5 text-amber-400" /> {isChasing ? 'Target' : 'Projected Total'}
            </span>
            <div className="flex items-baseline gap-1">
              <span className="text-3xl font-black text-amber-400 font-mono">
                {isChasing ? score.target : projectedScore}
              </span>
              <span className="text-[10px] text-slate-500 font-bold">runs</span>
            </div>
          </div>

          {/* Deliveries Count */}
          <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
            <span className="text-[10px] uppercase font-extrabold tracking-wider text-slate-400 flex items-center gap-1.5 mb-1.5">
              <Gauge className="w-3.5 h-3.5 text-blue-400" /> Match Deliveries
            </span>
            <div className="flex items-baseline gap-1">
              <span className="text-2xl font-black text-slate-200 font-mono">
                {score.latestDeliveryId}
              </span>
              <span className="text-[10px] text-slate-500 font-bold">balls</span>
            </div>
          </div>

          {/* Required Rate or Innings status */}
          <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
            <span className="text-[10px] uppercase font-extrabold tracking-wider text-slate-400 flex items-center gap-1.5 mb-1.5">
              <Target className="w-3.5 h-3.5 text-rose-400" /> {isChasing ? 'Required RR' : 'Overs Done'}
            </span>
            <div className="flex items-baseline gap-1">
              <span className="text-2xl font-black text-rose-400 font-mono">
                {isChasing ? requiredRunRate : `${score.overs}.${score.balls}`}
              </span>
              <span className="text-[10px] text-slate-500 font-bold">{isChasing ? 'RPO' : 'ov'}</span>
            </div>
          </div>
        </div>
      </div>

      {/* JioHotstar / Star Sports Lower-Third: Batting & Bowling Strips */}
      <div className="bg-slate-950 border-t border-slate-800 px-6 py-4 grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
        {/* Batters On Crease */}
        <div className="space-y-2 border-b md:border-b-0 md:border-r border-slate-800/80 pb-4 md:pb-0 md:pr-6">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-slate-400">
              Batting
            </span>
            <span className="text-[10px] font-bold text-slate-500">R (B) • SR</span>
          </div>

          {/* Striker */}
          <div className="flex items-center justify-between text-xs bg-slate-900/60 px-3 py-1.5 rounded-lg border border-slate-800/60">
            <span className="font-extrabold text-white flex items-center gap-1.5">
              {striker}
              <span className="text-amber-400 font-black text-sm leading-none">*</span>
              <span className="text-[9px] px-1 py-0.2 rounded bg-amber-500/20 text-amber-300 font-semibold border border-amber-500/30">
                STRIKE
              </span>
            </span>
            <div className="flex items-center gap-2 font-mono">
              <span className="font-black text-white text-sm">{strikerRuns}</span>
              <span className="text-slate-400 text-xs">({strikerBalls})</span>
              <span className="text-emerald-400 text-xs font-semibold pl-1">{strikerSR}</span>
            </div>
          </div>

          {/* Non-Striker */}
          <div className="flex items-center justify-between text-xs bg-slate-900/30 px-3 py-1.5 rounded-lg border border-slate-800/40">
            <span className="font-semibold text-slate-300">{nonStriker}</span>
            <div className="flex items-center gap-2 font-mono">
              <span className="font-bold text-slate-200">{nonStrikerRuns}</span>
              <span className="text-slate-500 text-xs">({nonStrikerBalls})</span>
              <span className="text-slate-400 text-xs font-medium pl-1">{nonStrikerSR}</span>
            </div>
          </div>
        </div>

        {/* Current Bowler */}
        <div className="space-y-2 md:pl-2">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-slate-400">
              Bowling
            </span>
            <span className="text-[10px] font-bold text-slate-500">O • M • R • W • ECO</span>
          </div>

          <div className="flex items-center justify-between text-xs bg-slate-900/60 px-3 py-2.5 rounded-lg border border-slate-800/60">
            <div className="flex items-center gap-2">
              <span className="font-extrabold text-white text-sm">{bowler}</span>
              <span className="text-[10px] font-mono text-slate-400">({bowlerOvers} ov)</span>
            </div>

            <div className="flex items-center gap-3 font-mono">
              <div className="text-right">
                <span className="font-black text-rose-400 text-sm">
                  {bowlerWickets}
                </span>
                <span className="text-slate-500 text-xs">/</span>
                <span className="font-bold text-white text-sm">
                  {bowlerRuns}
                </span>
              </div>
              <div className="text-[11px] font-semibold text-amber-300 bg-amber-500/10 px-1.5 py-0.5 rounded border border-amber-500/20">
                Eco {bowlerEconomy}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
