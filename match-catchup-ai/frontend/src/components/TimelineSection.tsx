import React from 'react';
import { OverGroup } from '../types';
import { DeliveryBall } from './DeliveryBall';
import { ShieldAlert, Activity, ChevronRight } from 'lucide-react';

interface TimelineSectionProps {
  overs: OverGroup[];
}

export const TimelineSection: React.FC<TimelineSectionProps> = ({ overs }) => {
  return (
    <div className="space-y-6">
      {/* Timeline Section Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-4">
        <h3 className="text-xl font-black text-white tracking-tight flex items-center gap-2.5">
          <Activity className="w-5 h-5 text-rose-500" />
          <span>MISSED BALL-BY-BALL TIMELINE</span>
          <span className="text-xs font-bold px-3 py-1 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
            {overs.length} Overs Missed
          </span>
        </h3>

        {/* Legend */}
        <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400">
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-slate-700" /> Dot
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-blue-600" /> 1-3 Runs
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500" /> 4 (Four)
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-purple-600" /> 6 (Six)
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-rose-600" /> W (Wicket)
          </span>
        </div>
      </div>

      {overs.map((over) => (
        <div
          key={`over-${over.innings}-${over.overNumber}`}
          className="sports-card rounded-2xl p-6 border border-slate-800 hover:border-slate-700 transition-all shadow-lg"
        >
          {/* Over Header Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4 pb-4 border-b border-slate-800/80">
            <div className="flex items-center gap-3">
              <span className="text-xs font-black text-rose-400 tracking-wider uppercase bg-rose-500/15 px-3 py-1.5 rounded-xl border border-rose-500/30">
                Innings {over.innings} · Over {over.overNumber}
              </span>
              <span className="text-xs text-slate-300 font-semibold">
                Bowler: <strong className="text-white font-bold">{over.bowler}</strong>
              </span>
            </div>

            <div className="flex items-center gap-4 text-xs font-bold">
              <span className="text-slate-400">
                Over Total: <strong className="text-white font-mono text-sm">{over.totalRunsInOver} runs</strong>
              </span>
              {over.wicketsInOver > 0 && (
                <span className="px-2.5 py-1 rounded-lg bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1 font-black">
                  <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
                  {over.wicketsInOver} {over.wicketsInOver === 1 ? 'WICKET' : 'WICKETS'}
                </span>
              )}
            </div>
          </div>

          {/* Quick ball visual strip */}
          <div className="flex flex-wrap items-center gap-2 mb-4 bg-slate-900/60 p-3 rounded-xl border border-slate-800/60">
            <span className="text-[10px] font-black uppercase tracking-wider text-slate-400 mr-2">
              Deliveries:
            </span>
            {over.deliveries.map((del) => (
              <DeliveryBall key={del.id} delivery={del} size="sm" />
            ))}
          </div>

          {/* Ball details table / list */}
          <div className="space-y-2">
            {over.deliveries.map((del) => (
              <div
                key={del.id}
                className={`flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-xl text-xs gap-3 transition-colors ${
                  del.wicket
                    ? 'bg-rose-950/40 border border-rose-800/60 text-rose-100'
                    : del.runs === 6
                    ? 'bg-purple-950/30 border border-purple-800/40 text-purple-100'
                    : del.runs === 4
                    ? 'bg-amber-950/30 border border-amber-800/40 text-amber-100'
                    : 'bg-slate-900/50 border border-slate-800/50 text-slate-300 hover:bg-slate-900/80'
                }`}
              >
                <div className="flex items-center gap-3">
                  <span className="font-mono font-black text-amber-400 w-8">
                    {del.overNumber}.{del.ballNumber}
                  </span>
                  <div>
                    <span className="font-black text-white">{del.batter}</span>{' '}
                    <span className="text-slate-400">faced</span>{' '}
                    <span className="text-slate-200 font-semibold">{del.bowler}</span>
                    <span className="ml-2 text-slate-500">
                      ({del.runs} {del.runs === 1 ? 'run' : 'runs'} + {del.extras} extras)
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-4 justify-between sm:justify-end">
                  <span className="italic text-slate-400 truncate max-w-xs sm:max-w-md">
                    {del.eventDescription}
                  </span>

                  <div className="flex items-center gap-2">
                    {del.wicket ? (
                      <span className="font-black text-rose-400 uppercase tracking-wide bg-rose-500/20 px-2 py-0.5 rounded border border-rose-500/30">
                        Wicket: {del.wicketType || 'Out'}
                      </span>
                    ) : (
                      <span
                        className={`font-mono font-black ${
                          del.runs === 6
                            ? 'text-purple-400'
                            : del.runs === 4
                            ? 'text-amber-400'
                            : del.runs === 0
                            ? 'text-slate-500'
                            : 'text-blue-400'
                        }`}
                      >
                        {del.totalRuns} {del.totalRuns === 1 ? 'run' : 'runs'}
                      </span>
                    )}
                  </div>
                  <span className="font-mono text-slate-400 font-bold bg-slate-950 px-2 py-0.5 rounded border border-slate-800 whitespace-nowrap">
                    {del.inningsScoreAfterDelivery}/{del.inningsWicketsAfterDelivery}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};
