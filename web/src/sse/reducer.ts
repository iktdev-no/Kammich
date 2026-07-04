import type { SseEvent } from './events';
import type { SseState } from './state';

export function sseReducer(state: SseState, event: SseEvent): SseState {
  switch (event.type) {
    case 'ping':
      return { ...state, lastPing: event.timestamp };

    case 'job-update':
      return {
        ...state,
        jobs: { ...state.jobs, [event.jobId]: event.status },
      };

    case 'notification':
      return {
        ...state,
        notifications: [...state.notifications, event.message],
      };

    case 'sync-status':
      return { ...state, syncRunning: event.running };

    case 'custom':
      return state;

    default:
      return state;
  }
}
