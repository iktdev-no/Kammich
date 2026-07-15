import type { RemovableDevice, MediaStats, Notification, StorageInfo, WifiInterfaceScanState, WifiInterfaceState, WifiTetheringState, WifiTethering, WifiTetherInterface } from "../types/types";

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
  wifiTether: WifiTethering | undefined
  wifiTetherDevice: WifiTetherInterface | undefined
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
  wifiConnections: [],
  wifiTether: undefined,
  wifiTetherDevice: undefined,
};
