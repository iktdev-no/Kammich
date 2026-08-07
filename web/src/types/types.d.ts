// AUTO-GENERATED. DO NOT EDIT.
// Version: 0.0.1-SNAPSHOT
// Time: 2026-08-07T00:18:44.654049475Z
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

export interface WirelessNetworkSearch {
  lastSearched: string;
  networks: WifiNetwork[];
}

export interface NetworkInterface {
  interfaceName: string;
  macAdress: string;
  mode: NetworkInterfaceMode;
  type: NetworkInterfaceType;
}

export type NetworkInterfaceType = "Ethernet" | "Wifi"

export interface WifiTetherAP {
  password: string;
  security: WifiSecurityType;
  ssid: string;
}

export type InterfaceActiveState = "Idle" | "Scanning" | "StartingTether" | "Tethering" | "StoppingTether" | "Connecting" | "Connected" | "Disconnected" | "CaptivePortal"

export interface WirelessTethering {
  network: WifiNetwork | null;
  state: WirelessTetheringState;
}

export type WifiSecurityType = "NONE" | "WPA2" | "WPA3"

export interface WifiNetworkInterface {
  name: string;
}

export interface WifiNetworkConnection extends WifiNetworkInterface {
  network: WifiNetwork | null;
  state: InterfaceActiveState;
}

export interface WifiNetworkTether extends WifiNetworkInterface {
  network: WifiNetwork | null;
  state: WirelessTetheringState;
}

export type WirelessTetheringState = "Idle" | "Broadcasting"

export type NetworkInterfaceMode = "External" | "Master" | "Client" | "Idle"

export type WifiNetworkHardwareMode = "a" | "g"

export interface WirelessNetworkInterface extends NetworkInterface {
  caps: WirelessNetworkInterfaceCapability[];
}

export interface WirelessConnection {
  network: WifiNetwork | null;
  state: InterfaceActiveState;
}

export type WirelessOperatingState = "AP" | "STA" | "Idle"

export interface WifiConnectionResult {
  message: string;
  status: InterfaceActiveState;
  success: boolean;
}

export type WirelessNetworkInterfaceCapability = "STA" | "AP" | "Concurrent" | "Concurrent_Restricted_Same_Channel"

export interface WirelessInterface {
  address: string;
  connection: WirelessConnection | null;
  isAvailable: boolean;
  name: string;
  operatingState: WirelessOperatingState;
  search: WirelessNetworkSearch | null;
  tethering: WirelessTethering | null;
}

export interface WifiNetworkScan extends WifiNetworkInterface {
  networks: WifiNetwork[];
  state: InterfaceActiveState;
}


export interface WifiNetwork {
  bssid: string;
  channel: number | null;
  frequencyMhz: number;
  hwMode: WifiNetworkHardwareMode;
  interfaceName: string;
  isHidden: boolean;
  isSecure: boolean;
  securityType: string;
  signalPercent: number;
  ssid: string;
}

export interface EthernetNetworkInterface extends NetworkInterface {
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

export interface NvmeRoot extends SmartCtlRoot {
  log: NvmeLog;
}

export interface StorageInfo {
  health: DiskHealth;
  stats: StorageStats;
}

export interface SataAttributes {
  table: SataAttribute[];
}

export interface SataRoot extends SmartCtlRoot {
  attrs: SataAttributes;
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

