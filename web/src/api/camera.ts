import type { DeviceInfo, KFile } from "../types/types";
import { apiGet } from "./client";

export function getFiles(port: string, path: string) {
    return apiGet<KFile[]>(`/camera/${port}/files/${path}`);
}

export function getDeviceInfo(port: string) {
    return apiGet<DeviceInfo>(`/camera/${port}`)
}