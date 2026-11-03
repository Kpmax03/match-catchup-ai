import React, { useEffect, useState } from 'react';
import { Match } from '../types';
import { fetchMatches } from '../api';
import { Radio, ArrowRight, ShieldCheck, Zap, Bot, Trophy, Sparkles, Activity, Info, Tv } from 'lucide-react';

interface HomePageProps {
  onSelectMatch: (matchId: string) => void;
}

interface DisplayMatch extends Match {
  isDummy?: boolean;
  venue?: string;
  badgeLabel?: string;
  equation?: string;
}

const DUMMY_MATCHES: DisplayMatch[] = [
  {
    id: 'ENG-SA-002',
    matchName: 'England vs South Africa',
    format: 'T20',
    status: 'LIVE',
    innings: 1,
    battingTeam: 'England',
    bowlingTeam: 'South Africa',
    totalScore: 164,
    wickets: 4,
    overs: 16,
    balls: 4,
    runRate: 9.84,
    target: null,
    isDummy: true,
    venue: "Lord's, London",
    badgeLabel: 'SHOWCASE FIXTURE',
    equation: '1st Innings • Projected: 196 at current run rate',
  },
  {
    id: 'NZ-PAK-003',
    matchName: 'New Zealand vs Pakistan',
    format: 'T20',
    status: 'LIVE',
    innings: 2,
    battingTeam: 'Pakistan',
    bowlingTeam: 'New Zealand',
    totalScore: 138,
    wickets: 5,
    overs: 15,
    balls: 2,
    runRate: 9.00,
    target: 182,
    isDummy: true,
    venue: 'Eden Park, Auckland',
    badgeLabel: 'SHOWCASE FIXTURE',
    equation: 'Need 44 runs in 28 balls • Req. RR 9.42',
  },
];

