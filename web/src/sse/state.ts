import type { RemovableDevice, MediaStats, Notification, StorageInfo, WifiScanState, WifiConnectivityState, WifiInterfaceScanState, WifiInterfaceState } from "../types/types";

export interface SseState {
  lastPing?: number;
  jobs: Record<string, string>;
  notifications: Notification[];
  syncRunning: boolean;
  devices: Array<RemovableDevice>;
  internalStorageInfo: Array<StorageInfo>;
  internalMediaStats: MediaStats | undefined
  connectionStatus: "online" | "connecting" | "offline";

  wifiScans: Array<WifiInterfaceScanState>
  wifiConnections: Array<WifiInterfaceState>

  
}

export const initialSseState: SseState = {
  jobs: {},
  notifications: [],
  syncRunning: false,
  devices: [],
  connectionStatus: "connecting",
  internalStorageInfo: [],
  internalMediaStats: undefined,
  wifiScans: [],
  wifiConnections: []
};
