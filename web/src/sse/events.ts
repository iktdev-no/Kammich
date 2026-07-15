import type { StorageInfo, Notification, MediaStats, WifiInterfaceScanState, WifiInterfaceState, WifiTethering, WifiTetherInterface } from "../types/types";

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
  | { type: "wifi-scan"; payload: Array<WifiInterfaceScanState> }
  | { type: "wifi-connectivity"; payload: Array<WifiInterfaceState> }
  | { type: "wifi-tethering"; payload: WifiTethering }
  | { type: "wifi-tethering-selected-interface"; payload: WifiTetherInterface | undefined }
  ;
