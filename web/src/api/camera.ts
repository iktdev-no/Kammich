import type { DeviceInfo, DeviceSettingsDto, WFile } from "../types/types";
import { apiGet, apiPatch } from "./client";

export function getFiles(port: string, path: string) {
    return apiGet<WFile[]>(`/v1/camera/${port}/files/${path}`);
}

export function getDeviceInfo(port: string) {
    return apiGet<DeviceInfo>(`/v1/camera/${port}`)
}

export function updateDeviceSettings(deviceId: string, settings: Partial<DeviceSettingsDto>) {
    return apiPatch<Partial<DeviceSettingsDto>, DeviceInfo>(`/v1/camera/${deviceId}`, settings);
}
