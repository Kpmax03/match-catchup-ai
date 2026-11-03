import React, { useState } from 'react';
import { Eye, EyeOff, LockKeyhole, Mail, UserRound, Flame, ShieldCheck, Zap, Radio, Sparkles } from 'lucide-react';
import { login, signup, AuthUser } from '../api';

export function AuthPage({ onAuthenticated }: { onAuthenticated: (user: AuthUser) => void }) {
  const [registering, setRegistering] = useState(false);
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      onAuthenticated(
        registering
          ? await signup(displayName, email, password)
          : await login(email, password)
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to authenticate.');
    } finally {
      setBusy(false);
    }
  }

  // Quick fill demo user credentials
  const fillDemoUser = () => {
    setRegistering(false);
    setEmail('user1@example.com');
    setPassword('password123');
    setError('');
  };

  return (
    <main className="min-h-screen bg-[#070b14] text-white grid lg:grid-cols-12 overflow-hidden">
      {/* Left Sports Hero Section (Cols 7) */}
      <section className="hidden lg:flex lg:col-span-7 flex-col justify-between p-14 bg-gradient-to-br from-[#10192e] via-[#0b101f] to-[#1c0e18] border-r border-slate-800 relative overflow-hidden">
        {/* Stadium Glows */}
        <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-rose-600/15 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />

        {/* Top Logo */}
        <div className="flex items-center gap-3 relative z-10">
          <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-rose-600 to-amber-500 flex items-center justify-center text-white shadow-xl shadow-rose-950/80">
            <Flame className="w-6 h-6 text-amber-200 fill-amber-200" />
          </div>
          <div>
            <span className="font-black text-2xl tracking-tight text-white">
              MatchCatchup <span className="text-rose-500">AI</span>
            </span>
            <span className="block text-xs font-semibold text-slate-400">
              Dream11 Broadcast Companion
            </span>
          </div>
        </div>

        {/* Center Pitch & Mock Scoreboard Teaser */}
        <div className="space-y-8 relative z-10 max-w-xl">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs font-black uppercase tracking-wider">
            <Zap className="w-4 h-4 text-amber-300" />
            <span>Smart Match Inactivity Tracking</span>
          </div>

          <h1 className="text-5xl font-black leading-[1.1] tracking-tight">
            Step away.<br />
            Return to every single moment you missed.
          </h1>

          <p className="text-slate-300 text-base leading-relaxed">
            Follow broadcast-grade live scoreboards. When you close the tab or step away, MatchCatchup AI automatically computes the exact gap, delivering an instant factual summary and grounded RAG insights.
          </p>

          {/* Interactive Mock Broadcast Teaser Card */}
          <div className="p-5 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-2xl space-y-3">
            <div className="flex items-center justify-between text-xs">
              <span className="flex items-center gap-1.5 text-rose-400 font-bold uppercase">
                <Radio className="w-3.5 h-3.5 animate-pulse text-rose-500" /> LIVE • INNINGS 2
              </span>
              <span className="text-amber-400 font-mono font-bold">Target 176</span>
            </div>

            <div className="flex items-baseline justify-between">
              <div>
                <span className="text-2xl font-black text-white">India</span>
                <span className="text-xs text-slate-400 block">vs Australia</span>
              </div>
              <div className="text-right">
                <span className="text-3xl font-black font-mono text-white">142/3</span>
                <span className="text-xs text-slate-400 font-mono block">(14.2 / 20.0 ov)</span>
              </div>
            </div>

            <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
              <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                <ShieldCheck className="w-4 h-4" /> Catch-Up Engine Armed
              </span>
              <span className="font-mono text-slate-500">Auto-tracks deliveries</span>
            </div>
          </div>
        </div>

        {/* Footer */}
        <p className="text-xs text-slate-500 relative z-10">
          Personal match progress securely synchronized across sessions.
        </p>
      </section>

      {/* Right Login / Register Card Section (Cols 5) */}
      <section className="lg:col-span-5 grid place-items-center p-6 sm:p-12 relative">
        <div className="w-full max-w-md space-y-8">
          {/* Mobile Header */}
          <div className="lg:hidden flex items-center gap-3 justify-center mb-6">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-rose-600 to-amber-500 flex items-center justify-center text-white">
              <Flame className="w-5 h-5 text-amber-200 fill-amber-200" />
            </div>
            <span className="font-black text-xl text-white">
              MatchCatchup <span className="text-rose-500">AI</span>
            </span>
          </div>

          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-black uppercase tracking-wider text-rose-400">
                {registering ? 'New Account' : 'Welcome Back'}
              </span>
              <button
                type="button"
                onClick={fillDemoUser}
                className="text-xs font-bold text-amber-400 hover:text-amber-300 underline cursor-pointer"
              >
                Use Demo Account
              </button>
            </div>
            <h2 className="text-3xl font-black mt-2 text-white">
              {registering ? 'Create your account' : 'Sign in to match'}
            </h2>
            <p className="text-sm text-slate-400 mt-1">
              Your match progress is saved securely to your profile.
            </p>
          </div>

          {/* Form */}
          <form onSubmit={submit} className="space-y-4">
            {registering && (
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
                Display Name
                <div className="mt-2 relative">
                  <UserRound className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
                  <input
                    required
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    className="w-full rounded-2xl border border-slate-800 bg-slate-900/90 py-3.5 pl-11 pr-4 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-rose-500 transition-colors"
                    placeholder="E.g. Virat Fan"
                  />
                </div>
              </label>
            )}

            <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
              Email Address
              <div className="mt-2 relative">
                <Mail className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
                <input
                  required
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full rounded-2xl border border-slate-800 bg-slate-900/90 py-3.5 pl-11 pr-4 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-rose-500 transition-colors"
                  placeholder="you@example.com"
                />
              </div>
            </label>

            <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
              Password
              <div className="mt-2 relative">
                <LockKeyhole className="absolute left-3.5 top-3.5 w-4 h-4 text-slate-500" />
                <input
                  required
                  type={showPassword ? 'text' : 'password'}
                  minLength={registering ? 6 : 1}
                  autoComplete={registering ? 'new-password' : 'current-password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full rounded-2xl border border-slate-800 bg-slate-900/90 py-3.5 pl-11 pr-12 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-rose-500 transition-colors"
                  placeholder={registering ? 'At least 6 characters' : 'Enter password'}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3.5 text-slate-400 hover:text-white cursor-pointer"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </label>

            {error && (
              <div role="alert" className="rounded-xl border border-rose-800 bg-rose-950/60 p-3.5 text-xs text-rose-300">
                {error}
              </div>
            )}

            <button
              disabled={busy}
              type="submit"
              className="w-full py-4 rounded-2xl bg-gradient-to-r from-rose-600 to-rose-500 hover:from-rose-500 hover:to-rose-400 text-white font-black text-sm shadow-xl shadow-rose-950/60 transition-all hover:scale-[1.01] disabled:opacity-50 cursor-pointer animate-shimmer"
            >
              {busy ? 'Authenticating…' : registering ? 'Create Account' : 'Sign In to Match'}
            </button>

            <div className="text-center pt-2">
              <span className="text-xs text-slate-400">
                {registering ? 'Already have an account? ' : 'New to MatchCatchup AI? '}
              </span>
              <button
                type="button"
                className="text-xs font-black text-rose-400 hover:text-rose-300 transition-colors cursor-pointer"
                onClick={() => {
                  setRegistering(!registering);
                  setError('');
                }}
              >
                {registering ? 'Sign In' : 'Create an Account'}
              </button>
            </div>
          </form>
        </div>
      </section>
    </main>
  );
}
