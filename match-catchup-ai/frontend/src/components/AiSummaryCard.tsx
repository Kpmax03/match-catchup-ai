import React from 'react';
import { Sparkles, CheckCircle2, ShieldCheck, Flame } from 'lucide-react';

interface AiSummaryCardProps {
  summary: string;
  totalDeliveries: number;
  fromOver: string;
  toOver: string;
}

export const AiSummaryCard: React.FC<AiSummaryCardProps> = ({
  summary,
  totalDeliveries,
  fromOver,
  toOver,
}) => {
  // Format summary text into readable paragraphs or bullet items
  const lines = summary.split('\n').filter((l) => l.trim().length > 0);

  return (
    <div className="sports-card rounded-3xl p-6 sm:p-8 border border-rose-500/40 relative overflow-hidden shadow-2xl mb-8">
      {/* Ambient gradient */}
      <div className="absolute top-0 right-0 w-80 h-40 bg-gradient-to-l from-rose-500/15 via-amber-500/10 to-transparent pointer-events-none" />

      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6 border-b border-slate-800/80 pb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-rose-600 to-amber-500 flex items-center justify-center text-white shadow-lg shadow-rose-950/60 shrink-0">
            <Sparkles className="w-5 h-5 text-amber-200" />
          </div>
          <div>
            <h3 className="text-lg font-black text-white tracking-tight flex items-center gap-2">
              <span>AI MATCH CATCH-UP SUMMARY</span>
              <span className="text-[10px] uppercase font-black tracking-wider px-2.5 py-0.5 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30 flex items-center gap-1">
                <ShieldCheck className="w-3 h-3 text-amber-400" />
                Ground Truth Verified
              </span>
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              Comprehensive factual breakdown of {totalDeliveries} missed deliveries (Overs {fromOver} to {toOver})
            </p>
          </div>
        </div>
      </div>

      {/* Summary Content */}
      <div className="space-y-3.5 text-sm leading-relaxed">
        {lines.map((line, idx) => {
          const isBullet = line.startsWith('- ') || line.startsWith('* ');
          const text = isBullet ? line.replace(/^[-*]\s+/, '') : line;
          const isWicket = text.toLowerCase().includes('wicket') || text.toLowerCase().includes('out') || text.toLowerCase().includes('bowled');
          const isBoundary = text.toLowerCase().includes('six') || text.toLowerCase().includes('four') || text.toLowerCase().includes('boundary');

          return (
            <div
              key={idx}
              className={`p-3.5 rounded-2xl border transition-all ${
                isWicket
                  ? 'bg-rose-950/30 border-rose-800/50 text-rose-100'
                  : isBoundary
                  ? 'bg-amber-950/25 border-amber-800/40 text-amber-100'
                  : 'bg-slate-900/60 border-slate-800/80 text-slate-200'
              }`}
            >
              <div className="flex items-start gap-3">
                {isWicket ? (
                  <span className="px-2 py-0.5 rounded bg-rose-600 text-white font-black text-[10px] uppercase shrink-0 mt-0.5">
                    WICKET
                  </span>
                ) : isBoundary ? (
                  <span className="px-2 py-0.5 rounded bg-amber-500 text-slate-950 font-black text-[10px] uppercase shrink-0 mt-0.5">
                    BOUNDARY
                  </span>
                ) : (
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                )}
                <span className="text-xs sm:text-sm font-medium">{text}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
