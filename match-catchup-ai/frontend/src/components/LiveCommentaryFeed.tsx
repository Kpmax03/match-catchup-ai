import React from 'react';
import { Delivery } from '../types';
import { Radio, AlertCircle } from 'lucide-react';

interface LiveCommentaryFeedProps {
  deliveries: Delivery[];
}

export const LiveCommentaryFeed: React.FC<LiveCommentaryFeedProps> = ({ deliveries }) => {
  return (
    <div className="rounded-2xl border border-slate-800 bg-[#0f172a] shadow-xl overflow-hidden">
      {/* Feed Header */}
      <div className="bg-slate-900/90 px-6 py-4 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <Radio className="w-4 h-4 text-rose-500 animate-pulse" />
          <h2 className="text-sm font-extrabold uppercase tracking-wider text-white">
            Live Ball-by-Ball Commentary Feed
          </h2>
        </div>
        <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-400">
          Showing latest {deliveries.length} balls
        </span>
      </div>

      {/* Feed List */}
      <div className="divide-y divide-slate-800/60 max-h-[550px] overflow-y-auto">
        {deliveries.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-sm">
            Waiting for live deliveries to stream in...
          </div>
        ) : (
          deliveries.map((del) => {
            const isWicket = del.wicket;
            const isSix = del.runs === 6;
            const isFour = del.runs === 4;

            let rowBg = 'hover:bg-slate-850/40';
            if (isWicket) rowBg = 'bg-rose-950/20 border-l-4 border-l-rose-500 hover:bg-rose-950/30';
            else if (isSix) rowBg = 'bg-purple-950/20 border-l-4 border-l-purple-500 hover:bg-purple-950/30';
            else if (isFour) rowBg = 'bg-amber-950/20 border-l-4 border-l-amber-500 hover:bg-amber-950/30';

            return (
              <div
                key={del.id}
                className={`p-4 transition-colors flex items-start gap-4 ${rowBg}`}
              >
                {/* Over & Ball Badge */}
                <div className="shrink-0 text-center">
                  <div className="w-12 h-8 rounded-lg bg-slate-800 border border-slate-700/80 flex items-center justify-center font-mono font-black text-xs text-white">
                    {del.overNumber}.{del.ballNumber}
                  </div>
                </div>

                {/* Delivery Commentary */}
                <div className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2 mb-1">
                    <span className="font-bold text-sm text-white">
                      {del.bowler} to {del.batter}
                    </span>

                    {/* Result Tag */}
                    {isWicket ? (
                      <span className="px-2 py-0.5 rounded bg-rose-600 text-white font-extrabold text-[11px] uppercase tracking-wider shadow-sm shadow-rose-950/40">
                        OUT — {del.wicketType || 'WICKET'}
                      </span>
                    ) : isSix ? (
                      <span className="px-2 py-0.5 rounded bg-purple-600 text-white font-black text-[11px] uppercase tracking-wider shadow-sm shadow-purple-950/40">
                        SIX (6 RUNS)
                      </span>
                    ) : isFour ? (
                      <span className="px-2 py-0.5 rounded bg-amber-500 text-slate-950 font-black text-[11px] uppercase tracking-wider">
                        FOUR (4 RUNS)
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[11px] font-semibold">
                        {del.totalRuns} {del.totalRuns === 1 ? 'run' : 'runs'}
                      </span>
                    )}

                    {del.extras > 0 && (
                      <span className="text-[11px] text-amber-400 font-medium">
                        ({del.extras} extras)
                      </span>
                    )}
                  </div>

                  {/* Description */}
                  <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                    {del.eventDescription}
                  </p>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
