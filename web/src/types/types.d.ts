/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 4.1.1 on 2026-07-08 00:58:17.

export interface DeviceSettingsDto {
    autoImport: boolean | null;
    includeFolders: string[] | null;
    excludeFolders: string[] | null;
}

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
    uploaded: boolean;
}

export interface Companion {
}

export interface WFile {
    id: string;
    name: string;
    type: WFileType;
    size: number;
    path: string;
    importStatus: WFileStatus;
    uploaded: boolean;
}

export interface DeviceConfig {
    deviceId: string;
    sourcePath: string[];
    autoImport: boolean;
}

export interface KammichConfig {
    mediaPath: string;
    apiAuth: ImmichAuth | null;
    autoImportCameraByDefault: boolean;
    deviceSettings: { [index: string]: DeviceSettings };
}

export interface BlockDevice {
    name: string;
    path: string;
    mountPoint: string | null;
    serialNumber: string;
    modelName: string;
    transport: Transport;
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

export interface MediaStats {
    manufacturer: string | null;
    model: string;
    totalBytes: number;
    freeBytes: number;
    usedBytes: number;
    percentUsed: number;
    serial: string;
    transport: string;
    photoCount: number;
    videoCount: number;
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

export interface BlockDeviceDefaultInfo {
    name: string;
    physical: string;
    mountPoint: string;
    modelName: string;
    serial: string;
    transport: string | null;
}

export interface BlockDeviceDetectedEvent extends DeviceDetectedEvent {
    devicePath: string;
    defaultInfo: BlockDeviceDefaultInfo;
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
    deviceSettings: DeviceSettingsDto | null;
}

export interface DeviceStorageStats {
    id: string;
    description: string;
    capacityBytes: number;
    freeSpaceBytes: number;
}

export interface ImmichAuth {
}

export interface DeviceSettings {
    autoImport: boolean;
    includeFolders: string[];
    excludeFolders: string[];
}

export type NotificationType = "Alert";

export type Severity = "Info" | "Warning" | "Error";

export type Transport = "USB" | "SATA" | "NVME" | "UNKNOWN";

export type WFileStatus = "Included" | "Excluded" | "None";

export type WFileType = "FILE" | "DIRECTORY";

export type DeviceType = "BLOCK" | "MTP" | "PTP";

export type Capability = "CAPTURE" | "DELETE" | "UPLOAD" | "PREVIEW" | "CONFIGURE";
