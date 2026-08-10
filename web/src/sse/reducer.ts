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

    case 'wifi-scan':
      return {
        ...state,
        wifiScans: event.payload,
      }
    case 'wifi-connectivity':
      return {
        ...state,
        wifiConnections: event.payload
      }

    case "wifi-tethering":
      return {
        ...state,
        wifiTether: event.payload
      }

    case "wifi-tethering-selected-interface":
      return {
        ...state,
        wifiTetherDevice: event.payload
      }

    case 'job-update':
      return {
        ...state,
        jobs: { ...state.jobs, [event.jobId]: event.status },
      };

    case "import-media-progress":
      return {
        ...state,
        activeMediaImports: {
          ...state.activeMediaImports,
          [event.payload.deviceId]: event.payload,
        },
      };

    case "import-device-state":
      return {
        ...state,
        importDevices: {
          ...state.importDevices,
          ...Object.fromEntries(
            event.states.map(device => [device.deviceId, device])
          ),
        },
      };

    case "immich-user-me":
      return {
        ...state,
        immichUserMe: event.payload
      }

    case "immich-api-key-in-use":
      return {
        ...state,
        immichApiKeyInUse: event.payload
      }

    case "immich-availability":
      return {
        ...state,
        immichAvailability: event.payload
      }

    default:
      console.log("Ingen tok seg av ", event)
      return state;
  }
}
