// AUTO-GENERATED. DO NOT EDIT.
// Version: 0.0.1-SNAPSHOT
// Time: 2026-08-26T22:40:49.572385014Z
// Source: no.iktdev.kammich.models.shared

export interface DeviceSettingsDto {
  autoImport: boolean | null;
  deleteWhenVerifiedBackedup: boolean | null;
  excludeFolders: string[] | null;
  includeFolders: string[] | null;
}

export type JobStatus = "Running" | "Completed" | "Failed"

export interface ImportProgressEvent {
  completedFiles: number;
  currentFile: string | null;
  deviceId: string;
  files: ImportFile[];
  state: FileImportState;
  totalFiles: number;
}

export interface DeviceImportJobSummary {
  claimable: boolean;
  claimedBy: string | null;
  deviceId: string;
  deviceName: string;
  jobs: ImportJobSummary[];
  started: string;
}

export interface UploadMediaItem {
  fileName: string;
  fileSize: number;
  state: UploadState;
}

export interface AlbumUpdateRequest {
  albumName: string | null;
  description: string | null;
  endDate: string | null;
  startDate: string | null;
  use: boolean | null;
}

export interface ActionResponse {
  message: string;
  success: boolean;
}

export interface PowerPermissionsDto {
  canPowerOff: boolean;
  canReboot: boolean;
}

export type NotificationType = "Alert"

export interface Version {
  kammichGithubVersion: string;
  kammichVersion: string;
  updateAvailable: boolean;
}

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

export type DeviceType = "PhysicalStorageDevice" | "Camera" | "Phone" | "Unknown"

export type Capability = "CAPTURE" | "DELETE" | "UPLOAD" | "PREVIEW" | "CONFIGURE"


export type DeviceInterfaceType = "BLOCK" | "PTP" | "MTP" | "NETWORK" | "AUDIO" | "UNKNOWN"

export interface StoredDeviceInfo {
  deviceName: string;
  deviceType: DeviceType;
  lastSeen: string;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string;
}

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
  type: DeviceInterfaceType;
}

export interface GPhoto2Device extends RemovableDevice {
  port: string;
  storage: GPhoto2StorageDevice[];
}

export interface DeviceOwnershipSummary {
  claimable: boolean;
  claimedBy: string | null;
  deviceId: string;
  deviceType: DeviceType;
  manufacturer: string | null;
  model: string | null;
  name: string;
}

export interface DeviceStorageStats {
  capacityBytes: number;
  description: string;
  freeSpaceBytes: number;
  id: string;
}

export interface RemovableDevice {
  deviceType: DeviceType;
  id: string;
  interfaceType: DeviceInterfaceType;
  isReady: boolean;
  manufacturer: string;
  model: string;
  name: string;
  sn: string;
  sysPath: string;
}

export interface PhotoDevice {
  manufacturer: string | null;
  model: string | null;
  name: string;
  serialNumber: string;
}

export interface ImportJobOwnershipSummary {
  claimable: boolean;
  claimedBy: string | null;
  deviceId: string;
  jobId: string;
  totalFiles: number;
}

export type Severity = "Info" | "Warning" | "Error"

export type UploadState = "Pending" | "Uploading" | "Success" | "Failure"

export interface ImportFile {
  file: string;
  isNew: boolean;
  state: FileImportState;
}

export interface ImportJobSummary {
  claimable: boolean;
  claimedBy: string | null;
  completedFiles: number;
  jobId: string;
  totalFiles: number;
}

export type FileImportState = "Pending" | "InProgress" | "Success" | "Failure"

export type WFileType = "FILE" | "DIRECTORY"

export type ImportState = "Indexing" | "Importing" | "Completed" | "Canceled"

export type DeleteState = "Pending" | "Deleted" | "Failed"

export type Verification = "Verified" | "NotVerified" | "Failed"

export interface UploadSummary {
  lastUpdatedAt: string | null;
  totalFailedUploads: number;
  totalInQueueUploads: number;
  totalReadyUploads: number;
  totalSucceededUploads: number;
  totalUploads: number;
  userId: string;
}

export interface UploadJobSummary {
  jobId: string;
  total: number;
  totalFailure: number;
  totalSuccess: number;
  userId: string;
}

