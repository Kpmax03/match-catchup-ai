import { Match, LiveScore, CatchupSession, TimelineData, ChatReference, Delivery } from '../types';

const API_BASE = '/api';
export interface AuthUser { email: string; displayName: string; lastActiveAt: string | null; token: string; tokenType: string; }
export function saveToken(token: string | null) {
  if (token) {
    sessionStorage.setItem('matchcatchup-token', token);
    localStorage.removeItem('matchcatchup-token');
  } else {
    sessionStorage.removeItem('matchcatchup-token');
    localStorage.removeItem('matchcatchup-token');
  }
}
export function getToken(): string | null {
  return sessionStorage.getItem('matchcatchup-token');
}
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.detail || body?.message || `Request failed (${res.status})`);
  }
  if (res.status === 204) return undefined as T;
  const body = await res.text();
  if (!body.trim()) return undefined as T;
  return JSON.parse(body) as T;
}
export const login = (email: string, password: string) => request<AuthUser>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
export const signup = (displayName: string, email: string, password: string) => request<AuthUser>('/auth/signup', { method: 'POST', body: JSON.stringify({ displayName, email, password }) });
export const fetchMe = () => request<AuthUser>('/auth/me');
export const fetchMatches = () => request<Match[]>('/matches');
export const fetchLiveScore = (matchId: string) => request<LiveScore>(`/matches/${encodeURIComponent(matchId)}/live`);
export const fetchLiveFeed = (matchId: string) => request<Delivery[]>(`/matches/${encodeURIComponent(matchId)}/feed`);
export interface HeartbeatResult { userId: string; matchId: string; activityStatus: string; lastHeartbeatAt: string; lastViewedDeliveryId: number; latestCollectedDeliveryId: number; catchupAvailable: boolean; catchupSession?: CatchupSession | null; }
export const sendHeartbeat = (matchId: string) => request<HeartbeatResult>(`/matches/${encodeURIComponent(matchId)}/activity/heartbeat`, { method: 'POST', body: '{}' });
export const sendViewedDelivery = (matchId: string, deliveryId: number) => request<void>(`/matches/${encodeURIComponent(matchId)}/activity/viewed`, { method: 'POST', body: JSON.stringify({ deliveryId }) });
export const sendLeaveMatch = (matchId: string) => {
  const token = getToken();
  return fetch(`${API_BASE}/matches/${encodeURIComponent(matchId)}/activity/leave`, {
    method: 'POST',
    keepalive: true,
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json',
    },
  }).catch(() => {});
};
export const fetchCatchupAvailable = (matchId: string) => request<CatchupSession | null>(`/matches/${encodeURIComponent(matchId)}/catchup`);
export const fetchCatchupSession = (sessionId: string) => request<CatchupSession>(`/catchup/${encodeURIComponent(sessionId)}`);
export const fetchTimeline = (sessionId: string) => request<TimelineData>(`/catchup/${encodeURIComponent(sessionId)}/timeline`);
export const fetchSummary = async (sessionId: string) => (await request<{ summary: string }>(`/catchup/${encodeURIComponent(sessionId)}/summary`)).summary;
export const completeCatchup = (sessionId: string) => request<CatchupSession>(`/catchup/${encodeURIComponent(sessionId)}/complete`, { method: 'POST' });
export const resetMatchSimulation = (matchId: string) => request<{ message: string }>(`/matches/${encodeURIComponent(matchId)}/reset`, { method: 'POST' });
export interface ChatResponse { answer: string; references: ChatReference[]; }
export const sendChatQuestion = (sessionId: string, question: string) => request<ChatResponse>(`/catchup/${encodeURIComponent(sessionId)}/chat`, { method: 'POST', body: JSON.stringify({ question }) });
export const sendMatchChatQuestion = (matchId: string, question: string) => request<ChatResponse>(`/matches/${encodeURIComponent(matchId)}/chat`, { method: 'POST', body: JSON.stringify({ question }) });
