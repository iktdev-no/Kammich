import type { SseEvent } from './events';
import type { SseState } from './state';

export function sseReducer(state: SseState, event: SseEvent): SseState {
  console.log("Reducer mottok event-type:", event.type);
  console.log("Reducer mottok full payload:", event);
  switch (event.type) {
    case 'ping':
      return { ...state, lastPing: event.timestamp };

    case 'job-update':
      return {
        ...state,
        jobs: { ...state.jobs, [event.jobId]: event.status },
      };

    case 'notifications':
      console.log(event.payload)
      return {
        ...state,
        notifications: event.payload
      }
    case 'sync-status':
      return { ...state, syncRunning: event.running };

    case 'custom':
      return state;
    case 'removable-devices':
      return {
            ...state,
        devices: event.payload
      }

    case 'storage-info-internal':
      return {
        ...state,
        internalStorageInfo: event.payload
      }
    case "storage-stats-media":
      return {
        ...state,
        internalMediaStats: event.payload
      }

    case 'sse-online':
      return { ...state, connectionStatus: 'online' };

    case 'sse-connecting':
      return { ...state, connectionStatus: 'connecting' };

    case 'sse-offline':
      return { ...state, connectionStatus: 'offline' };

    default:
      console.log("Ingen tok seg av ", event)
      return state;
  }
}
