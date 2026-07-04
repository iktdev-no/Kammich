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
    case 'removable-devices':
      return {
            ...state,
        devices: event.payload
      }


    case 'sse-online':
      return { ...state, connectionStatus: 'online' };

    case 'sse-connecting':
      return { ...state, connectionStatus: 'connecting' };

    case 'sse-offline':
      return { ...state, connectionStatus: 'offline' };

    default:
      return state;
  }
}
