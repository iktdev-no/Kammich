import type { RemovableDevice, MediaStats, Notification, StorageInfo, FeWifiNetwork, WifiActivityState } from "../types/types";

export interface SseState {
  lastPing?: number;
  jobs: Record<string, string>;
  notifications: Notification[];
  syncRunning: boolean;
  devices: Array<RemovableDevice>;
  internalStorageInfo: Array<StorageInfo>;
  internalMediaStats: MediaStats | undefined
  connectionStatus: "online" | "connecting" | "offline";
  wifiNetworks: Array<FeWifiNetwork>;
  wifiStatus: WifiActivityState;
  
}

export const initialSseState: SseState = {
  jobs: {},
  notifications: [],
  syncRunning: false,
  devices: [],
  connectionStatus: "connecting",
  internalStorageInfo: [],
  internalMediaStats: undefined,
  wifiNetworks: [],
  wifiStatus: 'IDLE'
};
