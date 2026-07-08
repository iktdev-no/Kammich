import type { StorageInfo, Notification, MediaStats } from "../types/types";

export type SseEvent =
  | { type: 'ping'; timestamp: number }
  | { type: 'job-update'; jobId: string; status: string }
  | { type: 'sync-status'; running: boolean }
  | { type: 'storage-info-internal'; payload: Array<StorageInfo>}
  | { type: 'removable-devices'; payload: Array<any> }
  | { type: 'custom'; payload: unknown }
  | { type: "sse-online"; }
  | { type: "sse-connecting"; }
  | { type: "sse-offline"; }
  | { type: "notifications"; payload: Array<Notification> }
  | { type: "storage-stats-media"; payload: MediaStats}

  ;