export interface UploadProgressEvent {
  failedFiles: number;
  items: UploadMediaItem[];
  jobId: string;
  state: JobStatus;
  successfulFiles: number;
  totalFiles: number;
}

export interface AlbumDeleteRequest {
  albumId: number;
  deleteFromImmich: boolean;
}

export interface AlbumCreateRequest {
  albumName: string;
  description: string | null;
  endDate: string | null;
  startDate: string | null;
}

export interface SharedWifiInterfaceInstance {
  caps: WirelessNetworkInterfaceCapability[];
  isUsable: boolean;
  mode: InterfaceMode;
  name: string;
  network: WifiNetwork | null;
  operatingMode: NetworkInterfaceMode;
}

export interface NetworkInterface {
  interfaceName: string;
  macAdress: string;
  mode: NetworkInterfaceMode;
  type: NetworkInterfaceType;
}

export type NetworkInterfaceType = "Ethernet" | "Wifi"

export interface NetworkCaptiveStatus {
  interfaceName: string;
  message: string | null;
  portalUrl: string | null;
  state: CaptivePortalState;
}

export interface WifiTetherAP {
  password: string;
  security: WifiSecurityType;
  ssid: string;
}

export type CaptivePortalState = "Online" | "CaptivePortal" | "Offline"

export type WirelessTetheringError = "Unknown" | "DeviceNotFound" | "StartFailed" | "StopFailed" | "PasswordTooShort" | "InvalidSettings"

export type WifiSecurityType = "NONE" | "WPA2" | "WPA3"

export interface WifiTether {
  error: WirelessTetheringError | null;
  ifName: string;
  network: WifiNetwork | null;
  state: WirelessTetheringState;
}

export type WirelessTetheringState = "Idle" | "Acquired" | "Starting" | "Tethering" | "Stopping"

export type NetworkInterfaceMode = "External" | "Tether" | "Client" | "Idle"

export type InterfaceMode = "Idle" | "Tether" | "Client" | "Mesh" | "AdHoc"

export interface WifiScanResult {
  error: WifiScanError | null;
  ifName: string;
  networks: WifiNetwork[];
}

export type WifiNetworkHardwareMode = "a" | "g"

export interface WifiScanStatus {
  ifName: string;
  isScanning: boolean;
}

export interface WirelessNetworkInterface extends NetworkInterface {
  caps: WirelessNetworkInterfaceCapability[];
}

export type WifiScanError = "Unknown"

export interface WifiInterfaceClient extends SharedWifiInterfaceInstance {
  state: WifiConnectionStateType;
}

export interface WifiConnection {
  error: WifiInterfaceClientError | null;
  ifName: string;
  network: WifiNetwork | null;
  state: WifiConnectionStateType;
}

export type WirelessNetworkInterfaceCapability = "STA" | "AP" | "Concurrent" | "Concurrent_Restricted_Same_Channel"

export type WifiConnectionStateType = "Connecting" | "Connected" | "Disconnecting" | "Disconnected" | "Idle"

export interface WifiNetwork {
  bandwidthMhz: number;
  bssid: string;
  channel: number | null;
  frequencyMhz: number;
  hwMode: WifiNetworkHardwareMode;
  inUse: boolean;
  interfaceName: string;
  isActive: boolean;
  isHidden: boolean;
  isSecure: boolean;
  securityType: string;
  signalPercent: number;
  ssid: string;
}

export interface WifiInterfaceTether extends SharedWifiInterfaceInstance {
  state: WirelessTetheringState;
}

export type WifiInterfaceClientError = "WrongPassword" | "NetworkNotFound" | "Unknown"

export interface EthernetNetworkInterface extends NetworkInterface {
}

export interface RemoteFile {
  deviceId: number;
  fileName: string;
  id: number;
  uploaded: boolean;
}

