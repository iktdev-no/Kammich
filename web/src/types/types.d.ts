/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 4.1.1 on 2026-07-06 01:44:55.

export interface Notification {
    id: string;
    type: NotificationType;
    title: string;
    message: string;
    severity: Severity;
    dismissed: boolean;
    dismissable: boolean;
    createdAt: number;
}

export interface PagedResponse<T> {
    data: T[];
    totalPages: number;
    currentPage: number;
    hasMore: boolean;
}

export interface RemoteFile {
    id: number;
    deviceId: number;
    fileName: string;
}

export interface DeviceConfig {
    deviceId: string;
    sourcePath: string[];
    autoImport: boolean;
}

export interface KammichConfig {
    cachePath: string;
    mediaPath: string;
    apiKey: string | null;
    devices: DeviceConfig[];
}

export interface KFile {
    id: string;
    device: Device;
    name: string;
    type: KFileType;
    size: number;
    path: string;
}

export interface BlockDevice {
    path: string;
    mountPoint: string;
    serialNumber: string;
    modelName: string;
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
    value: number | null;
    worst: number | null;
    thresh: number | null;
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

export interface BlockDeviceDetectedEvent extends DeviceDetectedEvent {
    devicePath: string;
}

export interface DeviceDetectedEvent extends DeviceEvent {
    vendor: string;
    product: string;
    serial: string;
    devicePath: string | null;
}

export interface DeviceEvent {
    sysPath: string;
}

export interface DeviceRemovedEvent extends DeviceEvent {
}

export interface DiskInfo {
    path: string;
    type: string;
    transport: string;
}

export interface MTPDeviceDetectedEvent extends DeviceDetectedEvent {
    devicePath: string;
}

export interface PTPDeviceDetectedEvent extends DeviceDetectedEvent {
}

export interface UdevEvent {
    event: string;
    path: string;
}

export interface Device {
    id: string;
    name: string;
    type: DeviceType;
    path: string;
    vendor: string | null;
    model: string | null;
}

export interface DeviceInfo {
    id: string;
    type: DeviceType;
    friendlyName: string | null;
    manufacturer: string | null;
    model: string | null;
    capabilities: Capability[];
    storage: DeviceStorageStats[];
    attributes: { [index: string]: any };
}

export interface DeviceStorageStats {
    id: string;
    description: string;
    capacityBytes: number;
    freeSpaceBytes: number;
}

export type NotificationType = "Alert";

export type Severity = "Info" | "Warning" | "Error";

export type KFileType = "FILE" | "DIRECTORY";

export type DeviceType = "BLOCK" | "MTP" | "PTP";

export type Capability = "CAPTURE" | "DELETE" | "UPLOAD" | "PREVIEW" | "CONFIGURE";
