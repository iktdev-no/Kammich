import type { StorageInfo, Notification, MediaStats, WifiNetworkScan, WifiNetworkConnection, WifiNetworkTether, ImportProgressEvent, DeviceImportSummary, ImmichUserMe, ImmichApiKeyPostResponseDto, ImmichAvailability, } from "../types/types";

export type SseEvent =
  | { type: 'ping'; timestamp: number }
  | { type: 'job-update'; jobId: string; status: string }
  | { type: 'sync-status'; running: boolean }
  | { type: 'storage-info-internal'; payload: Array<StorageInfo> }
  | { type: 'removable-devices'; payload: Array<any> }
  | { type: 'custom'; payload: unknown }
  | { type: "sse-online"; }
  | { type: "sse-connecting"; }
  | { type: "sse-offline"; }
  | { type: "notifications"; payload: Array<Notification> }
  | { type: "storage-stats-media"; payload: MediaStats }
  | { type: "wifi-scan"; payload: Array<WifiNetworkScan> }
  | { type: "wifi-connectivity"; payload: Array<WifiNetworkConnection> }
  | { type: "wifi-tethering"; payload: WifiNetworkTether }
  | { type: "wifi-tethering-selected-interface"; payload: WifiNetworkTether | undefined }
  | { type: "import-media-progress"; payload: ImportProgressEvent }
  | { type: "import-device-state"; states: Array<DeviceImportSummary> }
  | { type: "immich-user-me"; payload: ImmichUserMe }
  | { type: "immich-api-key-in-use"; payload: ImmichApiKeyPostResponseDto }
  | { type: "immich-availability"; payload: ImmichAvailability }
  ;
