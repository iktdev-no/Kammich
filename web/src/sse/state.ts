import type { SseEvent } from './events';

export interface SseState {
  lastPing?: number;
  jobs: Record<string, string>;
  notifications: string[];
  syncRunning: boolean;
}

export const initialSseState: SseState = {
  jobs: {},
  notifications: [],
  syncRunning: false,
};
