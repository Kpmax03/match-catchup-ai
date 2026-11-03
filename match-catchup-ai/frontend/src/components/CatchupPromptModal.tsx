import React from 'react';
import { CatchupSession } from '../types';
import { Sparkles, ArrowRight, X, Clock } from 'lucide-react';

interface CatchupPromptModalProps {
  session: CatchupSession;
  onOpenCatchup: (sessionId: string) => void;
  onDismiss: () => void;
}

export const CatchupPromptModal: React.FC<CatchupPromptModalProps> = ({
  session,
  onOpenCatchup,
  onDismiss,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-gradient-to-b from-slate-900 via-slate-900 to-slate-950 border border-rose-500/30 rounded-2xl p-6 shadow-2xl shadow-rose-950/40 text-center overflow-hidden">
        {/* Glow ambient background */}
        <div className="absolute -top-24 -left-24 w-48 h-48 bg-rose-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -right-24 w-48 h-48 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />

        {/* Close Button */}
        <button
          onClick={onDismiss}
          className="absolute top-4 right-4 text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800/60 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header Icon */}
        <div className="mx-auto w-16 h-16 rounded-2xl bg-gradient-to-tr from-rose-500/20 to-amber-500/20 border border-rose-500/40 flex items-center justify-center text-rose-400 mb-5">
          <Sparkles className="w-8 h-8 animate-pulse text-amber-400" />
        </div>

        {/* Title */}
        <h2 className="text-2xl font-black text-white tracking-tight mb-2">
          YOU ARE BACK!
        </h2>
        <p className="text-sm text-slate-300 max-w-sm mx-auto mb-6">
          While you were away, the live match kept progressing in the background.
        </p>

        {/* Catch-up Stats Card */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-4 mb-6 text-left">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <span className="text-[11px] font-semibold tracking-wider uppercase text-slate-400 block mb-1">
                Missed Deliveries
              </span>
              <div className="text-2xl font-black text-rose-400">
                {session.missedDeliveriesCount}{' '}
                <span className="text-xs font-semibold text-slate-400">balls</span>
              </div>
            </div>

            <div>
              <span className="text-[11px] font-semibold tracking-wider uppercase text-slate-400 block mb-1">
                Missed Overs
              </span>
              <div className="text-lg font-bold text-white flex items-center gap-1.5 mt-0.5">
                <Clock className="w-4 h-4 text-amber-400" />
                <span>
                  {session.fromOver} <span className="text-slate-400">to</span> {session.toOver}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <button
            onClick={() => onOpenCatchup(session.id)}
            className="flex-1 py-3 px-5 rounded-xl bg-gradient-to-r from-rose-600 to-rose-500 hover:from-rose-500 hover:to-rose-400 text-white font-bold text-sm shadow-lg shadow-rose-900/40 flex items-center justify-center gap-2 transition-all cursor-pointer"
          >
            <span>Catch Up on Missed Action</span>
            <ArrowRight className="w-4 h-4" />
          </button>

          <button
            onClick={onDismiss}
            className="py-3 px-5 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-300 font-semibold text-sm border border-slate-700/60 transition-colors cursor-pointer"
          >
            Stay on Live Score
          </button>
        </div>
      </div>
    </div>
  );
};
