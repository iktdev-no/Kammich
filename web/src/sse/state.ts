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
  UploadProgressEvent,
  AppUpdateProgress,
  EthernetInterfaceState,
  WifiInterfaceState,
  DiskHealth,
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
  ethState: Record<string, EthernetInterfaceState>;
  wifiState: Record<string, WifiInterfaceState>;

  diskHealth: DiskHealth[]

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

  ethState: {},
  wifiState: {},


  activeUploadProgress: {},

  diskHealth: [],

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