export interface Album {
  createdAt: string;
  description: string | null;
  endDate: string | null;
  id: number;
  sampleFile: RemoteFile | null;
  startDate: string | null;
  title: string;
  totalFiles: number;
  use: boolean;
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

export type Transport = "USB" | "SATA" | "NVME" | "UNKNOWN"

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

export interface DeviceImportSummary {
  completed: string;
  deviceId: string;
  started: string;
  state: ImportState;
}

export interface PagedResponse<T> {
  currentPage: number;
  data: T[];
  hasMore: boolean;
  totalPages: number;
}

export type WFileStatus = "Included" | "Excluded" | "None"

export interface ImmichAvailability {
  error: string | null;
  isAvailable: boolean;
  serverUrl: string | null;
  user: ImmichUserMe | null;
}

export interface ImmichLoginRequest {
  address: string;
  email: string;
  password: string;
}

export interface ImmichApiKeysMe {
  createdAt: string;
  id: string;
  name: string;
  permissions: string[];
  updatedAt: string;
}

export interface ImmichUserLicense {
  activatedAt: string;
  activationKey: string;
  licenseKey: string;
}


export interface ImmichAuthenticationLogin {
  email: string;
  password: string;
}

export interface ImmichServerVersion {
  major: number;
  minor: number;
  patch: number;
  preRelease: number | null;
}

export interface ImmichServerConnection {
  url: string;
}

export type ImmichUserStatus = "active" | "removing" | "deleted"

export type UserAvatarColor = "primary" | "pink" | "red" | "yellow" | "blue" | "green" | "purple" | "orange" | "gray" | "amber"

export interface ImmichUserMe {
  avatarColor: UserAvatarColor;
  createdAt: string;
  deletedAt: string | null;
  email: string;
  id: string;
  isAdmin: boolean;
  license: ImmichUserLicense | null;
  name: string;
  oauthId: string | null;
  profileChangedAt: string | null;
  profileImagePath: string | null;
  quotaSizeInBytes: number | null;
  quotaUsageInBytes: number | null;
  shouldChangePassword: boolean;
  status: ImmichUserStatus | null;
  storageLabel: string | null;
  updatedAt: string | null;
}

export interface ImmichSupportedMediaTypes {
  images: string[];
  sidecar: string[];
  videos: string[];
}

export interface ImmichApiKeyPostResponse {
  apiKey: ImmichApiKeyPostResponseDto;
  secret: string;
}

export interface ImmichServerStorage {
  diskAvailable: string;
  diskAvailableRaw: number;
  diskSize: string;
  diskSizeRaw: number;
  diskUsagePercentage: number;
  diskUse: string;
  diskUseRaw: number;
}

export interface ImmichApiKeyPost {
  name: string;
  permissions: string[];
}

export interface ImmichApiKeyPostResponseDto {
  createdAt: string;
  id: string;
  name: string;
  permissions: string[];
  updatedAt: string;
}

export interface ImmichServerFeatures {
  configFileAvailable: boolean;
  duplicateDetectionEnabled: boolean;
  emailNotificationEnabled: boolean;
  facialRecognitionEnabled: boolean;
  importFacesEnabled: boolean;
  mapEnabled: boolean;
  oauthAutoLaunchEnabled: boolean;
  oauthEnabled: boolean;
  ocrEnabled: boolean;
  passwordLoginEnabled: boolean;
  realtimeTranscodingEnabled: boolean;
  reverseGeocodingEnabled: boolean;
  searchEnabled: boolean;
  sidecarSupported: boolean;
  smartSearchEnabled: boolean;
  trashEnabled: boolean;
}

export interface ImmichServerConfig {
  externalDomain: string;
  isInitialized: boolean;
  isOnboarded: boolean;
  loginPageMessage: string;
  maintenanceMode: boolean;
  mapDarkStyleUrl: string;
  mapLightStyleUrl: string;
  minFaces: number;
  oauthButtonText: string;
  publicUsersEnabled: boolean;
  trashDays: number;
  userDeleteDelay: number;
}

export interface ImmichAuthenticationLoginResponse {
  accessToken: string;
  isAdmin: boolean;
  isOnboarded: boolean;
  name: string;
  profileImagePath: string;
  shouldChangePassword: boolean;
  userEmail: string;
  userId: string;
}

export interface ImmichUserAccesses {
  isActive: boolean;
  servers: ImmichServerAccess[];
  user: ImmichUserMe;
}

export interface ImmichServerAccess {
  createdAt: string;
  isActive: boolean;
  keyId: string;
  keyName: string;
  serverUrl: string;
}

export interface DeviceClaim {
  claimedByUserId: string;
  deviceSN: string;
}

export interface DeviceImport {
  completedFiles: number;
  currentFileName: string | null;
  deviceId: string;
  deviceName: string;
  files: ImportFile[];
  started: string;
  totalFiles: number;
}

