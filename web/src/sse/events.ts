import type { StorageInfo, Notification, MediaStats, ImportProgressEvent, DeviceImportSummary, ImmichUserMe, ImmichApiKeyPostResponseDto, ImmichAvailability, WifiScanStatus, WifiScanResult, WifiConnection, WifiInterfaceTether, WifiTether, WifiInterfaceClient, UploadProgressEvent, AppUpdateProgress } from "../types/types";

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
  | { type: "wifi-scan-status"; state: WifiScanStatus }
  | { type: "wifi-scan-result"; payload: WifiScanResult }
  | { type: "wifi-connect"; ifName: string; payload: WifiConnection | undefined }
  | { type: "wifi-interface-client"; payload: Array<WifiInterfaceClient> }
  | { type: "wifi-tether"; ifName: string; payload: WifiTether | undefined }
  | { type: "wifi-interface-tether"; payload: Array<WifiInterfaceTether> }
  | { type: "import-media-progress"; payload: ImportProgressEvent }
  | { type: "import-device-state"; states: Array<DeviceImportSummary> }
  | { type: "immich-user-me"; payload: ImmichUserMe }
  | { type: "immich-api-key-in-use"; payload: ImmichApiKeyPostResponseDto }
  | { type: "immich-availability"; payload: ImmichAvailability }
  | { type: "upload-media-progress"; payload: UploadProgressEvent }
  | { type: "app-updater"; payload: AppUpdateProgress }
  ;
