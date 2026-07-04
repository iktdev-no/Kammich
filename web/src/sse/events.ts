import type { StorageInfo } from "../types/types";

export type SseEvent =
  | { type: 'ping'; timestamp: number }
  | { type: 'job-update'; jobId: string; status: string }
  | { type: 'notification'; message: string }
  | { type: 'sync-status'; running: boolean }
  | { type: 'storage-info-internal'; payload: Array<StorageInfo>}
  | { type: 'custom'; payload: unknown };
