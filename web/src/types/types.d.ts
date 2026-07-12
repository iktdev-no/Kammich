// AUTO-GENERATED. DO NOT EDIT.
// Version: 0.0.1-SNAPSHOT
// Time: 2026-07-12T02:08:02.583638812Z
// Source: no.iktdev.kammich.models.shared

export interface DeviceSettingsDto {
  autoImport: boolean | null;
  excludeFolders: string[] | null;
  includeFolders: string[] | null;
}

export interface DeviceSettings {
  autoImport: boolean;
  excludeFolders: string[];
  includeFolders: string[];
}

export interface KammichConfig {
  apiAuth: ImmichAuth | null;
  assignUnknownDeviceAsBlockDevice: boolean;
  autoImportCameraByDefault: boolean;
  deviceSettings: Record<string, DeviceSettings>;
  mediaPath: string;
}

export type Transport = "USB" | "SATA" | "NVME" | "UNKNOWN"

export type NotificationType = "Alert"


export interface Notification {
  createdAt: number;
  dismissable: boolean;
  dismissed: boolean;
  id: string;
  message: string;
  severity: Severity;
  title: string;
  type: NotificationType;
}

export interface WFile {
  id: string;
  importStatus: WFileStatus;
  name: string;
  path: string;
  size: number;
  type: WFileType;
  uploaded: boolean;
}

export type DeviceType = "BLOCK" | "PTP" | "MTP" | "NETWORK" | "AUDIO" | "UNKNOWN"

export type Capability = "CAPTURE" | "DELETE" | "UPLOAD" | "PREVIEW" | "CONFIGURE"

export interface BlockDevice extends RemovableDevice {
  devicePath: string;
  mountPoint: string | null;
}

export interface DeviceInfo {
  attributes: Record<string, any>;
  capabilities: Capability[];
  deviceSettings: DeviceSettingsDto | null;
  friendlyName: string | null;
  id: string;
  manufacturer: string | null;
  model: string | null;
  storage: DeviceStorageStats[];
  type: DeviceType;
}

export interface GPhoto2Device extends RemovableDevice {
  port: string;
  storage: GPhoto2StorageDevice[];
}

export interface DeviceStorageStats {
  capacityBytes: number;
  description: string;
  freeSpaceBytes: number;
  id: string;
}

export interface RemovableDevice {
  id: string;
  manufacturer: string;
  model: string;
  name: string;
  sn: string;
  sysPath: string;
  type: DeviceType;
}

export type Severity = "Info" | "Warning" | "Error"

export type WFileType = "FILE" | "DIRECTORY"

export type WifiActivityState = "IDLE" | "SCANNING" | "CONNECTING" | "CONNECTED" | "DISCONNECTED" | "ERROR"

export interface FeWifiInterface {
  name: string;
  supportsAp: boolean;
  supportsSimultaneousApSta: boolean;
}

export interface WifiInterfaceInfo {
  hardwareName: string;
  interfaceName: string;
  supportsAp: boolean;
  supportsApAndStationSimultaneously: boolean;
}

export interface ConnectionResult {
  message: string;
  status: ConnectionStatus;
  success: boolean;
}

export interface FeWifiNetwork {
  bssid: string;
  isSecure: boolean;
  securityType: string;
  signalPercent: number;
  ssid: string;
}

export type ConnectionStatus = "CONNECTED" | "CAPTIVE_PORTAL" | "FAILED" | "DISCONNECTED"

export interface WifiSseEvent {
  errorMessage: string | null;
  networks: FeWifiNetwork[];
  status: WifiActivityState;
}

export interface RemoteFile {
  deviceId: number;
  fileName: string;
  id: number;
  uploaded: boolean;
}

export interface DiskHealth {
  deviceName: string;
  isHealthy: boolean;
  modelName: string;
  percentageUsed: number;
  protocol: string;
  serialNumber: string;
  temperatureCelsius: number;
}

export interface LsblkBlockDevice {
  modelName: string;
  mountPoint: string | null;
  mounted: boolean;
  name: string;
  path: string;
  serialNumber: string;
  transport: Transport;
}

export interface NvmeRoot {
  log: NvmeLog;
  modelName: string;
  serialNumber: string;
  smartStatus: SmartStatus;
}

export interface StorageInfo {
  health: DiskHealth;
  stats: StorageStats;
}

export interface SataAttributes {
  table: SataAttribute[];
}

export interface SataRoot {
  attrs: SataAttributes;
  modelName: string;
  serialNumber: string;
  smartStatus: SmartStatus;
}

export interface NvmeLog {
  pUsed: number;
  temp: number;
}

export interface SataAttribute {
  name: string;
  raw: RawValue;
  thresh: number | null;
  value: number | null;
  worst: number | null;
}

export interface MediaStats {
  freeBytes: number;
  manufacturer: string | null;
  model: string;
  percentUsed: number;
  photoCount: number;
  serial: string;
  totalBytes: number;
  transport: string;
  usedBytes: number;
  videoCount: number;
}

export interface SmartCtlRoot {
  modelName: string;
  serialNumber: string;
  smartStatus: SmartStatus;
}

export interface SmartStatus {
  passed: boolean;
}

export interface StorageStats {
  freeBytes: number;
  isMounted: boolean;
  percentUsed: number;
  totalBytes: number;
  usableBytes: number;
}

export interface RawValue {
  value: string;
}

export interface PagedResponse<T> {
  currentPage: number;
  data: T[];
  hasMore: boolean;
  totalPages: number;
}

export type WFileStatus = "Included" | "Excluded" | "None"

