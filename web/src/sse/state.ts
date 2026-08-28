import type {
  RemovableDevice,
  MediaStats,
  Notification,
  StorageInfo,
  ImportProgressEvent,
  DeviceImportSummary,
  ImmichUserMe,
  ImmichApiKeyPostResponseDto,
  ImmichAvailability,
  WifiScanStatus,
  WifiScanResult,
  WifiConnection,
  WifiInterfaceClient,
  WifiTether,
  WifiInterfaceTether,
  UploadProgressEvent,
  AppUpdateProgress,
} from "../types/types";

export interface SseState {
  lastPing?: number;
  jobs: Record<string, string>;
  notifications: Notification[];
  syncRunning: boolean;
  devices: Array<RemovableDevice>;
  internalStorageInfo: Array<StorageInfo>;
  internalMediaStats: MediaStats | undefined;
  connectionStatus: "online" | "connecting" | "offline";

  // WiFi strukturert per interface (Record<ifName, data>)
  wifiScanStatuses: Record<string, WifiScanStatus>;
  wifiScanResults: Record<string, WifiScanResult>;
  wifiConnection: Record<string, WifiConnection>;
  wifiConnectionInterfaces: Array<WifiInterfaceClient>;
  wifiTether: Record<string, WifiTether>;
  wifiTetherInterfaces: Array<WifiInterfaceTether>;

  importDevices: Record<string, DeviceImportSummary>;
  activeMediaImports: Record<string, ImportProgressEvent>;

  activeUploadProgress: Record<string, UploadProgressEvent>;

  immichUserMe: ImmichUserMe | undefined;
  immichApiKeyInUse: ImmichApiKeyPostResponseDto | undefined;
  immichAvailability: ImmichAvailability | undefined;

  appUpdate: AppUpdateProgress
}

export const initialSseState: SseState = {
  jobs: {},
  notifications: [],
  syncRunning: false,
  devices: [],
  connectionStatus: "connecting",
  internalStorageInfo: [],
  internalMediaStats: undefined,
  wifiScanStatuses: {},
  wifiScanResults: {},

  wifiConnection: {},
  wifiConnectionInterfaces: [],
  wifiTether: {},
  wifiTetherInterfaces: [],

  activeUploadProgress: {},

  importDevices: {},
  activeMediaImports: {},

  immichUserMe: undefined,
  immichApiKeyInUse: undefined,
  immichAvailability: undefined,

  appUpdate: {
    status: "None",
    error: null,
    message: null,
    progress: null,
    version: null
  }
};