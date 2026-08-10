import type { RemovableDevice, MediaStats, Notification, StorageInfo, WifiNetworkTether, WifiNetworkConnection, WifiNetworkScan, ImportProgressEvent, DeviceImportSummary, ImmichUserMe, ImmichApiKeyPostResponseDto, ImmichAvailability } from "../types/types";

export interface SseState {
  lastPing?: number;
  jobs: Record<string, string>;
  notifications: Notification[];
  syncRunning: boolean;
  devices: Array<RemovableDevice>;
  internalStorageInfo: Array<StorageInfo>;
  internalMediaStats: MediaStats | undefined
  connectionStatus: "online" | "connecting" | "offline";


  wifiScans: Array<WifiNetworkScan>
  wifiConnections: Array<WifiNetworkConnection>
  wifiTether: WifiNetworkTether | undefined
  wifiTetherDevice: undefined | WifiNetworkTether

  importDevices: Record<string, DeviceImportSummary>;
  activeMediaImports: Record<string, ImportProgressEvent>;

  immichUserMe: ImmichUserMe | undefined;
  immichApiKeyInUse: ImmichApiKeyPostResponseDto | undefined;
  immichAvailability: ImmichAvailability | undefined;
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
  importDevices: {},
  activeMediaImports: {},

  immichUserMe: undefined,
  immichApiKeyInUse: undefined,
  immichAvailability: undefined
};
