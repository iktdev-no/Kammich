import type { Device } from "../types/types";

export interface SseState {
  lastPing?: number;
  jobs: Record<string, string>;
  notifications: string[];
  syncRunning: boolean;
  devices: Array<Device>;
  connectionStatus: "online" | "connecting" | "offline";
}

export const initialSseState: SseState = {
  jobs: {},
  notifications: [],
  syncRunning: false,
  devices: [],
  connectionStatus: "connecting"
};
