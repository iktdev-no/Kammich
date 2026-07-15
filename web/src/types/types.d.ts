// AUTO-GENERATED. DO NOT EDIT.
// Version: 0.0.1-SNAPSHOT
// Time: 2026-07-15T15:56:42.337909170Z
// Source: no.iktdev.kammich.models.shared

export interface DeviceSettingsDto {
  autoImport: boolean | null;
  excludeFolders: string[] | null;
  includeFolders: string[] | null;
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

export interface WifiInterfaceInfo {
  deviceId: string;
  hardwareName: string;
  interfaceName: string;
  limitation: InterfaceLimitation;
  supportsAp: boolean;
  supportsApAndStationSimultaneously: boolean;
}

export interface WifiInterfaceState {
  connectivityState: ConnectivityState;
  interfaceName: string;
  network: WifiNetwork | null;
}

export interface WifiTetheringNetwork {
  alignedToSSID: string | null;
  channel: number;
  frequencyMhz: number;
  isAligned: boolean;
  ssid: string;
}

export type ConnectivityState = "IDLE" | "CONNECTING" | "CONNECTED" | "DISCONNECTED" | "CAPTIVE_PORTAL" | "FAILED" | "ERROR"

export type WifiSecurityType = "NONE" | "WPA2" | "WPA3"

export interface WifiInterface {
  deviceId: string;
  name: string;
  supportsAp: boolean;
  supportsSimultaneousApSta: boolean;
}

export interface WifiTetherInterface {
  deviceId: string;
  enabled: boolean;
  name: string;
  supportsAp: boolean;
  supportsApAndStationSimultaneously: boolean;
}

export type InterfaceLimitation = "NONE" | "CHANNEL_1_TO_1" | "NO_CONCURRENT"

export interface WifiTetherSetting {
  password: string;
  security: WifiSecurityType;
  ssid: string;
}

export interface WifiInterfaceScanState {
  interfaceName: string;
  networks: WifiNetwork[];
  scanning: WifiScanState;
}

export interface WifiConnectionResult {
  message: string;
  status: ConnectivityState;
  success: boolean;
}

export interface WifiTethering {
  iface: WifiTetherInterface;
  network: WifiTetheringNetwork | null;
  state: WifiTetheringState;
}

export type WifiScanState = "IDLE" | "SCANNING" | "ERROR"

export type WifiTetheringState = "IDLE" | "STARTING" | "RUNNING" | "ALIGNING" | "RUNNING_ALIGNED" | "ERROR"

export interface WifiNetwork {
  bssid: string;
  channel: number | null;
  frequencyMhz: number | null;
  hwMode: string | null;
  interfaceName: string;
  isHidden: boolean;
  isSecure: boolean;
  securityType: string;
  signalPercent: number;
  ssid: string;
}

export interface WifiSseEvent {
  errorMessage: string | null;
  networks: WifiNetwork[];
  status: ConnectivityState;
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