export const HomePage: React.FC<HomePageProps> = ({ onSelectMatch }) => {
  const [realMatches, setRealMatches] = useState<Match[]>([]);
  const [loading, setLoading] = useState(true);
  const [dummyNotice, setDummyNotice] = useState<string | null>(null);

  useEffect(() => {
    fetchMatches()
      .then((data) => setRealMatches(data))
      .catch((err) => console.error('Failed to load matches:', err))
      .finally(() => setLoading(false));
  }, []);

  // Merge real match (IND vs AUS) with dummy showcase matches to fill the interface
  const allMatches: DisplayMatch[] = [
    ...(realMatches.length > 0
      ? realMatches.map((m) => ({
          ...m,
          isDummy: false,
          venue: 'Melbourne Cricket Ground, Melbourne',
          badgeLabel: 'LIVE AI ENGINE ARMED',
          equation: m.target
            ? `Need ${Math.max(0, m.target - m.totalScore)} runs • Target ${m.target}`
            : `1st Innings • Setting target`,
        }))
      : [
          {
            id: 'IND-AUS-001',
            matchName: 'India vs Australia',
            format: 'T20',
            status: 'LIVE',
            innings: 2,
            battingTeam: 'India',
            bowlingTeam: 'Australia',
            totalScore: 142,
            wickets: 3,
            overs: 14,
            balls: 2,
            runRate: 9.91,
            target: 176,
            isDummy: false,
            venue: 'Melbourne Cricket Ground, Melbourne',
            badgeLabel: 'LIVE AI ENGINE ARMED',
            equation: 'Need 34 runs in 34 balls • Target 176',
          },
        ]),
    ...DUMMY_MATCHES,
  ];

  const getTeamBadge = (teamName: string) => {
    const t = teamName.toLowerCase();
    if (t.includes('ind')) {
      return {
        code: 'IND',
        classes: 'bg-gradient-to-br from-blue-700 via-blue-900 to-slate-900 border-amber-400/60 text-white',
      };
    }
    if (t.includes('aus')) {
      return {
        code: 'AUS',
        classes: 'bg-gradient-to-br from-yellow-500 via-emerald-800 to-slate-900 border-emerald-400/60 text-white',
      };
    }
    if (t.includes('eng')) {
      return {
        code: 'ENG',
        classes: 'bg-gradient-to-br from-rose-700 via-blue-950 to-slate-900 border-blue-400/60 text-white',
      };
    }
    if (t.includes('south') || t.includes('sa')) {
      return {
        code: 'SA',
        classes: 'bg-gradient-to-br from-emerald-600 via-green-800 to-slate-900 border-amber-400/60 text-white',
      };
    }
    if (t.includes('pak')) {
      return {
        code: 'PAK',
        classes: 'bg-gradient-to-br from-emerald-700 via-green-900 to-slate-900 border-emerald-300/60 text-white',
      };
    }
    if (t.includes('zealand') || t.includes('nz')) {
      return {
        code: 'NZ',
        classes: 'bg-gradient-to-br from-slate-800 via-slate-900 to-black border-slate-400/60 text-white',
      };
    }
    return {
      code: teamName.slice(0, 3).toUpperCase(),
      classes: 'bg-gradient-to-br from-slate-800 to-slate-900 border-slate-700 text-white',
    };
  };

  const handleCardClick = (match: DisplayMatch) => {
    if (match.isDummy) {
      setDummyNotice(match.matchName);
    } else {
      setDummyNotice(null);
      onSelectMatch(match.id);
    }
  };

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
                  Tracks departure & return timestamps
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
                  Ground-truth facts verified by DB
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
                  Chatbot bounded strictly to missed overs
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Available Fixtures Section */}
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Activity className="w-5 h-5 text-rose-500" />
              <h2 className="text-2xl font-black text-white tracking-tight">
                ACTIVE FIXTURES
              </h2>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Select a live match to view broadcast scores and experience real-time AI catch-up
            </p>
          </div>

          <div className="flex items-center gap-2 text-xs font-semibold text-slate-400">
            <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-pulse" />
            <span>3 Broadcast Feeds Available</span>
          </div>
        </div>

        {/* Dummy Notice Toast */}
        {dummyNotice && (
          <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-200 flex flex-wrap items-center justify-between gap-4 animate-in fade-in duration-200 shadow-xl">
            <div className="flex items-center gap-3">
              <Info className="w-5 h-5 text-amber-400 shrink-0" />
              <span className="text-xs sm:text-sm">
                <strong>{dummyNotice}</strong> is a showcase fixture to fill the interface. The live AI catch-up engine is running on{' '}
                <strong className="text-white">India vs Australia</strong>.
              </span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  setDummyNotice(null);
                  onSelectMatch('IND-AUS-001');
                }}
                className="px-4 py-2 rounded-xl bg-gradient-to-r from-rose-600 to-amber-500 hover:from-rose-500 hover:to-amber-400 text-white font-black text-xs shadow-md transition-all cursor-pointer"
              >
                Go to India vs Australia
              </button>
              <button
                onClick={() => setDummyNotice(null)}
                className="px-2.5 py-1 text-slate-400 hover:text-white text-xs font-bold cursor-pointer"
              >
                Dismiss
              </button>
            </div>
          </div>
        )}

        {/* Fixtures 3-Column Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {allMatches.map((match) => {
            const batBadge = getTeamBadge(match.battingTeam);
            const bowlBadge = getTeamBadge(match.bowlingTeam);

            return (
              <div
                key={match.id}
                className={`sports-card rounded-3xl p-6 sm:p-7 border transition-all shadow-xl flex flex-col justify-between group relative overflow-hidden ${
                  !match.isDummy
                    ? 'border-rose-500/40 hover:border-rose-500/80 shadow-rose-950/20'
                    : 'border-slate-800 hover:border-slate-700'
                }`}
              >
                {/* Top Status & Badge Bar */}
                <div>
                  <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-2">
                      <span
                        className={`px-3 py-1 rounded-full text-white text-xs font-black flex items-center gap-1.5 uppercase shadow-sm ${
                          !match.isDummy ? 'bg-rose-600' : 'bg-slate-800 text-slate-300'
                        }`}
                      >
                        <Radio
                          className={`w-3 h-3 ${!match.isDummy ? 'animate-pulse text-white' : 'text-slate-400'}`}
                        />
                        {match.status}
                      </span>
                      <span className="text-xs font-extrabold text-slate-400 uppercase tracking-wider">
                        {match.format} Series
                      </span>
                    </div>

                    <span
                      className={`text-[10px] font-mono px-2 py-0.5 rounded border ${
                        !match.isDummy
                          ? 'bg-rose-500/10 border-rose-500/30 text-rose-300 font-bold'
                          : 'bg-slate-900 border-slate-800 text-slate-500'
                      }`}
                    >
                      {match.badgeLabel}
                    </span>
                  </div>

                  {/* Team Crests & Match Info */}
                  <div className="mb-6 space-y-4">
                    <div className="flex items-center gap-3.5">
                      {/* Batting Team Crest */}
                      <div
                        className={`w-12 h-12 rounded-2xl border-2 flex items-center justify-center font-black text-sm shadow-md shrink-0 ${batBadge.classes}`}
                      >
                        {batBadge.code}
                      </div>

                      <div className="flex-1 min-w-0">
                        <h3 className="text-xl font-black text-white group-hover:text-rose-400 transition-colors truncate">
                          {match.matchName}
                        </h3>
                        <p className="text-xs text-slate-400 truncate">
                          Batting: <strong className="text-slate-200">{match.battingTeam}</strong> vs{' '}
                          <strong className="text-slate-200">{match.bowlingTeam}</strong>
                        </p>
                      </div>
                    </div>

                    {/* Score Display Card */}
                    <div className="bg-slate-900/90 rounded-2xl p-4 border border-slate-800/80 flex items-center justify-between shadow-inner">
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

                    {/* Equation / Venue subtitle */}
                    <p className="text-[11px] text-slate-400 flex items-center gap-1.5 font-medium">
                      <Tv className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                      <span className="truncate">{match.equation || match.venue}</span>
                    </p>
                  </div>
                </div>

                {/* Primary Button */}
                <button
                  onClick={() => handleCardClick(match)}
                  className={`w-full py-3.5 px-4 rounded-2xl font-black text-xs tracking-wider uppercase flex items-center justify-center gap-2 transition-all cursor-pointer ${
                    !match.isDummy
                      ? 'bg-gradient-to-r from-rose-600 to-rose-500 hover:from-rose-500 hover:to-rose-400 text-white shadow-xl shadow-rose-950/50 hover:scale-[1.02] animate-shimmer'
                      : 'bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700/80'
                  }`}
                >
                  <span>{!match.isDummy ? 'Open Live Scoreboard' : 'Exhibition Match (Preview)'}</span>
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
