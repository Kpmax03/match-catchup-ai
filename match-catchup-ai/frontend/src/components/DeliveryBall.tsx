import React from 'react';
import { Delivery } from '../types';

interface DeliveryBallProps {
  delivery: Delivery;
  size?: 'sm' | 'md' | 'lg';
}

export const DeliveryBall: React.FC<DeliveryBallProps> = ({ delivery, size = 'md' }) => {
  const { runs, wicket, wicketType } = delivery;

  // Visual categorization
  let badgeClasses = 'bg-slate-800 text-slate-300 border-slate-700';
  let label = runs.toString();

  if (wicket) {
    badgeClasses = 'bg-rose-600 text-white font-black border-rose-400 sports-glow-red shadow-lg shadow-rose-950/80';
    label = 'W';
  } else if (runs === 6) {
    badgeClasses = 'bg-purple-600 text-white font-black border-purple-400 sports-glow-purple shadow-lg shadow-purple-950/80';
    label = '6';
  } else if (runs === 4) {
    badgeClasses = 'bg-amber-500 text-slate-950 font-black border-amber-300 sports-glow-amber shadow-lg shadow-amber-950/80';
    label = '4';
  } else if (runs === 0) {
    badgeClasses = 'bg-slate-800/90 text-slate-400 border-slate-700/60 font-semibold';
    label = '•';
  } else if (runs >= 2) {
    badgeClasses = 'bg-cyan-900/90 text-cyan-200 border-cyan-600/60 font-bold';
    label = runs.toString();
  } else if (runs === 1) {
    badgeClasses = 'bg-blue-900/80 text-blue-200 border-blue-600/60 font-medium';
    label = '1';
  }

  const sizeClasses = {
    sm: 'w-7 h-7 text-xs',
    md: 'w-9 h-9 text-sm',
    lg: 'w-11 h-11 text-base',
  }[size];

  return (
    <div
      title={`Over ${delivery.overNumber}.${delivery.ballNumber} — ${delivery.batter} faced ${delivery.bowler} (${delivery.totalRuns} runs${wicket ? ' - ' + wicketType : ''}): ${delivery.eventDescription}`}
      className={`rounded-full flex items-center justify-center border transition-all hover:scale-115 cursor-help ${badgeClasses} ${sizeClasses}`}
    >
      <span>{label}</span>
    </div>
  );
};
