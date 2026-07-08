import type { Device, MediaStats, Notification, StorageInfo } from "../types/types";

export interface SseState {
  lastPing?: number;
  jobs: Record<string, string>;
  notifications: Notification[];
  syncRunning: boolean;
  devices: Array<Device>;
  internalStorageInfo: Array<StorageInfo>;
  internalMediaStats: MediaStats | undefined
  connectionStatus: "online" | "connecting" | "offline";
}

export const initialSseState: SseState = {
  jobs: {},
  notifications: [],
  syncRunning: false,
  devices: [],
  connectionStatus: "connecting",
  internalStorageInfo: [],
  internalMediaStats: undefined
};
