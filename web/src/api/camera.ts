import type { DeviceInfo, DeviceSettingsDto, WFile } from "../types/types";
import { apiGet, apiPatch } from "./client";

export function getFiles(port: string, path: string) {
    return apiGet<WFile[]>(`/camera/${port}/files/${path}`);
}

export function getDeviceInfo(port: string) {
    return apiGet<DeviceInfo>(`/camera/${port}`)
}

export function updateDeviceSettings(deviceId: string, settings: Partial<DeviceSettingsDto>) {
    return apiPatch<Partial<DeviceSettingsDto>, DeviceInfo>(`/camera/${deviceId}`, settings);
}
