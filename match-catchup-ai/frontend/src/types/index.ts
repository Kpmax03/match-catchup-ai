export interface Match {
  id: string;
  matchName: string;
  format: string;
  status: string;
  innings: number;
  battingTeam: string;
  bowlingTeam: string;
  totalScore: number;
  wickets: number;
  overs: number;
  balls: number;
  runRate: number;
  target?: number | null;
  lastCollectedDeliveryId?: number;
  updatedAt?: string;
}

export interface LiveScore {
  matchId: string;
  format: string;
  status: string;
  innings: number;
  battingTeam: string;
  bowlingTeam: string;
  totalScore: number;
  wickets: number;
  overs: number;
  balls: number;
  runRate: number;
  target?: number | null;
  latestDeliveryId: number;
  source: 'REDIS' | 'DATABASE';
  striker?: string;
  strikerRuns?: number;
  strikerBalls?: number;
  nonStriker?: string;
  nonStrikerRuns?: number;
  nonStrikerBalls?: number;
  bowler?: string;
  bowlerWickets?: number;
  bowlerRuns?: number;
  bowlerOvers?: number;
}

export interface CatchupSession {
  id: string;
  userId: string;
  matchId: string;
  inactiveFrom: string;
  returnedAt: string;
  fromDeliveryId: number;
  toDeliveryId: number;
  missedDeliveriesCount: number;
  fromOver: string;
  toOver: string;
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'DISMISSED';
  summary?: string;
  createdAt: string;
}

export interface Delivery {
  id: number;
  providerDeliveryId: number;
  innings: number;
  overNumber: number;
  ballNumber: number;
  batter: string;
  bowler: string;
  runs: number;
  extras: number;
  totalRuns: number;
  inningsScoreAfterDelivery: number;
  inningsWicketsAfterDelivery: number;
  wicket: boolean;
  legalDelivery: boolean;
  wicketType?: string;
  eventType?: string;
  eventDescription: string;
  collectedAt: string;
}

export interface OverGroup {
  innings: number;
  overNumber: number;
  totalRunsInOver: number;
  wicketsInOver: number;
  bowler: string;
  deliveries: Delivery[];
}

export interface TimelineData {
  sessionId: string;
  matchId: string;
  fromDeliveryId: number;
  toDeliveryId: number;
  totalDeliveries: number;
  totalRuns: number;
  totalWickets: number;
  totalFours: number;
  totalSixes: number;
  overs: OverGroup[];
}

export interface ChatReference {
  innings: number;
  over: number;
  ball: number;
  description: string;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  references?: ChatReference[];
  timestamp: string;
}
