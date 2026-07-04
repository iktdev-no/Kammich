/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 4.1.1 on 2026-07-03 21:53:38.

export interface ImmichAuth {
}

export interface ApiKey extends ImmichAuth {
    key: string;
}

export interface OAuth extends ImmichAuth {
    accessToken: string;
    refreshToken: string;
}

export interface DeviceConfig {
    deviceId: string;
    sourcePath: string[];
    autoImport: boolean;
}

export interface KammichConfig {
    cachePath: string;
    apiKey: string | null;
    devices: DeviceConfig[];
}

export interface Device {
    path: string;
    mountPoint: string;
    serialNumber: string;
    modelName: string;
}

export interface DeviceDetectedEvent {
    sysPath: string;
    vendor: string;
    product: string;
    serial: string;
    gphotoPort: string;
    blockDevice: boolean;
}

export interface DiskHealth {
    deviceName: string;
    modelName: string;
    serialNumber: string;
    protocol: string;
    percentageUsed: number;
    temperatureCelsius: number;
    healthy: boolean;
}

export interface DiskInfo {
    path: string;
    type: string;
    transport: string;
}

export interface NvmeLog {
    temp: number;
    pused: number;
}

export interface NvmeRoot extends SmartCtlRoot {
    log: NvmeLog;
}

export interface RawValue {
    value: string;
}

export interface SataAttribute {
    name: string;
    raw: RawValue;
}

export interface SataAttributes {
    table: SataAttribute[];
}

export interface SataRoot extends SmartCtlRoot {
    attrs: SataAttributes;
}

export interface SmartCtlRoot {
    modelName: string;
    serialNumber: string;
    smartStatus: SmartStatus;
}

export interface SmartStatus {
    passed: boolean;
}

export interface StorageInfo {
    stats: StorageStats;
    health: DiskHealth;
}

export interface StorageStats {
    totalBytes: number;
    freeBytes: number;
    usableBytes: number;
    percentUsed: number;
    mounted: boolean;
}

export interface UdevEvent {
    event: string;
    path: string;
}
